package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.ExplodeComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BombEnemyTest {

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
    resourceService.loadTextureAtlases(
        new String[] {"images/golem.atlas", "images/bombEnemy.atlas", "images/beetle.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldHaveBombEnemyAnimations() {
    Entity golem = NPCFactory.createBombEnemy(new Entity(), "images/golem.atlas", 0.05f);
    Entity beetle = NPCFactory.createBombEnemy(new Entity(), "images/beetle.atlas", 0.05f);
    AnimationRenderComponent golemAnimator = golem.getComponent(AnimationRenderComponent.class);
    AnimationRenderComponent beetleAnimator = beetle.getComponent(AnimationRenderComponent.class);

    assertTrue(golemAnimator.hasAnimation("move"));
    assertTrue(golemAnimator.hasAnimation("chase"));
    assertTrue(golemAnimator.hasAnimation("dieAnimation"));
    assertTrue(golemAnimator.hasAnimation("default"));
    assertTrue(beetleAnimator.hasAnimation("move"));
    assertTrue(beetleAnimator.hasAnimation("chase"));
    assertTrue(beetleAnimator.hasAnimation("dieAnimation"));
    assertTrue(beetleAnimator.hasAnimation("default"));
  }

  @Test
  void testBombEnemyExplodesAfterFuseTime() {
    Entity player =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player, "images/bombEnemy.atlas", 0.05f);
    bombEnemy.create();

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    EventListener0 fuseStarted = mock(EventListener0.class);
    bombEnemy.getEvents().addListener("fuseStarted", fuseStarted);

    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);

    verify(fuseStarted, times(1)).handle();
  }

  @Test
  void testBombEnemyIgnoresCollisionFromDifferentFixture() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(100, 10));
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player, "images/bombEnemy.atlas", 0.05f);
    bombEnemy.create();

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    EventListener0 fuseStarted = mock(EventListener0.class);
    bombEnemy.getEvents().addListener("fuseStarted", fuseStarted);

    bombEnemy.getEvents().trigger("collisionStart", playerFixture, bombFixture);

    verify(fuseStarted, never()).handle();
  }

  @Test
  void testBombEnemyLightsFuseOnlyOnce() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(100, 10));
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player, "images/bombEnemy.atlas", 0.05f);
    bombEnemy.create();

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    EventListener0 fuseStarted = mock(EventListener0.class);
    bombEnemy.getEvents().addListener("fuseStarted", fuseStarted);

    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);
    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);
    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);

    verify(fuseStarted, times(1)).handle();
  }

  @Test
  void testBombEnemyDoesNotExplodeOnNonPlayerCollision() {
    Entity player =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player, "images/bombEnemy.atlas", 0.05f);
    bombEnemy.create();

    ExplodeComponent explodeComponent = bombEnemy.getComponent(ExplodeComponent.class);

    assertNotNull(explodeComponent);

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();

    Entity otherEntity =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    otherEntity.create();
    Fixture otherFixture = otherEntity.getComponent(HitboxComponent.class).getFixture();

    EventListener0 dieAnimationListener = mock(EventListener0.class);

    bombEnemy.getEvents().addListener("dieAnimation", dieAnimationListener);

    bombEnemy.getEvents().trigger("collisionStart", bombFixture, otherFixture);

    verify(dieAnimationListener, times(0)).handle();
  }

  @Test
  void testBombEnemyDoesNotExplodeOnInvisiblePlayerCollision() {
    GameTime abilityTime = mock(GameTime.class);
    when(abilityTime.getTime()).thenReturn(1_000L);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(abilityTime);

    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player, "images/bombEnemy.atlas", 0.05f);
    bombEnemy.create();

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    EventListener0 fuseStarted = mock(EventListener0.class);
    bombEnemy.getEvents().addListener("fuseStarted", fuseStarted);
    ExplodeComponent explode = bombEnemy.getComponent(ExplodeComponent.class);

    assertTrue(abilities.tryActivate(Invisibility.class));
    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);
    explode.update();

    verify(fuseStarted, times(0)).handle();
    assertFalse(bombEnemy.getComponent(CombatStatsComponent.class).isDead());

    // The player is still standing on the bomb when invisibility wears off: the fuse lights on the
    // next frame without the fixtures having to separate and touch again.
    when(abilityTime.getTime()).thenReturn(1_000L + Invisibility.DURATION_MS);
    explode.update();
    verify(fuseStarted, times(1)).handle();

    // A repeat contact does not light a second fuse.
    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);
    explode.update();
    verify(fuseStarted, times(1)).handle();
  }
}
