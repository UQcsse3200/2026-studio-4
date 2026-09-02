package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Player entity's state and plays the animation when one
 * of the events is triggered.
 */
public class PlayerAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean attacking;
  private String animation = "idle_down";

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
    entity.getEvents().addListener("weaponAttack", this::animateAttack);
  }

  @Override
  public void update() {
    if (attacking && animator.isFinished()) {
      attacking = false;
      animator.startAnimation(animation);
    }
  }

  private void animateIdleDown() {
    animation = "idle_down";
    if (!attacking) {
      animator.startAnimation("idle_down");
    }
  }

  private void animateIdleLeft() {
    animation = "idle_left";
    if (!attacking) {
      animator.startAnimation("idle_left");
    }
  }

  private void animateIdleRight() {
    animation = "idle_right";
    if (!attacking) {
      animator.startAnimation("idle_right");
    }
  }

  private void animateIdleUp() {
    animation = "idle_up";
    if (!attacking) {
      animator.startAnimation("idle_up");
    }
  }

  private void animateWalkDown() {
    animation = "walk_down";
    if (!attacking) {
      animator.startAnimation("walk_down");
    }
  }

  private void animateWalkLeft() {
    animation = "walk_left";
    if (!attacking) {
      animator.startAnimation("walk_left");
    }
  }

  private void animateWalkRight() {
    animation = "walk_right";
    if (!attacking) {
      animator.startAnimation("walk_right");
    }
  }

  private void animateWalkUp() {
    animation = "walk_up";
    if (!attacking) {
      animator.startAnimation("walk_up");
    }
  }

  private void animateAttack(Vector2 direction) {
    if (attacking) {
      return;
    }
    attacking = true;
    if (direction.y < 0) {
      animator.startAnimation("attack_down");
    } else if (direction.y > 0) {
      animator.startAnimation("attack_up");
    } else if (direction.x < 0) {
      animator.startAnimation("attack_left");
    } else if (direction.x > 0) {
      animator.startAnimation("attack_right");
    } else {
      animator.startAnimation("attack_down");
    }
  }
}
