package com.csse3200.game.components.miniboss.dragon;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Selects dragon animations without controlling combat or movement. */
public class DragonAnimationController extends Component {
  private static final String IDLE = "idle";
  private static final String CAST = "wave";
  private static final String MOVE_LEFT = "moveLeft";
  private static final String MOVE_RIGHT = "moveRight";
  private static final float CAST_DURATION = 0.6f;

  private AnimationRenderComponent animator;
  private DragonCloudDashComponent dash;
  private CombatStatsComponent stats;
  private String currentAnimation;
  private String movementAnimation = MOVE_RIGHT;
  private float castRemaining;
  private boolean stopped;

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);
    dash = entity.getComponent(DragonCloudDashComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);

    if (animator == null || dash == null || stats == null) {
      throw new IllegalStateException(
          "Dragon animation requires animator, cloud dash, and combat stats");
    }

    entity.getEvents().addListener(DragonThunderOrbComponent.ATTACK_STARTED, this::onCast);
    entity.getEvents().addListener(DragonStormZoneComponent.ATTACK_STARTED, this::onCast);
    entity.getEvents().addListener("entityDied", this::stop);

    if (stats.isDead()) {
      stop();
    } else {
      play(IDLE);
    }
  }

  private void onCast() {
    if (!stopped && !stats.isDead()) {
      castRemaining = CAST_DURATION;
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float delta) {
    if (stopped) {
      return;
    }

    if (stats.isDead()) {
      stop();
      return;
    }

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    castRemaining = Math.max(0f, castRemaining - delta);
    play(selectAnimation());
  }

  private String selectAnimation() {
    return switch (dash.getState()) {
      case WARNING -> CAST;
      case DASHING -> selectMovementAnimation();
      case RECOVERING, STOPPED -> IDLE;
      case READY -> castRemaining > 0f ? CAST : IDLE;
    };
  }

  private String selectMovementAnimation() {
    float directionX = dash.getLockedDirection().x;

    if (directionX < -0.001f) {
      movementAnimation = MOVE_LEFT;
    } else if (directionX > 0.001f) {
      movementAnimation = MOVE_RIGHT;
    }

    return movementAnimation;
  }

  private void play(String animation) {
    if (!animation.equals(currentAnimation)) {
      animator.startAnimation(animation);
      currentAnimation = animation;
    }
  }

  private void stop() {
    if (stopped) {
      return;
    }

    stopped = true;
    castRemaining = 0f;
    animator.stopAnimation();
  }

  @Override
  public void dispose() {
    stop();
    super.dispose();
  }
}
