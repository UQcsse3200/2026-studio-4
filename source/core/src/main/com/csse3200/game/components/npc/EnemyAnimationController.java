package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean dying = false;

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("wanderStart", this::animateWander);
    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("dieAnimation", this::animateDie);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
    entity.getEvents().addListener("rangedAttack", this::animateAttack);
    entity.getEvents().addListener("default", this::animatePause);
  }

  private void animateDie() {
    dying = true;
    animator.startAnimation("dieAnimation");
  }

  @Override
  public void update() {
    if (dying && animator.isFinished()) {
      dying = false;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
    if ("attack".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("move");
    }
  }

  private void animateWander() {
    animator.startAnimation("move");
  }

  private void animateChase() {
    animator.startAnimation("chase");
  }

  private void animatePause() {
    animator.startAnimation("default");
  }

  private void animatePatrol() {
    if (!dying) {
      animator.startAnimation("move");
    }
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
    }
  }
}
