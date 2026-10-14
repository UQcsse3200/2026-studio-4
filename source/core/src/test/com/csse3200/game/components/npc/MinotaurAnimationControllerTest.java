package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
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
    ServiceLocator.registerEntityService(mock(EntityService.class));
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
    target = new Entity();
    target.setPosition(2f, 0f);
    controller = new MinotaurAnimationController(target);
    enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 2))
            .addComponent(walking)
            .addComponent(attacks)
            .addComponent(controller);
    enemy.create();
    walking.startAnimation("default");
  }

  @Test
  void locomotionEventsCannotOverlayOrRestartAnUnfinishedAttack() {
    enemy.getEvents().trigger("touchAttack", target);
    assertNull(walking.getCurrentAnimation());
    attacks.render(mock(SpriteBatch.class));
    var secondFrame = attacks.getCurrentFrame();
    enemy.getEvents().trigger("chaseStart");
    assertEquals("normalAttack", attacks.getCurrentAnimation());
    assertSame(secondFrame, attacks.getCurrentFrame());
    assertNull(walking.getCurrentAnimation());
    when(time.getDeltaTime()).thenReturn(1f);
    attacks.render(mock(SpriteBatch.class));
    controller.update();
    assertNull(attacks.getCurrentAnimation());
    assertEquals("chase", walking.getCurrentAnimation());
  }

  @Test
  void telegraphUsesWalkAndTaskSwitchCancelsChargeClassification() {
    enemy.getEvents().trigger("touchAttack", target);
    enemy.getEvents().trigger("lungeTelegraphStart");
    assertNull(attacks.getCurrentAnimation());
    assertEquals("move", walking.getCurrentAnimation());
    enemy.getEvents().trigger("touchAttack", target);
    assertNull(attacks.getCurrentAnimation());
    enemy.getEvents().trigger("lungeDashStart");
    enemy.getEvents().trigger("touchAttack", target);
    assertEquals("chargeAttack", attacks.getCurrentAnimation());
    enemy.getEvents().trigger("lungeDashEnd");
    assertEquals(
        "chargeAttack",
        attacks.getCurrentAnimation(),
        "the strike can finish after the dash stops");
    enemy.getEvents().trigger("wanderStart");
    enemy.getEvents().trigger("touchAttack", target);
    assertEquals("normalAttack", attacks.getCurrentAnimation());
  }

  @Test
  void cancelledTelegraphDoesNotSuppressFutureNormalContact() {
    enemy.getEvents().trigger("lungeTelegraphStart");
    enemy.getEvents().trigger("wanderStart");
    enemy.getEvents().trigger("touchAttack", target);
    assertEquals("normalAttack", attacks.getCurrentAnimation());
  }

  @Test
  void deathStopsAttackAndRetainsExistingDisposalLifecycle() {
    enemy.getEvents().trigger("touchAttack", target);
    enemy.getEvents().trigger("dieAnimation");
    assertNull(attacks.getCurrentAnimation());
    assertEquals("dieAnimation", walking.getCurrentAnimation());
    enemy.getEvents().trigger("touchAttack", target);
    assertNull(attacks.getCurrentAnimation());
    when(time.getDeltaTime()).thenReturn(0.6f);
    walking.render(mock(SpriteBatch.class));
    controller.update();
    controller.update();
    verify(ServiceLocator.getEntityService(), times(1)).scheduleDisposal(enemy);
    assertEquals("dieAnimation", walking.getCurrentAnimation());
  }

  @Test
  void disposedControllerCannotRestartAttack() {
    enemy.getEvents().trigger("touchAttack", target);
    controller.dispose();
    enemy.getEvents().trigger("touchAttack", target);
    assertNull(attacks.getCurrentAnimation());
  }
}
