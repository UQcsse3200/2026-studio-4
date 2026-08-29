package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class PlayerAnimationController extends Component {
  AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("idleDown", this::animateIdleDown);
    entity.getEvents().addListener("idleLeft", this::animateIdleLeft);
    entity.getEvents().addListener("idleRight", this::animateIdleRight);
    entity.getEvents().addListener("idleUp", this::animateIdleUp);
    entity.getEvents().addListener("walkDown", this::animateWalkDown);
    entity.getEvents().addListener("walkLeft", this::animateWalkLeft);
    entity.getEvents().addListener("walkRight", this::animateWalkRight);
    entity.getEvents().addListener("walkUp", this::animateWalkUp);
  }

  void animateIdleDown() {
    animator.startAnimation("idle_down");
  }

  void animateIdleLeft() {
    animator.startAnimation("idle_left");
  }

  void animateIdleRight() {
    animator.startAnimation("idle_right");
  }

  void animateIdleUp() {animator.startAnimation("idle_up");}

  void animateWalkDown() {animator.startAnimation("walk_down");}

  void animateWalkLeft() {animator.startAnimation("walk_left");}

  void animateWalkRight() {animator.startAnimation("walk_right");}

  void animateWalkUp() {animator.startAnimation("walk_up");}
}
