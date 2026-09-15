package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

public class CerberusAnimationController extends Component {
  private static final String ANIM_MOVE = "move";

  private AnimationRenderComponent animator;
  private CombatStatsComponent stats;
  private boolean dead;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);

    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("default", this::animatePause);
    entity.getEvents().addListener("attackStart", this::animateAttack);
    entity.getEvents().addListener("enragePhaseStarted", this::animateEnrage);
    entity.getEvents().addListener("entityDied", this::onDeath);

    if (isDead()) {
      onDeath();
    } else {
      animator.startAnimation(ANIM_MOVE);
    }
  }

  @Override
  public void update() {
    if (isDead()) {
      return;
    }

    if (animator.isFinished()) {
      String currentAnim = animator.getCurrentAnimation();

      if ("lunge".equals(currentAnim) || "idle".equals(currentAnim)) {
        animator.startAnimation(ANIM_MOVE);
      }
    }
  }

  private boolean isDead() {
    return dead || (stats != null && stats.isDead());
  }

  private void animateChase() {
    if (!isDead()) {
      animator.startAnimation(ANIM_MOVE);
    }
  }

  private void animatePause() {
    if (!isDead()) {
      animator.startAnimation(ANIM_MOVE);
    }
  }

  private void animateAttack() {
    if (!isDead()) {
      animator.startAnimation("lunge");
    }
  }

  private void animateEnrage() {
    if (!isDead()) {
      animator.startAnimation("idle");
    }
  }

  private void onDeath() {
    if (dead) {
      return;
    }

    dead = true;
    animator.startAnimation("idle");
  }
}
