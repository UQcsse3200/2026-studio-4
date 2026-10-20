package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.boss.FinalBossExplosiveSummonComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusDeathComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class EnemyManagerComponentTest {
  private Entity room;
  private RoomConfig roomConfig;
  private EnemyManagerComponent enemyManager;
  private EntityService entityService;

  private static Entity createMockRoom() {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);

    // A real EventHandler, so triggers actually reach listeners.
    when(room.getEvents()).thenReturn(new EventHandler());
    when(terrain.getMapBounds(0)).thenAnswer(inv -> new GridPoint2(20, 20));
    when(terrain.getTileSize()).thenReturn(1f);
    when(terrain.tileToWorldPosition(Mockito.any(GridPoint2.class)))
        .thenAnswer(
            invocation -> {
              GridPoint2 tile = invocation.getArgument(0);
              return new Vector2((float) tile.x, (float) tile.y);
            });
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);

    return room;
  }

  @BeforeEach
  void setUp() {
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ResourceService resourceService = spy(ResourceService.class);
    ServiceLocator.registerRoomClearService(new RoomClearService());
    ServiceLocator.registerResourceService(resourceService);
    WorldConfig worldConfig = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");

    ServiceLocator.registerRunTimer(mock(RunTimer.class));

    RoomAssets roomAssets = spy(RoomAssets.class);
    roomAssets.loadAll();
    roomConfig = worldConfig.getRoom((worldConfig.startRoomId));

    Texture texture = mock(Texture.class);
    for (String itemId : ItemCatalog.ids()) {
      when(resourceService.getAsset(ItemCatalog.create(itemId, 1).getTexture(), Texture.class))
          .thenReturn(texture);
    }
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);

    room = createMockRoom();
    enemyManager =
        new EnemyManagerComponent(
            new EnemySpawnConfig[0], new ItemFactory(LootTable.defaultTable(), fixedDrop(5)));
    enemyManager.setEntity(room);
    enemyManager.create();
  }

  @Test
  void shouldSkipRejectedSpawnAndClearAfterRemainingEnemyDies() {
    EnemySpawnConfig rejectedSpawn = new EnemySpawnConfig();
    rejectedSpawn.type = EnemySpawnConfig.EnemyType.BUG;
    rejectedSpawn.x = 1;
    rejectedSpawn.y = 1;

    EnemySpawnConfig validSpawn = new EnemySpawnConfig();
    validSpawn.type = EnemySpawnConfig.EnemyType.WOLF;
    validSpawn.x = 3;
    validSpawn.y = 3;

    enemyManager =
        spy(
            new EnemyManagerComponent(
                new EnemySpawnConfig[] {rejectedSpawn, validSpawn},
                new ItemFactory(LootTable.defaultTable(), fixedDrop(5))));
    enemyManager.setEntity(room);

    // Isolate spawn-loop behaviour from the separate wall geometry rules.
    doReturn(true).when(enemyManager).outOfBounds(eq(new Vector2(1f, 1f)), any());
    doReturn(false).when(enemyManager).outOfBounds(eq(new Vector2(3f, 3f)), any());

    Entity target = new Entity();
    Entity rejectedEnemy = new Entity();
    Entity validEnemy = new Entity();

    try (MockedStatic<NPCFactory> factory = Mockito.mockStatic(NPCFactory.class)) {
      factory.when(() -> NPCFactory.createBug(target)).thenReturn(rejectedEnemy);
      factory.when(() -> NPCFactory.createWolf(target)).thenReturn(validEnemy);

      enemyManager.spawnEnemies(target);

      assertEquals(1, entityService.getEntities().size);
      verify(entityService).register(validEnemy);
      verify(entityService, never()).register(rejectedEnemy);
      factory.verify(() -> NPCFactory.createBug(target), never());
      factory.verify(() -> NPCFactory.createWolf(target));
      assertFalse(enemyManager.isCleared());

      validEnemy.getEvents().trigger("entityDied");

      assertTrue(enemyManager.isCleared());
    }
  }

  @Test
  void shouldUseInjectedLayoutForInitialSpawns() {
    EnemySpawnConfig blockedSpawn = new EnemySpawnConfig();
    blockedSpawn.type = EnemySpawnConfig.EnemyType.BUG;
    blockedSpawn.x = 1;
    blockedSpawn.y = 1;
    EnemySpawnConfig validSpawn = new EnemySpawnConfig();
    validSpawn.type = EnemySpawnConfig.EnemyType.WOLF;
    validSpawn.x = 3;
    validSpawn.y = 3;
    enemyManager =
        new EnemyManagerComponent(
            new EnemySpawnConfig[] {blockedSpawn, validSpawn}, customRoomLayout(), null);
    enemyManager.setEntity(room);
    Entity target = new Entity();
    Entity blockedEnemy = new Entity();
    Entity validEnemy = new Entity();

    try (MockedStatic<NPCFactory> factory = Mockito.mockStatic(NPCFactory.class)) {
      factory.when(() -> NPCFactory.createBug(target)).thenReturn(blockedEnemy);
      factory.when(() -> NPCFactory.createWolf(target)).thenReturn(validEnemy);

      enemyManager.spawnEnemies(target);

      assertEquals(1, entityService.getEntities().size);
      verify(entityService).register(validEnemy);
      verify(entityService, never()).register(blockedEnemy);
      factory.verify(() -> NPCFactory.createBug(target), never());
    }
  }

  @Test
  void shouldUseInjectedLayoutForSplitChildren() {
    enemyManager = new EnemyManagerComponent(new EnemySpawnConfig[0], customRoomLayout(), null);
    enemyManager.setEntity(room);
    Entity parent = new Entity();
    Entity blockedChild = new Entity();
    blockedChild.setPosition(1f, 1f);
    Entity validChild = new Entity();
    validChild.setPosition(3f, 3f);
    enemyManager.track(parent);

    parent.getEvents().trigger("spawnChildren", blockedChild);
    parent.getEvents().trigger("spawnChildren", validChild);

    assertEquals(2, entityService.getEntities().size);
    verify(entityService).register(blockedChild);
    verify(entityService).register(validChild);
    assertEquals(new Vector2(1f, 2f), blockedChild.getPosition());
    assertFalse(enemyManager.isCleared());

    blockedChild.getEvents().trigger("entityDied");
    validChild.getEvents().trigger("entityDied");
    entityService.update();
    assertTrue(enemyManager.isCleared());

    int goldDrops = 0;
    for (Entity spawned : entityService.getEntities()) {
      ItemComponent item = spawned.getComponent(ItemComponent.class);
      if (item != null && ItemIds.GOLD_COIN.equals(item.getItemId())) {
        goldDrops++;
      }
    }
    assertEquals(2, goldDrops);
  }

  @Test
  void shouldRequireLayoutForRoomAwareConstructor() {
    assertThrows(
        NullPointerException.class,
        () -> new EnemyManagerComponent(new EnemySpawnConfig[0], null, null));
  }

  private static RoomConfig customRoomLayout() {
    RoomConfig config = new RoomConfig();
    // Deliberately absent from rooms.json: the supplied layout must be used directly.
    config.id = "custom-spawn-regression";
    config.mapWidth = 5;
    config.mapHeight = 5;
    config.obstacles.spawns = List.of("33333", "3#333", "33333", "33333", "33333");
    return config;
  }

  /** Registers n enemies with the manager and returns them. */
  private Entity[] trackEnemies(int n) {
    Entity[] enemies = new Entity[n];
    for (int i = 0; i < n; i++) {
      Entity enemy = mock(Entity.class);
      when(enemy.getEvents()).thenReturn(new EventHandler());
      when(enemy.getPosition()).thenReturn(new Vector2());
      enemyManager.track(enemy);
      enemies[i] = enemy;
    }
    return enemies;
  }

  @Test
  void shouldNotClearRoomWhileEnemiesRemain() {
    Entity[] enemies = trackEnemies(3);
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);

    enemies[0].getEvents().trigger("entityDied");
    enemies[1].getEvents().trigger("entityDied");

    assertEquals(0, cleared[0], "roomCleared fired before the last enemy died");
    assertFalse(enemyManager.isCleared());
  }

  @Test
  void shouldNotifiyOnUpdateWithNoMoreActiveEnemies() {
    int[] cleared = {0}; // array workaround for anon lambdas
    var spawns = new EnemySpawnConfig[] {new EnemySpawnConfig()};
    var enemy = new EnemyManagerComponent(spawns);
    var config = new RoomConfig();
    config.id = "asdfasdf";
    var roomData = new RoomDataComponent(config);
    var roomL = new Entity().addComponent(enemy).addComponent(roomData);

    roomL.getEvents().addListener("RoomClear", a -> cleared[0]++);

    enemy.clear();
    enemy.update();
    enemy.update();

    assertEquals(0, cleared[0]);
    assertTrue(enemy.isCleared());
  }

  @Test
  void shouldNotSpawnQueuedRewardsAfterOwningRoomIsDisposed() {
    Entity enemy = new Entity();
    enemyManager.track(enemy);
    enemy.getEvents().trigger("entityDied");
    enemyManager.dispose();
    entityService.update();
    enemy.getEvents().trigger("entityDied");
    entityService.update();
    verify(entityService, never()).register(Mockito.any(Entity.class));
  }

  @Test
  void shouldRequestEachEnemyTypesLootOnceDespiteRepeatedDeathEvents() {
    ItemFactory rewards = mock(ItemFactory.class);
    enemyManager = new EnemyManagerComponent(new EnemySpawnConfig[0], rewards);
    enemyManager.setEntity(room);
    for (EnemyType type : EnemyType.values()) {
      Entity enemy = new Entity();
      enemyManager.track(enemy, type.name());
      enemy.getEvents().trigger("entityDied");
      enemy.getEvents().trigger("entityDied");
      enemy.getEvents().trigger("finalBossEncounterCompleted");
    }

    entityService.update();

    for (EnemyType type : EnemyType.values()) {
      verify(rewards).createEnemyDrops(eq(type.name()), any(Vector2.class));
    }
  }

  @Test
  void shouldRewardExplosiveSummonsOnceWithoutHoldingTheBossRoomOpen() {
    ItemFactory rewards = mock(ItemFactory.class);
    enemyManager = new EnemyManagerComponent(new EnemySpawnConfig[0], rewards);
    enemyManager.setEntity(room);
    Entity boss = new Entity();
    enemyManager.track(boss, "FINAL_BOSS");
    Entity defeatedSummon = explosiveSummonMock();
    Entity survivingSummon = explosiveSummonMock();
    enemyManager.spawnFinalBossEntity(defeatedSummon);
    enemyManager.spawnFinalBossEntity(survivingSummon);

    defeatedSummon.getEvents().trigger("entityDied");
    defeatedSummon.getEvents().trigger("entityDied");
    assertFalse(enemyManager.isCleared());
    verify(rewards, never()).createEnemyDrops(any(), any());
    entityService.update();
    verify(rewards).createEnemyDrops("FINAL_BOSS_SUMMON", new Vector2(0.5f, 0.5f));

    boss.getEvents().trigger("finalBossEncounterCompleted");
    assertTrue(enemyManager.isCleared(), "A living summon must not block encounter completion");
    entityService.update();
    verify(rewards).createEnemyDrops(eq("FINAL_BOSS"), any(Vector2.class));
  }

  @Test
  void shouldNotRewardScriptedBossObjectsOrSummonCleanup() {
    ItemFactory rewards = mock(ItemFactory.class);
    enemyManager = new EnemyManagerComponent(new EnemySpawnConfig[0], rewards);
    enemyManager.setEntity(room);
    Entity scriptedObject = enemyMock();
    Entity cleanedUpSummon = explosiveSummonMock();
    enemyManager.spawnFinalBossEntity(scriptedObject);
    enemyManager.spawnFinalBossEntity(cleanedUpSummon);

    scriptedObject.getEvents().trigger("entityDied");
    entityService.scheduleDisposal(cleanedUpSummon);
    entityService.update();

    verify(rewards, never()).createEnemyDrops(any(), any());
  }

  @Test
  void shouldCancelSummonRewardsWhenTheRoomIsDisposed() {
    ItemFactory rewards = mock(ItemFactory.class);
    enemyManager = new EnemyManagerComponent(new EnemySpawnConfig[0], rewards);
    enemyManager.setEntity(room);
    Entity summon = explosiveSummonMock();
    enemyManager.spawnFinalBossEntity(summon);
    summon.getEvents().trigger("entityDied");

    enemyManager.dispose();
    entityService.update();
    summon.getEvents().trigger("entityDied");
    entityService.update();

    verify(rewards, never()).createEnemyDrops(any(), any());
  }

  @Test
  void splitChildrenShouldKeepTheirParentsLootType() {
    ItemFactory rewards = mock(ItemFactory.class);
    enemyManager = spy(new EnemyManagerComponent(new EnemySpawnConfig[0], rewards));
    // Geometry is covered separately; isolate inheritance of the parent's loot rule here.
    doReturn(false).when(enemyManager).outOfBounds(any(Vector2.class), any());
    enemyManager.setEntity(room);
    Entity parent = enemyMock();
    Entity child = enemyMock();
    enemyManager.track(parent, "CRAB");
    parent.getEvents().trigger("spawnChildren", child);
    child.getEvents().trigger("entityDied");
    entityService.update();

    verify(rewards).createEnemyDrops(eq("CRAB"), any(Vector2.class));
  }

  @Test
  void shouldSpawnRegisteredItemAtEnemyCentreAfterUpdate() {
    Entity enemy = new Entity();
    Vector2 deathPosition = new Vector2(3f, 4f);
    enemy.setPosition(deathPosition);
    enemyManager.track(enemy);

    enemy.getEvents().trigger("entityDied");
    verify(entityService, never()).register(Mockito.any(Entity.class));

    entityService.update();

    assertEquals(1, entityService.getEntities().size);
    Entity drop = entityService.getEntities().first();
    assertNotNull(drop.getComponent(ItemComponent.class));
    assertEquals(enemy.getCenterPosition(), drop.getCenterPosition());
    verify(entityService).register(drop);
  }

  @Test
  void shouldSpawnChosenRoomItemWithoutEnemyDeath() {
    Vector2 position = new Vector2(3f, 4f);

    enemyManager.spawnItem(ItemIds.HEALTH_POTION, 2, position);
    verify(entityService, never()).register(Mockito.any(Entity.class));
    position.set(9f, 9f);
    entityService.update();

    assertEquals(1, entityService.getEntities().size);
    Entity item = entityService.getEntities().first();
    assertEquals(ItemIds.HEALTH_POTION, item.getComponent(ItemComponent.class).getItemId());
    assertEquals(2, item.getComponent(ItemComponent.class).getQuantity());
    assertEquals(new Vector2(3f, 4f), item.getPosition());
    enemyManager.dispose();
    verify(entityService).unregister(item);
  }

  @Test
  void shouldSpawnMultipleSelectedCharmsAsIndependentRoomItems() {
    Vector2 position = new Vector2(3f, 4f);
    enemyManager.spawnItem(ItemIds.SPEED_CHARM, 2, position);
    verify(entityService, never()).register(Mockito.any(Entity.class));

    entityService.update();

    assertEquals(2, entityService.getEntities().size);
    Entity first = entityService.getEntities().get(0);
    Entity second = entityService.getEntities().get(1);
    assertNotNull(first.getComponent(ItemComponent.class));
    assertNotNull(second.getComponent(ItemComponent.class));
    assertEquals(ItemIds.SPEED_CHARM, first.getComponent(ItemComponent.class).getItemId());
    assertEquals(ItemIds.SPEED_CHARM, second.getComponent(ItemComponent.class).getItemId());
    assertEquals(position, first.getPosition());
    assertEquals(position, second.getPosition());
    enemyManager.dispose();
    verify(entityService).unregister(first);
    verify(entityService).unregister(second);
  }

  @Test
  void shouldUseEnemyTypeRulesAtDeathWithoutChangingSpawnDrops() {
    LootTable table = singleItemTable(ItemIds.HEALTH_POTION);
    table.enemyRules =
        new LootTable.EnemyRule[] {
          new LootTable.EnemyRule("GOLEM", singleItemTable(ItemIds.GOLD_COIN))
        };
    enemyManager =
        new EnemyManagerComponent(new EnemySpawnConfig[0], new ItemFactory(table, fixedDrop(0)));
    enemyManager.setEntity(room);
    Entity golem = new Entity();
    Entity medusa = new Entity();
    enemyManager.track(golem, "GOLEM");
    enemyManager.track(medusa, "MEDUSA");

    golem.getEvents().trigger("entityDied");
    medusa.getEvents().trigger("entityDied");
    entityService.update();

    assertEquals(2, entityService.getEntities().size);
    assertEquals(
        ItemIds.GOLD_COIN,
        entityService.getEntities().get(0).getComponent(ItemComponent.class).getItemId());
    assertEquals(
        ItemIds.HEALTH_POTION,
        entityService.getEntities().get(1).getComponent(ItemComponent.class).getItemId());
  }

  @Test
  void shouldSpawnSelectedCharmsAsSeparateRoomOwnedEntities() {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemIds.SPEED_CHARM, 1, 2, 2)};
    enemyManager =
        new EnemyManagerComponent(new EnemySpawnConfig[0], new ItemFactory(table, fixedDrop(0)));
    enemyManager.setEntity(room);
    Entity enemy = new Entity();
    enemyManager.track(enemy, "GOLEM");

    enemy.setPosition(3f, 4f);
    enemy.getEvents().trigger("entityDied");
    entityService.update();

    List<Entity> drops = new ArrayList<>();
    for (Entity drop : entityService.getEntities()) {
      drops.add(drop);
    }
    assertEquals(2, drops.size());
    for (Entity drop : drops) {
      assertEquals(ItemIds.SPEED_CHARM, drop.getComponent(ItemComponent.class).getItemId());
      verify(entityService).register(drop);
    }
    enemyManager.dispose();
    drops.forEach(drop -> verify(entityService).unregister(drop));
  }

  @Test
  void shouldAllowARestrictedTableToProduceNoDrop() {
    LootTable table = new LootTable();
    table.noDropWeight = 1;
    enemyManager =
        new EnemyManagerComponent(new EnemySpawnConfig[0], new ItemFactory(table, fixedDrop(0)));
    enemyManager.setEntity(room);
    Entity enemy = new Entity();
    enemyManager.track(enemy, "GOLEM");

    enemy.getEvents().trigger("entityDied");
    entityService.update();
    assertTrue(entityService.getEntities().isEmpty());
    verify(entityService, never()).register(Mockito.any(Entity.class));
  }

  @Test
  void shouldReplaceSplitParentWithTrackedChildren() {
    Entity parent = enemyMock();
    Entity firstChild = enemyMock();
    Entity secondChild = enemyMock();
    parent.setPosition(new Vector2(2, 2));
    firstChild.setPosition(new Vector2(2, 2));
    secondChild.setPosition(new Vector2(10, 10));
    assertTrue(enemyManager.isCleared());
    assertTrue(enemyManager.outOfBounds(firstChild.getPosition(), roomConfig));
    assertTrue(enemyManager.outOfBounds(secondChild.getPosition(), roomConfig));

    enemyManager.spawnEnemies(firstChild);
    enemyManager.spawnEnemies(secondChild);
    verify(entityService, never()).register(firstChild);
    verify(entityService, never()).register(secondChild);

    assertTrue(enemyManager.isCleared());
  }

  @Test
  void shouldClearEveryLivingEnemy() {
    Entity first = combatEnemy();
    Entity second = combatEnemy();
    enemyManager.track(first);
    enemyManager.track(second);

    enemyManager.clear();

    assertTrue(first.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(second.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(enemyManager.isCleared());
  }

  @Test
  void shouldScaleAllEnemies() {
    Entity[] enemies = trackEnemies(3);
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    for (Entity enemy : enemies) {
      when(enemy.getComponent(CombatStatsComponent.class)).thenReturn(stats);
    }

    enemyManager.scale(1);

    verify(stats, times(3)).scale(anyInt());
  }

  private static RandomGenerator fixedDrop(int index) {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(6)).thenReturn(index);
    return random;
  }

  private static LootTable singleItemTable(String itemType) {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(itemType, 1, 1, 1)};
    return table;
  }

  private Entity combatEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 1));
    entityService.register(enemy);
    return enemy;
  }

  private static Entity enemyMock() {
    Entity enemy = mock(Entity.class);
    when(enemy.getEvents()).thenReturn(new EventHandler());
    when(enemy.getPosition()).thenReturn(new Vector2());
    when(enemy.getCenterPosition()).thenReturn(new Vector2(0.5f, 0.5f));
    return enemy;
  }

  private static Entity explosiveSummonMock() {
    Entity summon = enemyMock();
    FinalBossExplosiveSummonComponent explosive = mock(FinalBossExplosiveSummonComponent.class);
    when(summon.getComponent(FinalBossExplosiveSummonComponent.class)).thenReturn(explosive);
    return summon;
  }

  @Test
  void shouldReportCerberusKillOnlyAfterAllHeadsDie() {
    AchievementService achievements = mock(AchievementService.class);
    ServiceLocator.registerAchievementService(achievements);

    Entity left = new Entity().addComponent(new CombatStatsComponent(125, 10));
    Entity middle =
        new Entity()
            .addComponent(new CombatStatsComponent(250, 20))
            .addComponent(new EnemyTypeComponent(EnemySpawnConfig.EnemyType.CERBERUS));
    Entity right = new Entity().addComponent(new CombatStatsComponent(125, 10));

    CerberusDeathComponent death = new CerberusDeathComponent(left, right);
    middle.addComponent(death);

    enemyManager.track(left, "CERBERUS");
    enemyManager.track(right, "CERBERUS");
    enemyManager.track(middle, "CERBERUS");
    death.create();

    // Killing the middle head must not count as defeating the entire boss.
    middle.getComponent(CombatStatsComponent.class).setHealth(0);
    verify(achievements, never()).update(any(AchievementContext.class));
    assertFalse(enemyManager.isCleared());

    left.getComponent(CombatStatsComponent.class).setHealth(0);
    verify(achievements, never()).update(any(AchievementContext.class));
    assertFalse(enemyManager.isCleared());

    right.getComponent(CombatStatsComponent.class).setHealth(0);

    ArgumentCaptor<AchievementContext> context = ArgumentCaptor.forClass(AchievementContext.class);
    verify(achievements, times(1)).update(context.capture());
    assertEquals(EnemySpawnConfig.EnemyType.CERBERUS, context.getValue().enemyKilled);
    assertTrue(enemyManager.isCleared());

    // Repeated death notifications must not report another kill.
    middle.getEvents().trigger("entityDied");
    left.getEvents().trigger("entityDied");
    right.getEvents().trigger("entityDied");

    verify(achievements, times(1)).update(any(AchievementContext.class));
  }
}
