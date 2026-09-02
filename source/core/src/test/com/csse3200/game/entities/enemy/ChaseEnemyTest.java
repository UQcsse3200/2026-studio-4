package com.csse3200.game.entities.enemy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChaseEnemyTest {
  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);

    ServiceLocator.registerPhysicsService(new PhysicsService());
    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(new String[] {"images/chaseEnemy.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void testChaseEnemyKnockbackOnPlayerCollision() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(
                new HitboxComponent().setLayer(com.csse3200.game.physics.PhysicsLayer.PLAYER))
            .addComponent(new com.csse3200.game.components.CombatStatsComponent(20, 0));
    player.create();

    Entity chaseEnemy = NPCFactory.createChaseEnemy(player, true);
    chaseEnemy.create();

    Fixture chaseFixture = chaseEnemy.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    @SuppressWarnings("unchecked")
    EventListener1<Entity> hitReactionListener = mock(EventListener1.class);
    player.getEvents().addListener("hitReaction", hitReactionListener);

    chaseEnemy.getEvents().trigger("collisionStart", chaseFixture, playerFixture);

    verify(hitReactionListener, times(1)).handle(any(Entity.class));
  }

  @Test
  void testChaseEnemyDoesNotKnockbackOnNonPlayerCollision() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(
                new HitboxComponent().setLayer(com.csse3200.game.physics.PhysicsLayer.PLAYER))
            .addComponent(new com.csse3200.game.components.CombatStatsComponent(20, 0));
    player.create();

    Entity chaseEnemy = NPCFactory.createChaseEnemy(player, true);
    chaseEnemy.create();

    Entity otherEntity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(
                new HitboxComponent().setLayer(com.csse3200.game.physics.PhysicsLayer.NPC))
            .addComponent(new com.csse3200.game.components.CombatStatsComponent(20, 0));
    otherEntity.create();

    Fixture chaseFixture = chaseEnemy.getComponent(HitboxComponent.class).getFixture();
    Fixture otherFixture = otherEntity.getComponent(HitboxComponent.class).getFixture();

    @SuppressWarnings("unchecked")
    EventListener1<Entity> hitReactionListener = mock(EventListener1.class);
    player.getEvents().addListener("hitReaction", hitReactionListener);

    chaseEnemy.getEvents().trigger("collisionStart", chaseFixture, otherFixture);

    verify(hitReactionListener, times(0)).handle(any(Entity.class));
  }
}
