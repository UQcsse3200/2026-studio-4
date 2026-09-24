package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
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
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class EnemyManagerComponentTest {
  private Entity room;
  private EnemyManagerComponent enemyManager;
  private EntityService entityService;

  private static Entity createMockRoom() {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);

    // A real EventHandler, so triggers actually reach listeners.
    when(room.getEvents()).thenReturn(new EventHandler());
    when(terrain.getMapBounds(0)).thenAnswer(inv -> new GridPoint2(20, 20));
    when(terrain.getTileSize()).thenReturn(1f);
    when(terrain.tileToWorldPosition(Mockito.any())).thenReturn(new Vector2());
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);

    return room;
  }

  @BeforeEach
  void setUp() {
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    for (ItemType itemType : ItemType.values()) {
      when(resourceService.getAsset(itemType.getTexturePath(), Texture.class)).thenReturn(texture);
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
  void shouldClearRoomWhenLastEnemyDies() {
    Entity[] enemies = trackEnemies(3);
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);

    for (Entity enemy : enemies) {
      enemy.getEvents().trigger("entityDied");
    }

    assertEquals(1, cleared[0], "roomCleared should fire once all enemies are dead");
    assertTrue(enemyManager.isCleared());
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
  void shouldSpawnRegisteredItemAtEnemyDeathPositionAfterUpdate() {
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
    assertEquals(deathPosition, drop.getPosition());
    verify(entityService).register(drop);
  }

  @Test
  void shouldSpawnChosenRoomItemWithoutEnemyDeath() {
    Vector2 position = new Vector2(3f, 4f);

    enemyManager.spawnItem(ItemType.HEALTH_POTION, 1, position);
    verify(entityService, never()).register(Mockito.any(Entity.class));
    position.set(9f, 9f);
    entityService.update();

    assertEquals(1, entityService.getEntities().size);
    Entity item = entityService.getEntities().first();
    assertEquals(ItemType.HEALTH_POTION, item.getComponent(ItemComponent.class).getItemType());
    assertEquals(new Vector2(3f, 4f), item.getPosition());
    enemyManager.dispose();
    verify(entityService).unregister(item);
  }

  @Test
  void shouldUseEnemyTypeRulesAtDeathWithoutChangingSpawnDrops() {
    LootTable table = singleItemTable(ItemType.HEALTH_POTION);
    table.enemyRules =
        new LootTable.EnemyRule[] {
          new LootTable.EnemyRule("GOLEM", singleItemTable(ItemType.GOLD_COIN))
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
        ItemType.GOLD_COIN,
        entityService.getEntities().get(0).getComponent(ItemComponent.class).getItemType());
    assertEquals(
        ItemType.HEALTH_POTION,
        entityService.getEntities().get(1).getComponent(ItemComponent.class).getItemType());
  }

  @Test
  void shouldKeepEnemyTypeForEverySplitChild() {
    LootTable table = singleItemTable(ItemType.HEALTH_POTION);
    table.enemyRules =
        new LootTable.EnemyRule[] {
          new LootTable.EnemyRule("GOLEM", singleItemTable(ItemType.GOLD_COIN))
        };
    enemyManager =
        new EnemyManagerComponent(new EnemySpawnConfig[0], new ItemFactory(table, fixedDrop(0)));
    enemyManager.setEntity(room);
    Entity parent = enemyMock();
    Entity firstChild = enemyMock();
    Entity secondChild = enemyMock();
    enemyManager.track(parent, "GOLEM");

    parent.getEvents().trigger("spawnChildren", firstChild);
    parent.getEvents().trigger("spawnChildren", secondChild);
    firstChild.getEvents().trigger("entityDied");
    secondChild.getEvents().trigger("entityDied");
    entityService.update();

    int goldDrops = 0;
    for (Entity spawned : entityService.getEntities()) {
      ItemComponent item = spawned.getComponent(ItemComponent.class);
      if (item != null && item.getItemType() == ItemType.GOLD_COIN) {
        goldDrops++;
      }
    }
    assertEquals(2, goldDrops);
  }

  @Test
  void shouldSpawnSelectedCharmsAsSeparateRoomOwnedEntities() {
    LootTable table = new LootTable();
    table.rolls = 2;
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemType.SPEED_CHARM, 1, 1, 1)};
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
      assertEquals(ItemType.SPEED_CHARM, drop.getComponent(ItemComponent.class).getItemType());
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
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);
    enemyManager.track(parent);

    parent.getEvents().trigger("spawnChildren", firstChild);
    parent.getEvents().trigger("spawnChildren", secondChild);
    firstChild.getEvents().trigger("entityDied");

    assertFalse(enemyManager.isCleared());
    assertEquals(0, cleared[0]);
    secondChild.getEvents().trigger("entityDied");
    assertTrue(enemyManager.isCleared());
    assertEquals(1, cleared[0]);
    verify(entityService).register(firstChild);
    verify(entityService).register(secondChild);
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

  private static LootTable singleItemTable(ItemType itemType) {
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
    when(enemy.getCenterPosition()).thenReturn(new Vector2());
    return enemy;
  }
}
