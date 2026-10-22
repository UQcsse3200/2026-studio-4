package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FlashEffect;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class EnemyAnimationController extends Component {
  private static final String ATTACK_ANIMATION = "attack";
  private static final Color FUSE_FLASH_TINT = new Color(Color.WHITE);
  private static final Color FUSE_FLASH_GLOW = new Color(1f, 0f, 0f, 1f);
  private AnimationRenderComponent animator;
  private StatusEffectsControllerComponent statusEffects;
  private FlashEffect fuseFlash;
  private static final String DEFAULT = "default";
  private boolean dying = false;
  private boolean followPlayer = false; // used by enemies to follow player
  private float deathRotationPerFrame;
  private float maxDeathRotation;

  public void setDeathRotation(float degreesPerFrame, float maxDegrees) {
    if (!Float.isFinite(degreesPerFrame)
        || degreesPerFrame < 0f
        || !Float.isFinite(maxDegrees)
        || maxDegrees < 0f) {
      throw new IllegalArgumentException("Death rotation values must be finite and non-negative");
    }
    deathRotationPerFrame = degreesPerFrame;
    maxDeathRotation = maxDegrees;
  }

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);
    statusEffects = entity.getComponent(StatusEffectsControllerComponent.class);

    entity.getEvents().addListener("wanderStart", this::animateWander);
    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("enemyDeathAnimation", this::animateEnemyDeath);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
    entity.getEvents().addListener("dieAnimation", this::animateDie);
    entity.getEvents().addListener("rangedAttack", this::animateAttack);
    entity.getEvents().addListener("touchAttack", this::animateAttack);
    entity.getEvents().addListener("fuseStarted", this::animateFuse);
    entity.getEvents().addListener(DEFAULT, this::animatePause);
    entity.getEvents().addListener("moving", this::animateMove);
    entity.getEvents().addListener("stampede", this::dustCloud);
  }

  private void animateMove(Vector2 dir) {
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
    dying = true;
    animator.startAnimation("dieAnimation");
  }

  private void dustCloud() {
    animator.startAnimation(DEFAULT);
  }

  private void animateEnemyDeath() {
    entity.getComponent(AITaskComponent.class).setEnabled(false);
    entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    animateDie();
  }

  @Override
  public void update() {
    updateFacingDirection(followPlayer);

    if (dying && deathRotationPerFrame > 0f) {
      int frameIndex = animator.getCurrentFrameIndex();
      if (frameIndex >= 0) {
        animator.setRotation(Math.min(frameIndex * deathRotationPerFrame, maxDeathRotation));
      }
    }

    if (dying && animator.isFinished()) {
      dying = false;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
    if (ATTACK_ANIMATION.equals(animator.getCurrentAnimation()) && animator.isFinished()) {
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
    followPlayer = true;
    animator.startAnimation("move");
  }

  private void animateChase() {
    followPlayer = true;
    animator.startAnimation("chase");
  }

  private void animatePause() {
    animator.startAnimation(DEFAULT);
  }

  private void animateFuse(float fuseTime) {
    animator.startAnimation("fuse");
    if (statusEffects != null) {
      statusEffects.removeStatusEffect(fuseFlash);
      long durationMillis = Math.max(1L, Math.round(fuseTime * 1000d));
      fuseFlash =
          new FlashEffect(
              ServiceLocator.getTimeSource(), durationMillis, FUSE_FLASH_TINT, FUSE_FLASH_GLOW);
      statusEffects.addStatusEffect(fuseFlash);
    }
  }

  private void animatePatrol() {
    followPlayer = false;
    if (!dying) {
      animator.startAnimation("move");
    }
  }

  private void animateAttack() {
    if (!dying && animator.hasAnimation(ATTACK_ANIMATION)) {
      animator.startAnimation(ATTACK_ANIMATION);
    }
  }
}
