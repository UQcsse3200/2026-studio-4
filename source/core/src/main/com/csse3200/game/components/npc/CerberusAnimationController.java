package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

public class CerberusAnimationController extends Component {
  private static final String ANIM_MOVE = "move";
  private AnimationRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);

    entity.getEvents().addListener("chaseStart", this::animateChase);
    entity.getEvents().addListener("default", this::animatePause);
    entity.getEvents().addListener("attackStart", this::animateAttack);
    entity.getEvents().addListener("enragePhaseStarted", this::animateEnrage);

    animator.startAnimation(ANIM_MOVE);
  }

  @Override
  public void update() {
    if (animator.isFinished()) {
      String currentAnim = animator.getCurrentAnimation();

      if ("lunge".equals(currentAnim) || "idle".equals(currentAnim)) {
        animator.startAnimation(ANIM_MOVE);
      }
    }
  }

  private void animateChase() {
    animator.startAnimation(ANIM_MOVE);
  }

  private void animatePause() {
    animator.startAnimation(ANIM_MOVE);
  }

  private void animateAttack() {
    animator.startAnimation("lunge");
  }

  private void animateEnrage() {
    animator.startAnimation("idle");
  }
}
