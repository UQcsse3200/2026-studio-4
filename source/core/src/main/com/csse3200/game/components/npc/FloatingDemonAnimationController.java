package com.csse3200.game.components.npc;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/** Starts the floating demon's flying animation when it begins patrolling. */
public class FloatingDemonAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean dying;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
    entity.getEvents().addListener("rangedAttack", this::animateAttack);
    entity.getEvents().addListener("dieAnimation", this::animateDeath);
  }

  private void animatePatrol() {
    if (!dying) {
      animator.startAnimation("float");
    }
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
    }
  }

  private void animateDeath() {
    dying = true;
    entity.getComponent(AITaskComponent.class).setEnabled(false);
    entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    animator.startAnimation("dieAnimation");
  }

  @Override
  public void update() {
    if (dying && animator.isFinished()) {
      entity.dispose();
      return;
    }

    if ("attack".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("float");
    }
  }
}
