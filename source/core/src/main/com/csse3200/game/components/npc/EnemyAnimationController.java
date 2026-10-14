package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Plays enemy animations in response to existing AI and combat events. Skins with both lunge clips
 * opt into phase playback; other skins retain their existing animation mapping.
 */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean dying = false;
  private boolean disposalQueued = false;
  private boolean followPlayer = false; // used by enemies to follow player
  private boolean hasLungeAnimations;
  private boolean lunging;
  private String locomotionAnimation = "default";

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("wanderStart", this::animateWander);
    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("dieAnimation", this::animateDie);
    entity.getEvents().addListener("enemyDeathAnimation", this::animateEnemyDeath);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
    entity.getEvents().addListener("rangedAttack", this::animateAttack);
    entity.getEvents().addListener("fuseStarted", this::animateFuse);
    entity.getEvents().addListener("default", this::animatePause);
    entity.getEvents().addListener("moving", this::animateMove);

    hasLungeAnimations =
        animator.hasAnimation("lungeTelegraph") && animator.hasAnimation("lungeDash");
    if (hasLungeAnimations) {
      entity.getEvents().addListener("lungeTelegraphStart", () -> animateLunge("lungeTelegraph"));
      entity.getEvents().addListener("lungeDashStart", () -> animateLunge("lungeDash"));
      entity.getEvents().addListener("lungeDashEnd", this::finishLunge);
      entity.getEvents().addListener("hitReaction", this::animateHurt);
    }
  }

  private void animateMove(Vector2 dir) {
    if (dying) {
      return;
    }
    followPlayer = false;
    float x = dir.x;
    float y = dir.y;
    if (x > 0) {
      if (y > 0) {
        animator.startAnimation("move_NE");
      } else if (y < 0) {
        animator.startAnimation("move_SE");
      } else {
        animator.startAnimation("move_right");
      }
    } else if (x < 0) {
      if (y > 0) {
        animator.startAnimation("move_NW");
      } else if (y < 0) {
        animator.startAnimation("move_SW");
      } else {
        animator.startAnimation("move_left");
      }
    } else {
      if (y > 0) {
        animator.startAnimation("move_up");
      } else {
        animator.startAnimation("move_down");
      }
    }
  }

  private void animateDie() {
    if (dying) {
      return;
    }
    dying = true;
    animator.startAnimation("dieAnimation");
  }

  private void animateEnemyDeath() {
    entity.getComponent(AITaskComponent.class).setEnabled(false);
    entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    animateDie();
  }

  @Override
  public void update() {
    if (dying) {
      if (!disposalQueued && animator.isFinished()) {
        disposalQueued = true;
        ServiceLocator.getEntityService().scheduleDisposal(entity);
      }
      return;
    }
    if (hasLungeAnimations) {
      // AI and the renderer already pause for these effects. Keep the same displayed clip too.
      if (StatusEffectsControllerComponent.isImmobilised(entity)) {
        return;
      }
      updateFacingDirection(followPlayer);
      if (!lunging && !isPlayingHurt()) {
        updateLocomotion();
      }
      return;
    }
    updateFacingDirection(followPlayer);
    if ("attack".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("move");
    }
  }

  /** flip animation to face player - used by enemies */
  private void updateFacingDirection(boolean followPlayer) {
    if (!followPlayer) {
      return;
    }
    PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
    if (physics == null) {
      return;
    }

    float velocityX = physics.getBody().getLinearVelocity().x;
    if (Math.abs(velocityX) > 0.01f) {
      animator.setFlipX(velocityX < 0f);
    }
  }

  private void animateWander() {
    animateLocomotion("move", true);
  }

  private void animateChase() {
    animateLocomotion("chase", hasLungeAnimations);
  }

  private void animatePause() {
    animateLocomotion("default", followPlayer);
  }

  private void animateFuse() {
    if (!dying) {
      animator.startAnimation("fuse");
    }
  }

  private void animatePatrol() {
    animateLocomotion("move", false);
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
    }
  }

  private void animateLocomotion(String animation, boolean followMovement) {
    if (dying) {
      return;
    }
    followPlayer = followMovement;
    if (!hasLungeAnimations) {
      animator.startAnimation(animation);
      return;
    }
    locomotionAnimation = animation;
    // A cancelled telegraph can yield to wander/chase without emitting lungeDashEnd.
    lunging = false;
    if (!isPlayingHurt()) {
      updateLocomotion();
    }
  }

  private void updateLocomotion() {
    PhysicsMovementComponent movement = entity.getComponent(PhysicsMovementComponent.class);
    String animation = movement.getMoving() ? locomotionAnimation : "default";
    if (!animation.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(animation);
    }
  }

  private void animateLunge(String animation) {
    if (!dying) {
      lunging = true;
      followPlayer = true;
      animator.startAnimation(animation);
    }
  }

  private void finishLunge() {
    if (!dying) {
      lunging = false;
      updateLocomotion();
    }
  }

  private void animateHurt(Entity attacker) {
    if (!dying
        && !lunging
        && !StatusEffectsControllerComponent.isImmobilised(entity)
        && animator.hasAnimation("hurt")) {
      animator.startAnimation("hurt");
    }
  }

  private boolean isPlayingHurt() {
    return "hurt".equals(animator.getCurrentAnimation()) && !animator.isFinished();
  }
}
