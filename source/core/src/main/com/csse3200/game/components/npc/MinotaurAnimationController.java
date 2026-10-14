package com.csse3200.game.components.npc;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.MinotaurAttackRenderComponent;

/**
 * Selects minotaur visuals from existing combat events. It never starts attacks, moves the entity,
 * or controls damage/cooldowns. The shared controller retains the original death lifecycle.
 */
public class MinotaurAnimationController extends EnemyAnimationController {
  private enum Phase {
    LOCOMOTION,
    TELEGRAPH,
    DASH
  }

  private final Entity target;
  private AnimationRenderComponent locomotion;
  private MinotaurAttackRenderComponent attacks;
  private Phase phase = Phase.LOCOMOTION;
  private String resumeAnimation = "default";
  private boolean dying;
  private boolean disposed;

  public MinotaurAnimationController(Entity target) {
    this.target = target;
  }

  @Override
  public void create() {
    // Register shared listeners first within one component: Entity's component order is undefined.
    super.create();
    locomotion = entity.getComponent(AnimationRenderComponent.class);
    attacks = entity.getComponent(MinotaurAttackRenderComponent.class);
    entity.getEvents().addListener("touchAttack", this::animateContact);
    entity.getEvents().addListener("lungeTelegraphStart", () -> startApproach(Phase.TELEGRAPH));
    entity.getEvents().addListener("lungeDashStart", () -> startApproach(Phase.DASH));
    entity.getEvents().addListener("lungeDashEnd", this::endDash);
    entity.getEvents().addListener("wanderStart", () -> resumeLocomotion("move"));
    entity.getEvents().addListener("chaseStart", () -> resumeLocomotion("chase"));
    entity.getEvents().addListener("patrolStart", () -> resumeLocomotion("move"));
    entity.getEvents().addListener("default", () -> resumeLocomotion("default"));
    entity.getEvents().addListener("dieAnimation", this::stopForDeath);
    entity.getEvents().addListener("enemyDeathAnimation", this::stopForDeath);
  }

  private void startApproach(Phase nextPhase) {
    if (dying || disposed) {
      return;
    }
    phase = nextPhase;
    attacks.stopAnimation();
    resumeAnimation = "move";
    locomotion.startAnimation(resumeAnimation);
    locomotion.setFlipX(target.getCenterPosition().x < entity.getCenterPosition().x);
  }

  private void endDash() {
    phase = Phase.LOCOMOTION;
    // An actual contact strike can finish after movement stops. A missed dash has no strike.
  }

  private void animateContact(Entity victim) {
    if (dying
        || disposed
        || phase == Phase.TELEGRAPH
        || entity.getComponent(CombatStatsComponent.class).isDead()
        || StatusEffectsControllerComponent.isImmobilised(entity)) {
      return;
    }
    String animation =
        phase == Phase.DASH
            ? MinotaurAttackRenderComponent.CHARGE_ATTACK
            : MinotaurAttackRenderComponent.NORMAL_ATTACK;
    boolean facingLeft = victim.getCenterPosition().x < entity.getCenterPosition().x;
    attacks.startAnimation(animation, facingLeft);
    locomotion.setFlipX(facingLeft);
    locomotion.stopAnimation();
  }

  private void resumeLocomotion(String animation) {
    if (dying || disposed) {
      return;
    }
    // A new movement task also cancels a telegraph/dash interrupted without a dash-end event.
    phase = Phase.LOCOMOTION;
    resumeAnimation = animation;
    if (attacks.getCurrentAnimation() != null) {
      locomotion.stopAnimation();
    }
  }

  private void stopForDeath() {
    dying = true;
    attacks.stopAnimation();
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }
    super.update();
    if (!dying && attacks.isFinished()) {
      attacks.stopAnimation();
      locomotion.startAnimation(resumeAnimation);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    attacks.stopAnimation();
    super.dispose();
  }
}
