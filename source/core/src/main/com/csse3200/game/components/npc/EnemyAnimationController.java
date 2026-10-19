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
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class EnemyAnimationController extends Component {
  private static final String CONTACT_ATTACK = "contactAttack";
  private AnimationRenderComponent animator;
  private boolean hasContactAnimation;
  private String locomotionAnimation = "default";
  private boolean dying = false;
  private boolean disposalQueued = false;
  private boolean followPlayer = false; // used by enemies to follow player

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
    hasContactAnimation = animator.hasAnimation(CONTACT_ATTACK);
    if (hasContactAnimation) {
      entity.getEvents().addListener(CONTACT_ATTACK, this::animateContactAttack);
    }
  }

  private void animateMove(Vector2 dir) {
    if (dying) {
      return;
    }
    if (hasContactAnimation) {
      animateLocomotion("move", true);
      if (!isPlayingContactAttack() && dir.x != 0f) {
        animator.setFlipX(dir.x < 0f);
      }
      return;
    }
    followPlayer = false;
    String animation = directionAnimation(dir);
    if (animator.hasAnimation(animation)) {
      if (!animation.equals(animator.getCurrentAnimation())) {
        animator.startAnimation(animation);
      }
    } else if (animator.hasAnimation("move")
        && !("attack".equals(animator.getCurrentAnimation()) && !animator.isFinished())
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
    if (hasContactAnimation) {
      if (StatusEffectsControllerComponent.isImmobilised(entity) || isPlayingContactAttack()) {
        return;
      }
      updateFacingDirection(followPlayer);
      updateLocomotion();
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
    animateLocomotion("chase", hasContactAnimation);
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
    animateLocomotion("move", hasContactAnimation);
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
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

  private void animateLocomotion(String animation, boolean followMovement) {
    if (dying) {
      return;
    }
    followPlayer = followMovement;
    if (!hasContactAnimation) {
      animator.startAnimation(animation);
      return;
    }
    locomotionAnimation = animation;
    if (!isPlayingContactAttack()) {
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
}
