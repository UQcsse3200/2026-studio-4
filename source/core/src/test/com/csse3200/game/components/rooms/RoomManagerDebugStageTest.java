package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.components.rooms.configs.EntryPointConfig;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

/** Debug stage requests reload a room, then initialise its newly spawned and scaled boss. */
@ExtendWith(GameExtension.class)
class RoomManagerDebugStageTest {
  private RoomConfig hubConfig;
  private RoomConfig bossConfig;
  private RoomConfig dungeonConfig;
  private RoomFixture hub;
  private Entity player;
  private CameraComponent camera;
  private EntityService entities;
  private RoomManager manager;
  private MockedStatic<RoomFactory> roomFactory;

  @BeforeEach
  void setUp() {
    hubConfig = config("hub", null);
    bossConfig = config("finalBossRoom", "finalDungeon");
    EnemySpawnConfig bossSpawn = new EnemySpawnConfig();
    bossSpawn.type = EnemyType.FINAL_BOSS;
    bossConfig.enemySpawns = new EnemySpawnConfig[] {bossSpawn};
    dungeonConfig = config("dungeonOneRoom", "dungeonOne");
    ExitConfig finish = new ExitConfig();
    finish.id = "finish";
    finish.kind = "BOOKSHELF";
    finish.destinationRoomId = hubConfig.id;
    finish.destinationEntryPointId = "entry";
    finish.completesDungeon = true;
    // start(entry) places the player at entry + (2, -2).
    finish.x = 12;
    finish.y = 8;
    dungeonConfig.exits = new ExitConfig[] {finish};
    WorldConfig world = new WorldConfig();
    world.startRoomId = hubConfig.id;
    world.startEntryPointId = "entry";
    world.rooms = new RoomConfig[] {hubConfig, bossConfig, dungeonConfig};

    player = mock(Entity.class);
    when(player.getEvents()).thenReturn(new EventHandler());
    AtomicReference<Vector2> position = new AtomicReference<>(new Vector2());
    when(player.getPosition()).thenAnswer(call -> position.get().cpy());
    when(player.getCenterPosition()).thenAnswer(call -> position.get().cpy());
    doAnswer(
            call -> {
              Vector2 next = call.getArgument(0);
              position.set(next.cpy());
              return null;
            })
        .when(player)
        .setPosition(any(Vector2.class));
    camera = mock(CameraComponent.class);
    entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    ServiceLocator.registerRunTimer(mock(RunTimer.class));
    hub = room(true);
    roomFactory = mockStatic(RoomFactory.class);
    roomFactory
        .when(() -> RoomFactory.createRoom(hubConfig, camera, false, RoomManager.DEFAULT_TILESET))
        .thenReturn(hub.entity());
    manager =
        new RoomManager(world, player, camera) {
          @Override
          void attachMinimap() {
            // This test observes room transitions; no rendering resources are required.
          }
        };
    manager.create();
    clearInvocations(player, entities, hub.entity(), hub.enemies(), hub.following(), hub.events());
    roomFactory.clearInvocations();
  }

  @AfterEach
  void closeFactoryMock() {
    roomFactory.close();
  }

  @Test
  void shouldQueueThenInitialiseTheStageAfterSpawningScalingPlacementAndCameraSetup() {
    RoomFixture bossRoom = room(false);
    freshBossRooms(bossRoom);

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_TWO));

    verify(hub.entity(), never()).dispose();
    verify(bossRoom.enemies(), never()).startFinalBossStage(any());
    assertEquals(hubConfig.id, manager.getCheckpointData().roomId);
    manager.update();

    InOrder order =
        inOrder(entities, bossRoom.events(), bossRoom.enemies(), player, bossRoom.following());
    order.verify(entities).register(bossRoom.entity());
    order.verify(bossRoom.events()).trigger("RoomCreated", player);
    order.verify(bossRoom.enemies()).scale(0);
    order.verify(player).setPosition(new Vector2(12f, 8f));
    order.verify(bossRoom.following()).setCamera(camera);
    order.verify(bossRoom.following()).setTarget(player);
    order.verify(bossRoom.following()).snapToTarget();
    order.verify(bossRoom.enemies()).startFinalBossStage(FinalBossPhase.STAGE_TWO);
    verify(hub.entity()).dispose();
    assertEquals(bossConfig.id, manager.getCheckpointData().roomId);
  }

  @Test
  void shouldReloadFreshInstancesWhenGoingFromStageThreeBackToStageOne() {
    RoomFixture first = room(false);
    RoomFixture second = room(false);
    freshBossRooms(first, second);

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_THREE));
    manager.update();
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_ONE));
    verify(first.entity(), never()).dispose();
    manager.update();

    verify(first.entity()).dispose();
    verify(second.entity(), never()).dispose();
    verify(entities).register(first.entity());
    verify(entities).register(second.entity());
    verify(first.enemies()).startFinalBossStage(FinalBossPhase.STAGE_THREE);
    verify(second.enemies()).startFinalBossStage(FinalBossPhase.STAGE_ONE);
    roomFactory.verify(
        () -> RoomFactory.createRoom(bossConfig, camera, false, RoomManager.DUNGEON5_TILESET),
        times(2));
  }

  @Test
  void shouldPreferTheStageEntryWhileKeepingOrdinaryGotoExitFirst() {
    ExitConfig exit = new ExitConfig();
    exit.x = 35;
    exit.y = 25;
    bossConfig.exits = new ExitConfig[] {exit};
    RoomFixture stageRoom = room(false);
    RoomFixture normalRoom = room(false);
    freshBossRooms(stageRoom, normalRoom);

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_ONE));
    manager.update();

    assertEquals(new Vector2(12f, 8f), player.getPosition());
    assertEquals(10, manager.getCheckpointData().tileX);
    assertEquals(10, manager.getCheckpointData().tileY);

    manager.debugSwitchRoom(bossConfig);
    manager.update();

    assertEquals(new Vector2(37f, 23f), player.getPosition());
    assertEquals(35, manager.getCheckpointData().tileX);
    assertEquals(25, manager.getCheckpointData().tileY);
    verify(normalRoom.enemies(), never()).startFinalBossStage(any());
  }

  @Test
  void shouldForceEnemiesForAClearedBossRoomWithoutErasingItsNormalRevisitHistory() {
    RoomFixture first = room(false);
    RoomFixture fresh = room(false);
    RoomFixture clearedRevisit = room(true);
    freshBossRooms(first, fresh);
    roomFactory
        .when(() -> RoomFactory.createRoom(bossConfig, camera, true, RoomManager.DUNGEON5_TILESET))
        .thenReturn(clearedRevisit.entity());
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_THREE));
    manager.update();
    first.events().trigger("roomCleared");

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_TWO));
    manager.update();
    verify(fresh.enemies()).startFinalBossStage(FinalBossPhase.STAGE_TWO);

    manager.debugSwitchRoom(bossConfig);
    manager.update();

    roomFactory.verify(
        () -> RoomFactory.createRoom(bossConfig, camera, false, RoomManager.DUNGEON5_TILESET),
        times(2));
    roomFactory.verify(
        () -> RoomFactory.createRoom(bossConfig, camera, true, RoomManager.DUNGEON5_TILESET));
    verify(clearedRevisit.enemies(), never()).startFinalBossStage(any());
  }

  @Test
  void shouldLeaveTheCurrentRoomUntouchedForInvalidRequests() {
    assertFalse(manager.debugSwitchFinalBossRoom(null, FinalBossPhase.STAGE_ONE));
    assertFalse(manager.debugSwitchFinalBossRoom(bossConfig, null));
    assertFalse(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.DEFEATED));
    assertFalse(manager.debugSwitchFinalBossRoom(hubConfig, FinalBossPhase.STAGE_TWO));

    manager.update();

    verify(hub.entity(), never()).dispose();
    verify(entities, never()).register(any());
    assertEquals(hubConfig.id, manager.getCheckpointData().roomId);
    assertEquals(new Vector2(12f, 8f), player.getPosition());
  }

  @Test
  void shouldKeepAValidQueuedRequestWhenLaterRequestsAreInvalid() {
    RoomFixture bossRoom = room(false);
    freshBossRooms(bossRoom);
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_TWO));

    assertFalse(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.DEFEATED));
    assertFalse(manager.debugSwitchFinalBossRoom(null, FinalBossPhase.STAGE_THREE));
    assertFalse(manager.debugSwitchFinalBossRoom(hubConfig, FinalBossPhase.STAGE_ONE));
    manager.update();

    verify(bossRoom.enemies()).startFinalBossStage(FinalBossPhase.STAGE_TWO);
    assertEquals(bossConfig.id, manager.getCheckpointData().roomId);
  }

  @Test
  void shouldUseOnlyTheLatestValidStageRequest() {
    RoomFixture bossRoom = room(false);
    freshBossRooms(bossRoom);
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_ONE));
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_THREE));

    manager.update();
    manager.update();

    verify(bossRoom.enemies(), never()).startFinalBossStage(FinalBossPhase.STAGE_ONE);
    verify(bossRoom.enemies()).startFinalBossStage(FinalBossPhase.STAGE_THREE);
    roomFactory.verify(
        () -> RoomFactory.createRoom(bossConfig, camera, false, RoomManager.DUNGEON5_TILESET));
  }

  @Test
  void shouldDiscardThePendingStageWhenAnOrdinaryGotoReplacesIt() {
    RoomFixture normalRoom = room(true);
    roomFactory
        .when(
            () ->
                RoomFactory.createRoom(dungeonConfig, camera, false, RoomManager.DUNGEON1_TILESET))
        .thenReturn(normalRoom.entity());
    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_THREE));

    manager.debugSwitchRoom(dungeonConfig);
    manager.update();

    assertEquals(dungeonConfig.id, manager.getCheckpointData().roomId);
    verify(normalRoom.enemies(), never()).startFinalBossStage(any());
    verify(normalRoom.following(), never()).snapToTarget();
    roomFactory.verify(
        () -> RoomFactory.createRoom(bossConfig, camera, false, RoomManager.DUNGEON5_TILESET),
        never());
  }

  @Test
  void shouldCancelAnEarlierClearRequestInsteadOfClearingTheNewEncounter() {
    RoomFixture bossRoom = room(false);
    freshBossRooms(bossRoom);
    manager.clearCurrentRoom();

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_TWO));
    manager.update();
    manager.update();

    verify(hub.enemies(), never()).clear();
    verify(bossRoom.enemies(), never()).clear();
    verify(bossRoom.enemies()).startFinalBossStage(FinalBossPhase.STAGE_TWO);
  }

  @Test
  void shouldPreserveCompletedDungeonScalingWithoutDispatchingAStaleCompletionOnDebugGoto() {
    RoomFixture dungeon = room(true);
    RoomFixture bossRoom = room(false);
    roomFactory
        .when(
            () ->
                RoomFactory.createRoom(dungeonConfig, camera, false, RoomManager.DUNGEON1_TILESET))
        .thenReturn(dungeon.entity());
    freshBossRooms(bossRoom);
    // Enter through the entry point, so the fixture's completing bookshelf is beside the
    // player.
    manager.switchToRoom(dungeonConfig, dungeonConfig.entryPoints[0]);
    AchievementService achievements = mock(AchievementService.class);
    ServiceLocator.registerAchievementService(achievements);
    manager.interact();
    clearInvocations(achievements);

    assertTrue(manager.debugSwitchFinalBossRoom(bossConfig, FinalBossPhase.STAGE_TWO));
    manager.update();

    verify(bossRoom.enemies()).scale(1);
    verify(bossRoom.enemies()).startFinalBossStage(FinalBossPhase.STAGE_TWO);
    ArgumentCaptor<AchievementContext> contexts = ArgumentCaptor.forClass(AchievementContext.class);
    verify(achievements).update(contexts.capture());
    assertEquals("finalDungeon", contexts.getValue().dungeonEnteredId);
    assertNull(contexts.getValue().dungeonCompletedId);
  }

  private void freshBossRooms(RoomFixture first, RoomFixture... rest) {
    Entity[] later = new Entity[rest.length];
    for (int index = 0; index < rest.length; index++) later[index] = rest[index].entity();
    roomFactory
        .<Entity>when(
            () -> RoomFactory.createRoom(bossConfig, camera, false, RoomManager.DUNGEON5_TILESET))
        .thenReturn(first.entity(), later);
  }

  private static RoomConfig config(String id, String dungeonId) {
    RoomConfig config = new RoomConfig();
    config.id = id;
    config.title = id;
    config.dungeonId = dungeonId;
    EntryPointConfig entry = new EntryPointConfig();
    entry.id = "entry";
    entry.x = 10;
    entry.y = 10;
    config.entryPoints = new EntryPointConfig[] {entry};
    return config;
  }

  private static RoomFixture room(boolean cleared) {
    Entity entity = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    EnemyManagerComponent enemies = mock(EnemyManagerComponent.class);
    FollowingCameraComponent following = mock(FollowingCameraComponent.class);
    EventHandler events = spy(new EventHandler());
    when(terrain.tileToWorldPosition(any(GridPoint2.class)))
        .thenAnswer(
            invocation -> {
              GridPoint2 tile = invocation.getArgument(0);
              return new Vector2(tile.x, tile.y);
            });
    when(enemies.isCleared()).thenReturn(cleared);
    when(entity.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(entity.getComponent(EnemyManagerComponent.class)).thenReturn(enemies);
    when(entity.getComponent(FollowingCameraComponent.class)).thenReturn(following);
    when(entity.getEvents()).thenReturn(events);
    return new RoomFixture(entity, enemies, following, events);
  }

  private record RoomFixture(
      Entity entity,
      EnemyManagerComponent enemies,
      FollowingCameraComponent following,
      EventHandler events) {}
}
