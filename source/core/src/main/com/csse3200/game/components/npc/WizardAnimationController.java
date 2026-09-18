package com.csse3200.game.components.npc;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Control some wizard animation. It needs own controller because pull animation is different. */
public class WizardAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean dying;

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("wanderStart", () -> playIfAlive("move"));
    entity.getEvents().addListener("rangedAttack", () -> playIfAlive("attack"));
    entity.getEvents().addListener("wizardPullStart", () -> playIfAlive("pull"));
    entity.getEvents().addListener("wizardPullStop", () -> playIfAlive("move"));
    entity.getEvents().addListener("enemyDeathAnimation", this::playDeath);
  }

  @Override
  public void update() {
    if (dying && animator.isFinished()) {
      dying = false;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    } else if ("attack".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("move");
    }
  }

  private void playIfAlive(String animation) {
    if (!dying) {
      animator.startAnimation(animation);
    }
  }

  private void playDeath() {
    dying = true;
    entity.getComponent(AITaskComponent.class).setEnabled(false);
    entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    animator.startAnimation("dieAnimation");
  }
}
