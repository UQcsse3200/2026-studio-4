package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class DragonAnimationControllerTest {
  private Entity dragon;
  private Entity target;
  private CombatStatsComponent stats;
  private DragonCloudDashComponent dash;
  private AnimationRenderComponent animator;
  private DragonAnimationController controller;

  @BeforeEach
  void setUp() {
    target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    target.setPosition(5f, 0f);

    stats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    dash = new DragonCloudDashComponent(target);
    animator = mock(AnimationRenderComponent.class);
    controller = new DragonAnimationController();

    dragon =
        new Entity()
            .addComponent(stats)
            .addComponent(phase)
            .addComponent(dash)
            .addComponent(animator)
            .addComponent(controller);

    phase.create();
    dash.create();
    controller.create();
  }

  @Test
  void shouldStartIdleWithoutRestartingEveryFrame() {
    controller.update(0.1f);
    controller.update(0.1f);

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldCastForThunderOrbThenReturnToIdle() {
    dragon.getEvents().trigger(DragonThunderOrbComponent.ATTACK_STARTED);

    controller.update(0.1f);
    verify(animator).startAnimation("wave");

    controller.update(0.6f);
    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  void shouldCastForStormZone() {
    dragon.getEvents().trigger(DragonStormZoneComponent.ATTACK_STARTED);

    controller.update(0.1f);

    verify(animator).startAnimation("wave");
  }

  @Test
  void shouldNotRestartCastForSimultaneousSkills() {
    dragon.getEvents().trigger(DragonThunderOrbComponent.ATTACK_STARTED);
    dragon.getEvents().trigger(DragonStormZoneComponent.ATTACK_STARTED);

    controller.update(0.1f);
    controller.update(0.1f);

    verify(animator, times(1)).startAnimation("wave");
  }

  @Test
  void shouldWarnThenDashRightThenRecoverIdle() {
    assertTrue(dash.tryAttack());

    controller.update(0.1f);
    verify(animator).startAnimation("wave");

    dash.update(0.8f);
    controller.update(0.1f);
    verify(animator).startAnimation("moveRight");

    dash.finishDash();
    controller.update(0.1f);
    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  void shouldUseLeftAnimationForLeftDash() {
    target.setPosition(-5f, 0f);
    assertTrue(dash.tryAttack());
    dash.update(0.8f);

    controller.update(0.1f);

    verify(animator).startAnimation("moveLeft");
    verify(animator, never()).startAnimation("moveRight");
  }

  @Test
  void shouldStopOnceOnDeathAndDisposal() {
    stats.setHealth(0);
    controller.update(1f);
    controller.dispose();

    verify(animator, times(1)).stopAnimation();
    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldIgnoreInvalidDelta() {
    dragon.getEvents().trigger(DragonThunderOrbComponent.ATTACK_STARTED);

    controller.update(Float.NaN);
    controller.update(Float.POSITIVE_INFINITY);
    controller.update(-1f);

    verify(animator, never()).startAnimation("wave");

    controller.update(0.1f);
    verify(animator).startAnimation("wave");
  }
}
