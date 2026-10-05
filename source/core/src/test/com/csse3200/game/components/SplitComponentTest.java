package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Splitting is triggered by hit reactions, which fire from collision events while the physics world
 * is locked. A locked world cannot create the children's bodies, so the split must be deferred
 * until the entity service runs its update.
 */
@ExtendWith(GameExtension.class)
class SplitComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);

    ServiceLocator.registerPhysicsService(new PhysicsService());

    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(new String[] {"images/crab.atlas", "images/medusa.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  private Entity createSplitEnemy() {
    return createSplitEnemy("images/crab.atlas");
  }

  private Entity createSplitEnemy(String skin) {
    Entity enemy = NPCFactory.createChaseEnemy(new Entity(), true, skin);
    enemy.create();
    return enemy;
  }

  @SuppressWarnings("unchecked")
  private static EventListener1<Entity> addChildListener(Entity enemy) {
    EventListener1<Entity> childListener = mock(EventListener1.class);
    enemy.getEvents().addListener("spawnChildren", childListener);
    return childListener;
  }

  @Test
  void shouldNotSpawnChildrenBeforeEntityServiceUpdate() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);

    enemy.getEvents().trigger("hitReaction", (Entity) null);
    enemy.getComponent(CombatStatsComponent.class).setHealth(0);

    verify(childListener, times(0)).handle(any());
  }

  @Test
  void shouldScaleChildHealthAndAttackWithTheParent() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);
    CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
    enemyStats.scale(1);
    assertEquals(60, enemyStats.getMaxHealth());
    assertEquals(9, enemyStats.getBaseAttack());

    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();

    ArgumentCaptor<Entity> childCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(childListener, times(2)).handle(childCaptor.capture());
    for (Entity child : childCaptor.getAllValues()) {
      CombatStatsComponent childStats = child.getComponent(CombatStatsComponent.class);
      assertEquals(45, childStats.getHealth());
      assertEquals(45, childStats.getMaxHealth());
      assertEquals(4, childStats.getBaseAttack());
    }
  }

  @Test
  void shouldSplitCrabAfterFourNormalHits() {
    assertSplitAfterFourNormalHits("images/crab.atlas");
  }

  @Test
  void shouldSplitMedusaAfterFourNormalHits() {
    assertSplitAfterFourNormalHits("images/medusa.atlas");
  }

  private void assertSplitAfterFourNormalHits(String skin) {
    Entity enemy = createSplitEnemy(skin);
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    EventListener1<Entity> childListener = addChildListener(enemy);
    assertEquals(40, stats.getHealth());
    assertEquals(40, stats.getMaxHealth());
    assertEquals(6, stats.getBaseAttack());

    for (int hit = 1; hit <= 3; hit++) {
      stats.takeDamage(10);
      entityService.update();
      assertEquals(40 - 10 * hit, stats.getHealth());
      verify(childListener, times(0)).handle(any());
    }

    stats.takeDamage(10);
    assertEquals(0, stats.getHealth());
    verify(childListener, times(0)).handle(any());
    entityService.update();

    ArgumentCaptor<Entity> childCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(childListener, times(2)).handle(childCaptor.capture());
    for (Entity child : childCaptor.getAllValues()) {
      CombatStatsComponent childStats = child.getComponent(CombatStatsComponent.class);
      assertEquals(30, childStats.getHealth());
      assertEquals(30, childStats.getMaxHealth());
      assertEquals(3, childStats.getBaseAttack());
      assertNull(child.getComponent(SplitComponent.class));
    }
  }

  @Test
  void shouldCreateChildrenWithoutSplitComponent() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);

    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();

    ArgumentCaptor<Entity> childCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(childListener, times(2)).handle(childCaptor.capture());

    for (Entity child : childCaptor.getAllValues()) {
      assertNull(child.getComponent(SplitComponent.class));
    }
  }

  @Test
  void shouldSplitOnlyOnce() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);
    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();
    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();

    verify(childListener, times(2)).handle(any());
  }

  @Test
  void shouldDisposeOriginalOnEntityServiceUpdate() {
    Entity enemy = createSplitEnemy();

    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    verify(entityService, times(0)).unregister(enemy);

    entityService.update();
    verify(entityService, times(1)).unregister(enemy);
  }
}
