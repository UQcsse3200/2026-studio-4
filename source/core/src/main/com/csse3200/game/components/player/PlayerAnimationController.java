package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Player entity's state and plays the animation when one
 * of the events is triggered.
 */
public class PlayerAnimationController extends Component {
  // Event names
  private static final String EVENT_IDLE_DOWN = "idleDown";
  private static final String EVENT_IDLE_LEFT = "idleLeft";
  private static final String EVENT_IDLE_RIGHT = "idleRight";
  private static final String EVENT_IDLE_UP = "idleUp";
  private static final String EVENT_WALK_DOWN = "walkDown";
  private static final String EVENT_WALK_LEFT = "walkLeft";
  private static final String EVENT_WALK_RIGHT = "walkRight";
  private static final String EVENT_WALK_UP = "walkUp";
  private static final String EVENT_WEAPON_ATTACK = "weaponAttack";

  // Animation names
  private static final String ANIM_IDLE_DOWN = "idle_down";
  private static final String ANIM_IDLE_LEFT = "idle_left";
  private static final String ANIM_IDLE_RIGHT = "idle_right";
  private static final String ANIM_IDLE_UP = "idle_up";
  private static final String ANIM_WALK_DOWN = "walk_down";
  private static final String ANIM_WALK_LEFT = "walk_left";
  private static final String ANIM_WALK_RIGHT = "walk_right";
  private static final String ANIM_WALK_UP = "walk_up";
  private static final String ANIM_ATTACK_DOWN = "attack_down";
  private static final String ANIM_ATTACK_UP = "attack_up";
  private static final String ANIM_ATTACK_LEFT = "attack_left";
  private static final String ANIM_ATTACK_RIGHT = "attack_right";

  private AnimationRenderComponent animator;
  private boolean attacking;
  private String animation = ANIM_IDLE_DOWN;

  @Override
  public void create() {
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener(EVENT_IDLE_DOWN, this::animateIdleDown);
    entity.getEvents().addListener(EVENT_IDLE_LEFT, this::animateIdleLeft);
    entity.getEvents().addListener(EVENT_IDLE_RIGHT, this::animateIdleRight);
    entity.getEvents().addListener(EVENT_IDLE_UP, this::animateIdleUp);
    entity.getEvents().addListener(EVENT_WALK_DOWN, this::animateWalkDown);
    entity.getEvents().addListener(EVENT_WALK_LEFT, this::animateWalkLeft);
    entity.getEvents().addListener(EVENT_WALK_RIGHT, this::animateWalkRight);
    entity.getEvents().addListener(EVENT_WALK_UP, this::animateWalkUp);
    entity.getEvents().addListener(EVENT_WEAPON_ATTACK, this::animateAttack);
  }

  @Override
  public void update() {
    if (attacking && animator.isFinished()) {
      attacking = false;
      animator.startAnimation(animation);
    }
  }

  private void animateIdleDown() {
    setIdleAnimation(ANIM_IDLE_DOWN);
  }

  private void animateIdleLeft() {
    setIdleAnimation(ANIM_IDLE_LEFT);
  }

  private void animateIdleRight() {
    setIdleAnimation(ANIM_IDLE_RIGHT);
  }

  private void animateIdleUp() {
    setIdleAnimation(ANIM_IDLE_UP);
  }

  private void animateWalkDown() {
    setIdleAnimation(ANIM_WALK_DOWN);
  }

  private void animateWalkLeft() {
    setIdleAnimation(ANIM_WALK_LEFT);
  }

  private void animateWalkRight() {
    setIdleAnimation(ANIM_WALK_RIGHT);
  }

  private void animateWalkUp() {
    setIdleAnimation(ANIM_WALK_UP);
  }

  /**
   * Sets the current non-attack animation, and plays it immediately unless an attack animation is
   * in progress.
   *
   * @param animationName animation to switch to
   */
  private void setIdleAnimation(String animationName) {
    animation = animationName;
    if (!attacking) {
      animator.startAnimation(animationName);
    }
  }

  private void animateAttack(Vector2 direction) {
    if (attacking) {
      return;
    }
    attacking = true;
    if (direction.y < 0) {
      animator.startAnimation(ANIM_ATTACK_DOWN);
    } else if (direction.y > 0) {
      animator.startAnimation(ANIM_ATTACK_UP);
    } else if (direction.x < 0) {
      animator.startAnimation(ANIM_ATTACK_LEFT);
    } else if (direction.x > 0) {
      animator.startAnimation(ANIM_ATTACK_RIGHT);
    } else {
      animator.startAnimation(ANIM_ATTACK_DOWN);
    }
  }
}
