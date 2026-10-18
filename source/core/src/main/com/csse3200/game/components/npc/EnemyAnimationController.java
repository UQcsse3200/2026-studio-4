package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Plays available enemy animations in response to existing AI and combat events. */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;
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
  }

  private void animateMove(Vector2 dir) {
    if (dying) {
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
    if (dying) {
      return;
    }
    followPlayer = true;
    animator.startAnimation("move");
  }

  private void animateChase() {
    if (dying) {
      return;
    }
    followPlayer = false;
    animator.startAnimation("chase");
  }

  private void animatePause() {
    if (!dying) {
      animator.startAnimation("default");
    }
  }

  private void animateFuse() {
    if (!dying && animator.hasAnimation("fuse")) {
      animator.startAnimation("fuse");
    }
  }

  private void animatePatrol() {
    if (!dying) {
      followPlayer = false;
      animator.startAnimation("move");
    }
  }

  private void animateAttack() {
    if (!dying && animator.hasAnimation("attack")) {
      animator.startAnimation("attack");
    }
  }
}
