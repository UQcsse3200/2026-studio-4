package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/** Starts the floating demon's flying animation when it begins patrolling. */
public class FloatingDemonAnimationController extends Component {
  private AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
  }

  private void animatePatrol() {
    animator.startAnimation("float");
  }
}
