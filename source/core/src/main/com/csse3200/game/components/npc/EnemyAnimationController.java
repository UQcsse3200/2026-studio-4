package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class EnemyAnimationController extends Component {
  AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("wanderStart", this::animateWander);
    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("dieAnimation", this::animateDie);
    entity.getEvents().addListener("default", this::animatePause);
  }

  void animateWander() {
    animator.startAnimation("move");
  }

  void animateChase() {
    animator.startAnimation("chase");
  }

  void animateDie() {
    animator.startAnimation("dieAnimation");
  }

  void animatePause() {
    animator.startAnimation("default");
  }
}
