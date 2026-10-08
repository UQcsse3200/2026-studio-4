package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.minimap.Minimap;
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
  private String checkpointRoomId;
  private String checkpointEntryPointId;
  private PositionConfig checkpointPosition;
  private PositionConfig initialEntryPoint;
  private Vector2 initialWorldPosition;
  private RoomConfig pendingDestination;
  private PositionConfig pendingArrivalPosition;
  private boolean clearRequested;
  private final RunTimer runTimer;
  private String pendingDungeonCompletion;

  public static final String DEFAULT_TILESET = "images/dungeons/tileSet0.png";
  public static final String DUNGEON1_TILESET = "images/dungeons/tileSet1.png";
  public static final String DUNGEON2_TILESET = "images/dungeons/tileSet2.png";
  public static final String DUNGEON3_TILESET = "images/dungeons/tileSet3.png";
  public static final String DUNGEON4_TILESET = "images/dungeons/tileSet4.png";
  public static final String DUNGEON5_TILESET = "images/dungeons/tileSet5.png";

  /** Creates the JSON-driven room manager. Call {@link #create()} to register the initial room. */
  public RoomManager(WorldConfig world, Entity player, CameraComponent camera) {
    world.validate();
    this.world = world;
    this.player = player;
    this.camera = camera;
    this.runTimer = ServiceLocator.getRunTimer();
    currentConfig = world.getRoom(world.startRoomId);
    initialEntryPoint = currentConfig.getEntryPoint(world.startEntryPointId);
    String tileset = DEFAULT_TILESET;
    if (currentConfig.dungeonId != null) {
      tileset = getTileset(currentConfig.dungeonId);
    }
    currentRoom = RoomFactory.createRoom(currentConfig, camera, false, tileset);
    currentConfig = world.getRoom(world.startRoomId);
    for (RoomConfig room : world.rooms) {
      if (room.dungeonId != null) {
        runTimer.registerDungeon(room.dungeonId);
      }
    }
    checkpointRoomId = currentConfig.id;
    checkpointEntryPointId = world.startEntryPointId;
    checkpointPosition = new PositionConfig();
    checkpointPosition.x = initialEntryPoint.x;
    checkpointPosition.y = initialEntryPoint.y;

    player.getEvents().addListener("interact", this::interact);
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
    attachMinimap();

    if (ServiceLocator.getAchievementService() != null) {
      ServiceLocator.getAchievementService()
          .getEvents()
          .addListener("achievementUnlocked", this::onAchievementUnlocked);
    }
  }

  /** Pass in DungeonID to get tileset for the dungeon. deafult tileset as fallback */
  private String getTileset(String tilesetConfig) {
    if (tilesetConfig == null) {
      return DEFAULT_TILESET;
    }
    return switch (tilesetConfig) {
      case "dungeonOne" -> DUNGEON1_TILESET;
      case "dungeonTwo" -> DUNGEON2_TILESET;
      case "dungeonThree" -> DUNGEON3_TILESET;
      case "dungeonFour" -> DUNGEON4_TILESET;
      case "finalDungeon" -> DUNGEON5_TILESET;
      default -> DEFAULT_TILESET;
    };
  }

  /** Package private constructer to create empty room manager for testing */
  RoomManager(Entity player) {
    this.player = player;
    this.runTimer = ServiceLocator.getRunTimer();
    this.world = null;
    this.camera = null;
    this.initialEntryPoint = null;
  }

  /** Call after construction and before create() when starting from a save. */
  public void initializeFromCheckpoint(GameSaveData.Checkpoint checkpoint) {
    if (checkpoint == null) {
      throw new IllegalArgumentException("Checkpoint is required");
    }

    RoomConfig savedRoom = world.getRoom(checkpoint.roomId);
    if (savedRoom == null) {
      throw new IllegalArgumentException("Unknown checkpoint room: " + checkpoint.roomId);
    }

    PositionConfig spawn = new PositionConfig();
    spawn.x = checkpoint.tileX;
    spawn.y = checkpoint.tileY;

    if (checkpoint.entryPointId != null
        && savedRoom.getEntryPoint(checkpoint.entryPointId) == null) {
      throw new IllegalArgumentException("Unknown checkpoint entry: " + checkpoint.entryPointId);
    }

    currentRoom.dispose();
    currentConfig = savedRoom;
    currentRoom =
        RoomFactory.createRoom(savedRoom, camera, false, getTileset(currentConfig.dungeonId));
    initialEntryPoint = spawn;
    initialWorldPosition = null;

    checkpointRoomId = savedRoom.id;
    checkpointEntryPointId = checkpoint.entryPointId;
    checkpointPosition = spawn;

    FollowingCameraComponent following = currentRoom.getComponent(FollowingCameraComponent.class);
    following.setCamera(camera);
    following.setTarget(player);
  }

  /** Starts at the last quit position, while retaining the saved death checkpoint. */
  public void initializeFromSavedRun(GameSaveData save, boolean atCheckpoint) {
    if (save == null || save.checkpoint == null) {
      throw new IllegalArgumentException("Save data and checkpoint are required");
    }
    initializeFromCheckpoint(save.checkpoint);
    if (atCheckpoint || save.resumePosition == null) {
      return;
    }

    GameSaveData.ResumePosition resume = save.resumePosition;
    RoomConfig resumeRoom = world.getRoom(resume.roomId);
    if (resumeRoom == null || !Float.isFinite(resume.x) || !Float.isFinite(resume.y)) {
      throw new IllegalArgumentException("Invalid saved resume position");
    }

    currentRoom.dispose();
    currentConfig = resumeRoom;
    currentRoom =
        RoomFactory.createRoom(resumeRoom, camera, false, getTileset(resumeRoom.dungeonId));
    initialEntryPoint = null;
    initialWorldPosition = new Vector2(resume.x, resume.y);

    FollowingCameraComponent following = currentRoom.getComponent(FollowingCameraComponent.class);
    following.setCamera(camera);
    following.setTarget(player);
  }

  /** Registers the active room and player, then positions the player at its entry point. */
  public void create() {
    currentRoom =
        RoomFactory.createRoom(currentConfig, camera, false, getTileset(currentConfig.dungeonId));
    FollowingCameraComponent following = currentRoom.getComponent(FollowingCameraComponent.class);
    following.setCamera(camera);
    following.setTarget(player);

    EntityService entityService = ServiceLocator.getEntityService();
    entityService.register(currentRoom);
    entityService.register(player);
    if (initialWorldPosition == null) {
      start(initialEntryPoint);
    } else {
      start(initialWorldPosition);
    }
    if (runTimer != null) {
      runTimer.startDungeon(currentConfig.dungeonId);
    }
  }

  private void start(Vector2 worldPosition) {
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    scaleRoom(currentRoom);
    player.setPosition(worldPosition);
  }

  /** Package private for testing */
  void start(PositionConfig entryPoint) {
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    scaleRoom(currentRoom);
    Vector2 position =
        currentRoom
            .getComponent(TerrainComponent.class)
            .tileToWorldPosition(new GridPoint2(entryPoint.x + 2, entryPoint.y - 2));
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
    if (interactWithNpc()) {
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
      completeDungeon();
    }
    pendingDestination = destination;
    if (exit.destinationExitId != null) {
      pendingArrivalPosition = arrivalInsideDoor(destination.getExit(exit.destinationExitId));
    } else {
      pendingArrivalPosition = destination.getEntryPoint(exit.destinationEntryPointId);
    }
  }

  /** Records the current dungeon as completed and reports its clear time for achievements. */
  private void completeDungeon() {
    completedDungeonIds.add(currentConfig.dungeonId);
    if (runTimer != null && ServiceLocator.getAchievementService() != null) {
      AchievementContext ctx = new AchievementContext();
      ctx.dungeonId = currentConfig.dungeonId;
      ctx.dungeonSeconds = runTimer.getDungeonTime();
      ServiceLocator.getAchievementService().update(ctx);
    }
    pendingDungeonCompletion = currentConfig.dungeonId; // defer the toast
  }

  private boolean interactWithNpc() {
    FriendlyNpcManagerComponent npcs = currentRoom.getComponent(FriendlyNpcManagerComponent.class);
    if (npcs == null) {
      return false;
    }
    NpcInteractableComponent npc = npcs.findNearestInRange(player);
    if (npc == null) {
      return false;
    }
    if (npc.interact(player)) {
      return true;
    }
    String reason = npc.getPrompt(player);
    if (reason == null) {
      return false;
    }
    showStatus(reason);
    return true;
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

  void switchToRoom(RoomConfig destination, PositionConfig arrivalPosition) {
    String previousDungeonId = currentConfig.dungeonId;
    String tileset = DEFAULT_TILESET;
    if (destination.dungeonId != null) {
      tileset = getTileset(destination.dungeonId);
    }
    Entity nextRoom =
        RoomFactory.createRoom(
            destination, camera, clearedRoomIds.contains(destination.id), tileset);
    currentRoom.dispose();
    currentConfig = destination;
    currentRoom = nextRoom;
    if (runTimer != null && !Objects.equals(previousDungeonId, destination.dungeonId)) {
      runTimer.stopDungeon();
      if (destination.dungeonId != null) {
        runTimer.startDungeon(destination.dungeonId);
      }
    }
    ServiceLocator.getEntityService().register(currentRoom);
    player.getEvents().trigger(Minimap.ROOM_CHANGE_EVENT, destination);

    if (pendingDungeonCompletion != null && ServiceLocator.getAchievementService() != null) {
      AchievementContext ctx = new AchievementContext();
      ctx.dungeonCompletedId = pendingDungeonCompletion;
      ServiceLocator.getAchievementService().update(ctx);
      pendingDungeonCompletion = null;
    }

    start(arrivalPosition);
    rememberCheckpoint(arrivalPosition, null);
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
    if (runTimer != null
        && destination.dungeonId != null
        && !Objects.equals(previousDungeonId, destination.dungeonId)
        && ServiceLocator.getAchievementService() != null) {
      AchievementContext ctx = new AchievementContext();
      ctx.dungeonEnteredId = destination.dungeonId;
      ServiceLocator.getAchievementService().update(ctx);
    }
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
    NpcInteractorComponent interactor = player.getComponent(NpcInteractorComponent.class);
    if (interactor != null && interactor.isInteracting()) {
      display.clearPrompt();
      return;
    }
    display.setPrompt(InteractionPrompt.resolve(getNpcPrompt(), getItemPrompt(), getExitPrompt()));
  }

  private String getNpcPrompt() {
    FriendlyNpcManagerComponent npcs = currentRoom.getComponent(FriendlyNpcManagerComponent.class);
    return npcs == null ? null : npcs.getPrompt(player);
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

  private void onAchievementUnlocked(String name) {
    GameAreaDisplay display = currentRoom.getComponent(GameAreaDisplay.class);
    if (display != null) {
      display.showAchievement(name);
    }
  }

  /** Package private setter for unit testing */
  void setCurrentRoom(Entity room) {
    this.currentRoom = room;
  }

  /**
   * Attaches the minimap component to the player
   *
   * @requries {@link #player} != null && player is not registered
   * @requries {@link #currentConfig} != null
   */
  void attachMinimap() {
    var minimap = new Minimap(currentConfig, clearedRoomIds);
    player.addComponent(minimap);
  }

  public void activateCheckpoint(String entryPointId) {
    PositionConfig entry = currentConfig.getEntryPoint(entryPointId);
    if (entry == null) {
      throw new IllegalArgumentException("Unknown checkpoint entry: " + entryPointId);
    }
    rememberCheckpoint(entry, entryPointId);
  }

  private void rememberCheckpoint(PositionConfig position, String entryPointId) {
    checkpointRoomId = currentConfig.id;
    checkpointEntryPointId = entryPointId;
    checkpointPosition = new PositionConfig();
    checkpointPosition.x = position.x;
    checkpointPosition.y = position.y;
  }

  public GameSaveData.Checkpoint getCheckpointData() {
    GameSaveData.Checkpoint checkpoint = new GameSaveData.Checkpoint();
    checkpoint.roomId = checkpointRoomId;
    checkpoint.entryPointId = checkpointEntryPointId;
    checkpoint.tileX = checkpointPosition.x;
    checkpoint.tileY = checkpointPosition.y;
    return checkpoint;
  }

  public GameSaveData.ResumePosition getResumePositionData() {
    GameSaveData.ResumePosition position = new GameSaveData.ResumePosition();
    Vector2 playerPosition = player.getPosition();
    position.roomId = currentConfig.id;
    position.x = playerPosition.x;
    position.y = playerPosition.y;
    return position;
  }

  /** debug function for RoomCommand */
  WorldConfig getWorld() {
    return world;
  }

  /** debug function for RoomCommand */
  void debugSwitchRoom(RoomConfig destination) {
    pendingDestination = destination;

    // sets either the first exit of the room or first exit as destination pos
    if (destination.exits.length < 1 && destination.entryPoints.length > 0) {
      pendingArrivalPosition = destination.entryPoints[0];
    } else if (destination.exits.length >= 1) {
      pendingArrivalPosition = destination.exits[0];
    } else {
      // fallback if no entries or exits. Should ideally never happen
      // could end up spawning in a wall
      pendingArrivalPosition = new PositionConfig();
      pendingArrivalPosition.x = 5;
      pendingArrivalPosition.y = 5;
    }
  }
}
