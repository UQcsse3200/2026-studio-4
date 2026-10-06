package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * This class listens to events relevant to a ghost entity's state and plays the animation when one
 * of the events is triggered.
 */
public class EnemyAnimationController extends Component {
  private AnimationRenderComponent animator;
  private boolean dying = false;

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
    float direction = entity.getPosition().angleDeg();
    float x = dir.x;
    float y = dir.y;

    if (x == 0 && y > 0) {
      animator.startAnimation("move_up");
    } else if (x > 0 && y > 0) {
      animator.startAnimation("move_NE");
    } else if (x > 0 && y == 0) {
      animator.startAnimation("move_right");
    } else if (x > 0 && y < 0) {
      animator.startAnimation("move_SE");
    } else if (x == 0 && y < 0) {
      animator.startAnimation("move_down");
    } else if (x < 0 && y < 0) {
      animator.startAnimation("move_SW");
    } else if (x < 0 && y == 0) {
      animator.startAnimation("move_left");
    } else if (x < 0 && y > 0) {
      animator.startAnimation("move_NW");
    }
  }

  private void animateDie() {
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
    updateFacingDirection();
    if (dying && animator.isFinished()) {
      dying = false;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
    if ("attack".equals(animator.getCurrentAnimation()) && animator.isFinished()) {
      animator.startAnimation("move");
    }
  }

  private void updateFacingDirection() {
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
    animator.startAnimation("move");
  }

  private void animateChase() {
    animator.startAnimation("chase");
  }

  private void animatePause() {
    animator.startAnimation("default");
  }

  private void animateFuse() {
    animator.startAnimation("fuse");
  }

  private void animatePatrol() {
    if (!dying) {
      animator.startAnimation("move");
    }
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
    }
  }
}
