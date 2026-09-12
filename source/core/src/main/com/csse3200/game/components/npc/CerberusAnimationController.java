package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

public class CerberusAnimationController extends Component {
  private static final String ANIM_MOVE = "move";
  private AnimationRenderComponent animator;
  private boolean dying = false;

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("default", this::animatePause);

    entity.getEvents().addListener("attackStart", this::animateAttack);
    entity.getEvents().addListener("enragePhaseStarted", this::animateEnrage);
    entity.getEvents().addListener("dieAnimation", this::animateDeath);
    animator.startAnimation(ANIM_MOVE);
  }

  @Override
  public void update() {
    if (dying && animator.isFinished()) {
      dying = false;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
    if (!dying && animator.isFinished()) {
      String currentAnim = animator.getCurrentAnimation();
      if ("attack".equals(currentAnim) || "roar".equals(currentAnim)) {
        animator.startAnimation(ANIM_MOVE);
      }
    }
  }

  private void animateChase() {
    if (!dying) {
      animator.startAnimation(ANIM_MOVE);
    }
  }

  private void animatePause() {
    if (!dying) {
      animator.startAnimation(ANIM_MOVE);
    }
  }

  private void animateAttack() {
    if (!dying) {
      animator.startAnimation("attack");
    }
  }

  private void animateEnrage() {
    if (!dying) {
      animator.startAnimation("roar");
    }
  }

  private void animateDeath() {
    dying = true;
    animator.startAnimation("dieAnimation");
  }
}
