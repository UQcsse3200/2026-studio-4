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
 * Plays enemy animations in response to existing AI and combat events. Skins with a contact-attack
 * clip or with both lunge clips opt into phase playback; other skins retain their existing
 * animation mapping.
 */
public class EnemyAnimationController extends Component {
  private static final String ATTACK = "attack";
  private static final String CONTACT_ATTACK = "contactAttack";
  private static final String DEFAULT = "default";
  private static final String HURT = "hurt";
  private AnimationRenderComponent animator;
  private boolean hasContactAnimation;
  private boolean hasLungeAnimations;
  private String locomotionAnimation = DEFAULT;
  private boolean dying = false;
  private boolean disposalQueued = false;
  private boolean followPlayer = false; // used by enemies to follow player
  private boolean lunging = false;

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("wanderStart", this::animateWander);
    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("enemyDeathAnimation", this::animateEnemyDeath);
    entity.getEvents().addListener("patrolStart", this::animatePatrol);
    entity.getEvents().addListener("dieAnimation", this::animateDie);
    entity.getEvents().addListener("rangedAttack", this::animateAttack);
    entity.getEvents().addListener("fuseStarted", this::animateFuse);
    entity.getEvents().addListener(DEFAULT, this::animatePause);
    entity.getEvents().addListener("moving", this::animateMove);
    entity.getEvents().addListener("stampede", this::dustCloud);

    hasContactAnimation = animator.hasAnimation(CONTACT_ATTACK);
    if (hasContactAnimation) {
      entity.getEvents().addListener(CONTACT_ATTACK, this::animateContactAttack);
    }

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
    if (hasContactAnimation || hasLungeAnimations) {
      if (!lunging && !isPlayingContactAttack() && dir.x != 0f) {
        animator.setFlipX(dir.x < 0f);
      }
      animateLocomotion("move", true);
      return;
    }
    followPlayer = false;
    String animation = directionAnimation(dir);
    if (animator.hasAnimation(animation)) {
      if (!animation.equals(animator.getCurrentAnimation())) {
        animator.startAnimation(animation);
      }
    } else if (animator.hasAnimation("move")
        && !(ATTACK.equals(animator.getCurrentAnimation()) && !animator.isFinished())
        && !"move".equals(animator.getCurrentAnimation())) {
      // Flying skins share one flight loop instead of eight directional clips.
      animator.startAnimation("move");
    }
  }

  private String directionAnimation(Vector2 dir) {
    if (dir.x == 0f) {
      return dir.y > 0f ? "move_up" : "move_down";
    }
    if (dir.y == 0f) {
      return dir.x > 0f ? "move_right" : "move_left";
    }
    if (dir.x > 0f) {
      return dir.y > 0f ? "move_NE" : "move_SE";
    }
    return dir.y > 0f ? "move_NW" : "move_SW";
  }

  private void animateDie() {
    if (dying) {
      return;
    }
    dying = true;
    animator.startAnimation("dieAnimation");
  }

  private void dustCloud() {
    if (dying) {
      return;
    }
    animator.startAnimation(DEFAULT);
  }

  private void animateEnemyDeath() {
    entity.getComponent(AITaskComponent.class).setEnabled(false);
    entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    animateDie();
  }

  @Override
  public void update() {
    if (entity == null) {
      return;
    }
    if (dying) {
      disposeAfterDeathAnimation();
      return;
    }
    if (hasContactAnimation) {
      if (StatusEffectsControllerComponent.isImmobilised(entity) || isPlayingContactAttack()) {
        return;
      }
      updateFacingDirection(followPlayer);
      updateLocomotion();
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
    if (ATTACK.equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("move");
    }
  }

  private void disposeAfterDeathAnimation() {
    if (!disposalQueued && animator.isFinished()) {
      disposalQueued = true;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
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
    animateLocomotion("chase", hasContactAnimation || hasLungeAnimations);
  }

  private void animatePause() {
    animateLocomotion(DEFAULT, followPlayer);
  }

  private void animateFuse() {
    if (!dying) {
      animator.startAnimation("fuse");
    }
  }

  private void animatePatrol() {
    animateLocomotion("move", hasContactAnimation);
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation(ATTACK);
    }
  }

  private void animateContactAttack(Entity target) {
    if (dying
        || StatusEffectsControllerComponent.isImmobilised(entity)
        || isPlayingContactAttack()) {
      return;
    }
    float direction = target.getCenterPosition().x - entity.getCenterPosition().x;
    if (direction != 0f) {
      animator.setFlipX(direction < 0f);
    }
    animator.startAnimation(CONTACT_ATTACK);
  }

  private boolean isPlayingContactAttack() {
    return CONTACT_ATTACK.equals(animator.getCurrentAnimation()) && !animator.isFinished();
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
    if (!dying && !lunging && animator.hasAnimation(HURT)) {
      animator.startAnimation(HURT);
    }
  }

  private boolean isPlayingHurt() {
    return HURT.equals(animator.getCurrentAnimation()) && !animator.isFinished();
  }

  private void animateLocomotion(String animation, boolean followMovement) {
    if (dying) {
      return;
    }
    followPlayer = followMovement;
    if (!hasContactAnimation && !hasLungeAnimations) {
      animator.startAnimation(animation);
      return;
    }
    locomotionAnimation = animation;
    // A cancelled telegraph can yield to wander/chase without emitting lungeDashEnd.
    lunging = false;
    if (!isPlayingContactAttack() && !isPlayingHurt()) {
      updateLocomotion();
    }
  }

  private void updateLocomotion() {
    PhysicsMovementComponent movement = entity.getComponent(PhysicsMovementComponent.class);
    String animation = movement.getMoving() ? locomotionAnimation : DEFAULT;
    if (!animation.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(animation);
    }
  }
}
