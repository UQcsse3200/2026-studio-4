package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.player.InteractionPromptDisplay;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.Timer;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Owns the active room and applies the room graph specified by {@link WorldConfig}. */
public class RoomManager {
  private static final float INTERACTION_RANGE = 1f;
  private static final int ARRIVAL_OFFSET_TILES = 3;

  private Entity currentRoom;
  private final Entity player;
  private final WorldConfig world;
  private final CameraComponent camera;
  private final Set<String> clearedRoomIds = new HashSet<>();
  private final Set<String> completedDungeonIds = new HashSet<>();
  private RoomConfig currentConfig;
  private final PositionConfig initialEntryPoint;
  private RoomConfig pendingDestination;
  private PositionConfig pendingArrivalPosition;
  private boolean clearRequested;
  private final Timer timer;

  /** Creates the JSON-driven room manager. Call {@link #create()} to register the initial room. */
  public RoomManager(WorldConfig world, Entity player, CameraComponent camera, Timer timer) {
    world.validate();
    this.world = world;
    this.player = player;
    this.camera = camera;
    this.timer = timer;
    currentConfig = world.getRoom(world.startRoomId);
    initialEntryPoint = currentConfig.getEntryPoint(world.startEntryPointId);
    currentRoom = RoomFactory.createRoom(currentConfig, camera, false);
    player.getEvents().addListener("interact", this::interact);
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
  }

  /** Package private constructer to create empty room manager for testing */
  RoomManager(Entity player, Timer timer) {
    this.player = player;
    this.timer = timer;
    this.world = null;
    this.camera = null;
    this.initialEntryPoint = null;
  }

  /** Registers the active room and player, then positions the player at its entry point. */
  public void create() {
    EntityService entityService = ServiceLocator.getEntityService();
    entityService.register(currentRoom);
    entityService.register(player);
    start(initialEntryPoint);
    if (timer != null && currentConfig.dungeonId != null) {
      timer.startDungeon(currentConfig.dungeonId);
    }
  }

  /** Package private for testing */
  void start(PositionConfig entryPoint) {
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    scaleRoom(currentRoom);
    Vector2 position =
        currentRoom
            .getComponent(TerrainComponent.class)
            .tileToWorldPosition(new GridPoint2(entryPoint.x, entryPoint.y));
    player.setPosition(position);
  }

  /**
   * Calls scale on a room entities {@link EnemyManagerComponent}
   *
   * <p>uses the number of cleared dungeons {@link #completedDungeonIds} to determine amount to
   * scale
   */
  private void scaleRoom(Entity entity) {
    entity.getComponent(EnemyManagerComponent.class).scale(completedDungeonIds.size());
  }

  /** Applies a requested room switch after the current physics step has completed. */
  public void update() {
    refreshInteractionPrompt();
    if (clearRequested) {
      clearRequested = false;
      currentRoom.getComponent(EnemyManagerComponent.class).clear();
    }
    if (pendingDestination == null) {
      return;
    }
    RoomConfig destination = pendingDestination;
    PositionConfig arrivalPosition = pendingArrivalPosition;
    pendingDestination = null;
    pendingArrivalPosition = null;
    switchToRoom(destination, arrivalPosition);
  }

  /** Processes an E-key interaction with the nearest configured fixture. */
  public void interact() {
    if (pendingDestination != null) {
      return;
    }
    ExitConfig exit = findNearestExit();
    if (exit == null) {
      return;
    }
    if (!exit.available) {
      showStatus(exit.message == null ? InteractionPrompt.DUNGEON_UNAVAILABLE : exit.message);
      return;
    }
    RoomConfig destination = world.getRoom(exit.destinationRoomId);
    if (destination.dungeonId != null && completedDungeonIds.contains(destination.dungeonId)) {
      showStatus(InteractionPrompt.DUNGEON_COMPLETED);
      return;
    }
    EnemyManagerComponent enemies = currentRoom.getComponent(EnemyManagerComponent.class);
    boolean roomCleared = enemies.isCleared();
    if (exit.requiresClear && !roomCleared) {
      showStatus(InteractionPrompt.CLEAR_REQUIRED);
      return;
    }
    if (roomCleared) {
      clearedRoomIds.add(currentConfig.id);
    }
    if (exit.completesDungeon) {
      completedDungeonIds.add(currentConfig.dungeonId);
    }
    pendingDestination = destination;
    if (exit.destinationExitId != null) {
      pendingArrivalPosition = arrivalInsideDoor(destination.getExit(exit.destinationExitId));
    } else {
      pendingArrivalPosition = destination.getEntryPoint(exit.destinationEntryPointId);
    }
  }

  /** Requests that the current room's enemies be cleared at the next safe update point. */
  public void clearCurrentRoom() {
    clearRequested = true;
  }

  private void onRoomCleared() {
    if (clearedRoomIds.add(currentConfig.id)) {
      showStatus("Room cleared.");
    }
  }

  private ExitConfig findNearestExit() {
    TerrainComponent terrain = currentRoom.getComponent(TerrainComponent.class);
    ExitConfig nearest = null;
    float nearestDistance = INTERACTION_RANGE;
    for (ExitConfig exit : currentConfig.exits) {
      Vector2 exitPosition = terrain.tileToWorldPosition(new GridPoint2(exit.x, exit.y));
      float distance = player.getCenterPosition().dst(exitPosition);
      if (distance <= nearestDistance) {
        nearest = exit;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  private void switchToRoom(RoomConfig destination, PositionConfig arrivalPosition) {
    String previousDungeonId = currentConfig.dungeonId;
    Entity nextRoom =
        RoomFactory.createRoom(destination, camera, clearedRoomIds.contains(destination.id));
    currentRoom.dispose();
    currentConfig = destination;
    currentRoom = nextRoom;
    if (timer != null && !Objects.equals(previousDungeonId, destination.dungeonId)) {
      timer.stopDungeon();
      if (destination.dungeonId != null) {
        timer.startDungeon(destination.dungeonId);
      }
    }
    ServiceLocator.getEntityService().register(currentRoom);
    start(arrivalPosition);
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
  }

  private PositionConfig arrivalInsideDoor(ExitConfig door) {
    PositionConfig arrival = new PositionConfig();
    arrival.x = door.x;
    arrival.y = door.y;
    switch (door.side) {
      case "LEFT":
        arrival.x += ARRIVAL_OFFSET_TILES;
        break;
      case "RIGHT":
        arrival.x -= ARRIVAL_OFFSET_TILES;
        break;
      case "TOP":
        arrival.y -= ARRIVAL_OFFSET_TILES;
        break;
      case "BOTTOM":
        arrival.y += ARRIVAL_OFFSET_TILES;
        break;
      default:
        throw new IllegalStateException("Validated door has invalid side: " + door.side);
    }
    return arrival;
  }

  private void refreshInteractionPrompt() {
    InteractionPromptDisplay display = player.getComponent(InteractionPromptDisplay.class);
    if (display == null) {
      return;
    }
    display.setPrompt(InteractionPrompt.resolve(getItemPrompt(), getExitPrompt()));
  }

  private String getItemPrompt() {
    ItemPickupComponent pickup = player.getComponent(ItemPickupComponent.class);
    return pickup == null ? null : pickup.getPickupPrompt();
  }

  private String getExitPrompt() {
    ExitConfig exit = findNearestExit();
    if (exit == null) {
      return null;
    }
    RoomConfig destination =
        exit.destinationRoomId == null ? null : world.getRoom(exit.destinationRoomId);
    boolean dungeonCompleted =
        destination != null
            && destination.dungeonId != null
            && completedDungeonIds.contains(destination.dungeonId);
    EnemyManagerComponent enemies = currentRoom.getComponent(EnemyManagerComponent.class);
    boolean roomCleared = enemies != null && enemies.isCleared();
    return InteractionPrompt.forExit(exit, roomCleared, dungeonCompleted);
  }

  private void showStatus(String message) {
    GameAreaDisplay display = currentRoom.getComponent(GameAreaDisplay.class);
    if (display != null) {
      display.showStatus(message);
    }
  }

  /** Package private setter for unit testing */
  void setCurrentRoom(Entity room) {
    this.currentRoom = room;
  }
}
