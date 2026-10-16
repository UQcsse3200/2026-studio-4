package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.minimap.Minimap;
import java.util.HashSet;
import java.util.Set;

/**
 * Owns the active room and applies the room graph specified by {@link WorldConfig}.
 *
 * @deprecated Deprecated in favour of a new {@link com.csse3200.game.rooms.RoomSelector} and
 *     related classes.
 */
@Deprecated(since = "14.10", forRemoval = true)
public class RoomManager {
  /**
   * Triggered on the player with the room id, its dungeon id (null for the hub) and whether the
   * room has no enemies left. Also re-fired when the room is cleared, so the state is never stale.
   */
  public static final String ROOM_ENTERED = "roomEntered";

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
  private FinalBossPhase pendingFinalBossStage;
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
    currentConfig = world.getRoom(world.startRoomId);

    // Move save loading elsewhere
    checkpointRoomId = currentConfig.id;
    checkpointEntryPointId = world.startEntryPointId;
    checkpointPosition = new PositionConfig();
    checkpointPosition.x = initialEntryPoint.x;
    checkpointPosition.y = initialEntryPoint.y;

    // Player setup code
    player.getEvents().addListener("interact", this::interact);
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);
    attachMinimap();
  }

  /** Package private constructer to create empty room manager for testing */
  RoomManager(Entity player) {
    this.player = player;
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
    currentRoom = RoomFactory.createRoom(savedRoom, camera, false);
    initialEntryPoint = spawn;
    initialWorldPosition = null;

    checkpointRoomId = savedRoom.id;
    checkpointEntryPointId = checkpoint.entryPointId;
    checkpointPosition = spawn;

    // If the room can query the player than this can be set in the component
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
    currentRoom = RoomFactory.createRoom(resumeRoom, camera, false);
    initialEntryPoint = null;
    initialWorldPosition = new Vector2(resume.x, resume.y);

    FollowingCameraComponent following = currentRoom.getComponent(FollowingCameraComponent.class);
    following.setCamera(camera);
    following.setTarget(player);
  }

  /** Registers the active room and player, then positions the player at its entry point. */
  public void create() {
    // Figure out whats happening with constructor vs this function?
    currentRoom = RoomFactory.createRoom(currentConfig, camera, false);
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
  }

  private void start(Vector2 worldPosition) {
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    announceRoom();
    scaleRoom(currentRoom);
    player.setPosition(worldPosition);
  }

  /** Package private for testing */
  void start(PositionConfig entryPoint) {
    // this should just use the function it is overloading
    currentRoom.getEvents().addListener("roomCleared", this::onRoomCleared);
    currentRoom.getEvents().trigger("RoomCreated", player);
    announceRoom();
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
    // Try make this use the EntityService schedule.
    RoomConfig destination = pendingDestination;
    PositionConfig arrivalPosition = pendingArrivalPosition;
    // iiuc this was done to prevent room switching from crashing box2d
    FinalBossPhase bossStage = pendingFinalBossStage;
    pendingDestination = null;
    pendingArrivalPosition = null;
    pendingFinalBossStage = null;
    if (bossStage == null) switchToRoom(destination, arrivalPosition);
    else switchToRoom(destination, arrivalPosition, bossStage);
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
      // The room ids are added twice.
      clearedRoomIds.add(currentConfig.id);
    }
    if (exit.completesDungeon) {
      completedDungeonIds.add(currentConfig.dungeonId);
    }
    pendingDestination = destination;
    // If a specific exit destination is set use it, else use entry point????
    if (exit.destinationExitId != null) {
      pendingArrivalPosition = arrivalInsideDoor(destination.getExit(exit.destinationExitId));
    } else {
      pendingArrivalPosition = destination.getEntryPoint(exit.destinationEntryPointId);
    }
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

  /**
   * Tells anything outside the rooms which room the player is now standing in. Fired on the player
   * because the player outlives rooms.
   */
  private void announceRoom() {
    if (player == null || currentConfig == null) {
      return;
    }
    EnemyManagerComponent enemies =
        currentRoom == null ? null : currentRoom.getComponent(EnemyManagerComponent.class);
    boolean cleared = enemies == null || enemies.isCleared();
    //  I think the only place that uses this is the music stuff
    player.getEvents().trigger(ROOM_ENTERED, currentConfig.id, currentConfig.dungeonId, cleared);
  }

  /** Requests that the current room's enemies be cleared at the next safe update point. */
  public void clearCurrentRoom() {
    clearRequested = true;
  }

  private void onRoomCleared() {
    // cleared room id is added
    if (clearedRoomIds.add(currentConfig.id)) {
      showStatus("Room cleared.");
    }
    // The fight is over, so whatever was reacting to the room's state hears about it again.
    announceRoom();
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
    switchToRoom(destination, arrivalPosition, null);
  }

  private void switchToRoom(
      RoomConfig destination, PositionConfig arrivalPosition, FinalBossPhase bossStage) {
    Entity nextRoom =
        RoomFactory.createRoom(
            destination, camera, bossStage == null && clearedRoomIds.contains(destination.id));
    currentRoom.dispose();
    currentConfig = destination;
    currentRoom = nextRoom;

    ServiceLocator.getEntityService().register(currentRoom);
    player.getEvents().trigger(Minimap.ROOM_CHANGE_EVENT, destination);

    start(arrivalPosition);
    rememberCheckpoint(arrivalPosition, null);

    // moves camera to new room?
    FollowingCameraComponent cameraFollowingComponent =
        currentRoom.getComponent(FollowingCameraComponent.class);
    cameraFollowingComponent.setCamera(camera);
    cameraFollowingComponent.setTarget(player);

    if (bossStage != null) {
      // RoomCreated spawns the boss before start() scales it and places the player. Initialise the
      // requested stage only now, using its scaled health and the destination room's camera view.
      cameraFollowingComponent.snapToTarget();
      currentRoom.getComponent(EnemyManagerComponent.class).startFinalBossStage(bossStage);
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

  /** Package private for unit testing */
  void refreshInteractionPrompt() {
    // not sure why the prompt is refreshed here and not internally
    InteractionPromptDisplay display = player.getComponent(InteractionPromptDisplay.class);
    if (display == null) {
      return;
    }
    if (cannotAct()) {
      display.clearPrompt();
      return;
    }
    display.setPrompt(InteractionPrompt.resolve(getNpcPrompt(), getItemPrompt(), getExitPrompt()));
  }

  /**
   * Whether offering the player an interaction would be a lie. An NPC interaction is one case, but
   * so is anything else holding their controls: Hecate's menu, a boss sequence, an ending dialogue.
   * Each takes a lock of its own, so asking about the lock covers all of them.
   */
  private boolean cannotAct() {
    // This should either exist in one of the players components
    // Could introduce a new class that handles the player from a room perspective
    NpcInteractorComponent interactor = player.getComponent(NpcInteractorComponent.class);
    if (interactor != null && interactor.isInteracting()) {
      return true;
    }
    PlayerActions actions = player.getComponent(PlayerActions.class);
    return actions != null && actions.areControlsLocked();
  }

  private String getNpcPrompt() {
    // See line457
    FriendlyNpcManagerComponent npcs = currentRoom.getComponent(FriendlyNpcManagerComponent.class);
    return npcs == null ? null : npcs.getPrompt(player);
  }

  private String getItemPrompt() {
    // See line457
    ItemPickupComponent pickup = player.getComponent(ItemPickupComponent.class);
    return pickup == null ? null : pickup.getPickupPrompt();
  }

  private String getExitPrompt() {
    // See line457
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

  // Used primarily by interact()
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
    if (destination == null) return;
    pendingDestination = destination;
    pendingArrivalPosition = debugArrival(destination);
    pendingFinalBossStage = null;
  }

  /** Queues a fresh boss encounter; invalid requests leave any existing request untouched. */
  boolean debugSwitchFinalBossRoom(RoomConfig destination, FinalBossPhase stage) {
    if (!hasFinalBoss(destination)
        || (stage != FinalBossPhase.STAGE_ONE
            && stage != FinalBossPhase.STAGE_TWO
            && stage != FinalBossPhase.STAGE_THREE)) return false;
    pendingDestination = destination;
    pendingArrivalPosition =
        destination.entryPoints.length > 0 ? destination.entryPoints[0] : debugArrival(destination);
    pendingFinalBossStage = stage;
    clearRequested = false;
    return true;
  }

  /** Stage aliases are available only when the destination really declares a Final Boss. */
  static boolean hasFinalBoss(RoomConfig destination) {
    if (destination == null || destination.enemySpawns == null) return false;
    for (EnemySpawnConfig spawn : destination.enemySpawns) {
      if (spawn != null && spawn.type == EnemySpawnConfig.EnemyType.FINAL_BOSS) return true;
    }
    return false;
  }

  private PositionConfig debugArrival(RoomConfig destination) {
    if (destination.exits.length < 1 && destination.entryPoints.length > 0) {
      return destination.entryPoints[0];
    } else if (destination.exits.length >= 1) {
      return destination.exits[0];
    }
    PositionConfig fallback = new PositionConfig();
    fallback.x = 5;
    fallback.y = 5;
    return fallback;
  }
}
