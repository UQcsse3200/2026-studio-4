package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;

/** Owns the active room and applies the room graph specified by {@link WorldConfig}. */
public class RoomManager {
  private static final float INTERACTION_RANGE = 1f;
  private static final int ARRIVAL_OFFSET_TILES = 3;
  private static final String CLEAR_REQUIRED_MESSAGE = "Defeat all enemies first.";
  private static final String COMPLETED_MESSAGE = "Dungeon completed.";

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

  /** Creates the JSON-driven room manager. Call {@link #create()} to register the initial room. */
  public RoomManager(WorldConfig world, Entity player, CameraComponent camera) {
    world.validate();
    this.world = world;
    this.player = player;
    this.camera = camera;
    currentConfig = world.getRoom(world.startRoomId);
    initialEntryPoint = currentConfig.getEntryPoint(world.startEntryPointId);
    currentRoom = RoomFactory.createRoom(currentConfig, camera, false);
    player.getEvents().addListener("interact", this::interact);
    FollowingCameraComponent cameraFollowingComponent = currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
  }

  /** Registers the active room and player, then positions the player at its entry point. */
  public void create() {
    EntityService entityService = ServiceLocator.getEntityService();
    entityService.register(currentRoom);
    entityService.register(player);
    start(initialEntryPoint);
  }

  private void start(PositionConfig entryPoint) {
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    Vector2 position =
        currentRoom
            .getComponent(TerrainComponent.class)
            .tileToWorldPosition(new GridPoint2(entryPoint.x, entryPoint.y));
    player.setPosition(position);
  }

  /** Applies a requested room switch after the current physics step has completed. */
  public void update() {
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
      showStatus(exit.message == null ? "This dungeon is not available yet." : exit.message);
      return;
    }
    RoomConfig destination = world.getRoom(exit.destinationRoomId);
    if (destination.dungeonId != null && completedDungeonIds.contains(destination.dungeonId)) {
      showStatus(COMPLETED_MESSAGE);
      return;
    }
    EnemyManagerComponent enemies = currentRoom.getComponent(EnemyManagerComponent.class);
    boolean roomCleared = enemies.isCleared();
    if (exit.requiresClear && !roomCleared) {
      showStatus(CLEAR_REQUIRED_MESSAGE);
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
    Entity nextRoom =
        RoomFactory.createRoom(destination, camera, clearedRoomIds.contains(destination.id));
    currentRoom.dispose();
    currentConfig = destination;
    currentRoom = nextRoom;
    ServiceLocator.getEntityService().register(currentRoom);
    start(arrivalPosition);
    FollowingCameraComponent cameraFollowingComponent = currentRoom.getComponent(FollowingCameraComponent.class);
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

  private void showStatus(String message) {
    GameAreaDisplay display = currentRoom.getComponent(GameAreaDisplay.class);
    if (display != null) {
      display.showStatus(message);
    }
  }
}
