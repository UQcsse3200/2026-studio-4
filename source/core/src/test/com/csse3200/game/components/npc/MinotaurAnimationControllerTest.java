package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.MinotaurAttackRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MinotaurAnimationControllerTest {
  private Entity enemy;
  private Entity target;
  private AnimationRenderComponent walking;
  private MinotaurAttackRenderComponent attacks;
  private MinotaurAnimationController controller;
  private GameTime time;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerEntityService(spy(new EntityService()));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    ResourceService resources = new ResourceService();
    resources.loadTextureAtlases(
        new String[] {"images/minotaur.atlas", "images/minotaur-combat/attacks.atlas"});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    walking =
        new AnimationRenderComponent(
            resources.getAsset("images/minotaur.atlas", TextureAtlas.class));
    walking.addAnimation("default", 0.1f, Animation.PlayMode.LOOP);
    walking.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    walking.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);
    walking.addAnimation("dieAnimation", 0.5f);
    attacks =
        new MinotaurAttackRenderComponent(
            resources.getAsset("images/minotaur-combat/attacks.atlas", TextureAtlas.class));
    target =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    target.setPosition(2f, 0f);
    target.create();
    controller = new MinotaurAnimationController(target);
    enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 2))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(walking)
            .addComponent(attacks)
            .addComponent(controller);
    ServiceLocator.getEntityService().register(enemy);
    walking.startAnimation("default");
  }

  @Test
  void locomotionEventsCannotOverlayOrRestartAnUnfinishedAttack() {
    contact();
    assertNull(walking.getCurrentAnimation());
    controller.update();
    var secondFrame = attacks.getCurrentFrame();
    enemy.getEvents().trigger("chaseStart");
    assertEquals("normalAttack", attacks.getCurrentAnimation());
    assertSame(secondFrame, attacks.getCurrentFrame());
    assertNull(walking.getCurrentAnimation());
    when(time.getDeltaTime()).thenReturn(0.3f);
    controller.update();
    controller.update();
    assertNull(attacks.getCurrentAnimation());
    assertEquals("chase", walking.getCurrentAnimation());
  }

  @Test
  void telegraphUsesWalkAndTaskSwitchCancelsChargeClassification() {
    contact();
    enemy.getEvents().trigger("lungeTelegraphStart");
    assertNull(attacks.getCurrentAnimation());
    assertEquals("move", walking.getCurrentAnimation());
    contact();
    assertNull(attacks.getCurrentAnimation());
    enemy.getEvents().trigger("lungeDashStart");
    contact();
    assertEquals("chargeAttack", attacks.getCurrentAnimation());
    enemy.getEvents().trigger("lungeDashEnd");
    assertEquals(
        "chargeAttack",
        attacks.getCurrentAnimation(),
        "the strike can finish after the dash stops");
    enemy.getEvents().trigger("wanderStart");
    when(time.getDeltaTime()).thenReturn(0.3f);
    controller.update();
    when(time.getDeltaTime()).thenReturn(0.6f);
    controller.update();
    when(time.getDeltaTime()).thenReturn(0.1f);
    controller.update();
    assertEquals("normalAttack", attacks.getCurrentAnimation());
  }

  @Test
  void cancelledTelegraphDoesNotSuppressFutureNormalContact() {
    enemy.getEvents().trigger("lungeTelegraphStart");
    enemy.getEvents().trigger("wanderStart");
    contact();
    assertEquals("normalAttack", attacks.getCurrentAnimation());
  }

  @Test
  void deathStopsAttackAndRetainsExistingDisposalLifecycle() {
    contact();
    enemy.getEvents().trigger("dieAnimation");
    assertNull(attacks.getCurrentAnimation());
    assertEquals("dieAnimation", walking.getCurrentAnimation());
    contact();
    assertNull(attacks.getCurrentAnimation());
    when(time.getDeltaTime()).thenReturn(0.6f);
    walking.render(mock(SpriteBatch.class));
    controller.update();
    controller.update();
    verify(ServiceLocator.getEntityService(), times(1)).scheduleDisposal(enemy);
    assertEquals(20, target.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals("dieAnimation", walking.getCurrentAnimation());
  }

  private void contact() {
    enemy
        .getEvents()
        .trigger(
            "collisionStart",
            enemy.getComponent(HitboxComponent.class).getFixture(),
            target.getComponent(HitboxComponent.class).getFixture());
  }

  @Test
  void laterPhaseTransitionInTheSameGameUpdateCannotHideAnAlreadyAppliedHit() {
    contact();
    when(time.getDeltaTime()).thenReturn(0.4f);
    // Entity component order is not fixed. This second entity sends the same event a later AI
    // component can send after this controller's update, before that frame is rendered.
    ServiceLocator.getEntityService()
        .register(
            new Entity()
                .addComponent(
                    new Component() {
                      @Override
                      public void update() {
                        enemy.getEvents().trigger("lungeTelegraphStart");
                      }
                    }));
    ServiceLocator.getEntityService().update();
    assertEquals(20, target.getComponent(CombatStatsComponent.class).getHealth());
    assertNull(attacks.getCurrentAnimation());
    assertEquals("move", walking.getCurrentAnimation());
  }

  @Test
  void disposedControllerCannotRestartAttack() {
    contact();
    controller.dispose();
    contact();
    assertNull(attacks.getCurrentAnimation());
  }
}
