package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);

    room = createMockRoom();
    enemyManager = new EnemyManagerComponent();
    enemyManager.setEntity(room);
    enemyManager.create();
  }

  /** Registers n enemies with the manager and returns them. */
  private Entity[] trackEnemies(int n) {
    Entity[] enemies = new Entity[n];
    for (int i = 0; i < n; i++) {
      Entity enemy = mock(Entity.class);
      when(enemy.getEvents()).thenReturn(new EventHandler());
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
  void shouldRegisterStrengthCharmAtDefeatedEnemyPosition() {
    Vector2 deathPosition = new Vector2(4f, 6f);
    Entity enemy = new Entity();
    enemy.setPosition(deathPosition);
    enemyManager.track(enemy);

    enemy.getEvents().trigger("entityDied");
    entityService.update();

    ArgumentCaptor<Entity> dropCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(dropCaptor.capture());
    Entity drop = dropCaptor.getValue();
    ItemComponent item = drop.getComponent(ItemComponent.class);

    assertEquals(deathPosition, drop.getPosition());
    assertNotNull(item);
    assertEquals("Strength Charm", item.getCharm().getName());
    assertEquals(PhysicsLayer.ITEM, drop.getComponent(HitboxComponent.class).getLayer());
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

  private Entity combatEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 1));
    entityService.register(enemy);
    return enemy;
  }

  private static Entity enemyMock() {
    Entity enemy = mock(Entity.class);
    when(enemy.getEvents()).thenReturn(new EventHandler());
    when(enemy.getCenterPosition()).thenReturn(new Vector2());
    return enemy;
  }
}
