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
    resourceService.loadTextureAtlases(new String[] {"images/chaseEnemy.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  private Entity createSplitEnemy() {
    Entity enemy = NPCFactory.createChaseEnemy(new Entity(), true);
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

    verify(childListener, times(0)).handle(any());
  }

  @Test
  void shouldSpawnTwoHalvedChildrenOnEntityServiceUpdate() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);
    CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
    int halfHealth = Math.max(1, enemyStats.getMaxHealth() / 2);
    int halfAttack = Math.max(1, enemyStats.getBaseAttack() / 2);

    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();

    ArgumentCaptor<Entity> childCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(childListener, times(2)).handle(childCaptor.capture());
    for (Entity child : childCaptor.getAllValues()) {
      CombatStatsComponent childStats = child.getComponent(CombatStatsComponent.class);
      assertEquals(halfHealth, childStats.getHealth());
      assertEquals(halfHealth, childStats.getMaxHealth());
      assertEquals(halfAttack, childStats.getBaseAttack());
    }
  }

  @Test
  void shouldCreateChildrenWithoutSplitComponent() {
    Entity enemy = createSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);

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

    enemy.getEvents().trigger("hitReaction", (Entity) null);
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();
    enemy.getEvents().trigger("hitReaction", (Entity) null);
    entityService.update();

    verify(childListener, times(2)).handle(any());
  }

  @Test
  void shouldDisposeOriginalOnEntityServiceUpdate() {
    Entity enemy = createSplitEnemy();

    enemy.getEvents().trigger("hitReaction", (Entity) null);
    verify(entityService, times(0)).unregister(enemy);

    entityService.update();
    verify(entityService, times(1)).unregister(enemy);
  }
}
