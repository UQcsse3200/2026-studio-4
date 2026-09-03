package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Component added to Sword attack hitbox to make it make a sweeping motion
 *
 * <p>requires the duration, start and end angles, and radius of the sword attack
 */
public class SweepComponent extends Component {
  private final float duration;
  private final float startAngleDeg;
  private final float endAngleDeg;
  private final float radius;
  private float elapsed;

  public SweepComponent(float duration, float startAngleDeg, float endAngleDeg, float radius) {
    if (duration <= 0f) {
      throw new IllegalArgumentException("duration must be > 0");
    }
    if (radius < 0f) {
      throw new IllegalArgumentException("radius must be >= 0");
    }
    this.duration = duration;
    this.startAngleDeg = startAngleDeg;
    this.endAngleDeg = endAngleDeg;
    this.radius = radius;
  }

  @Override
  public void update() {
    FollowComponent follow = entity.getComponent(FollowComponent.class);
    if (follow == null) {
      return;
    }

    /* Updates the offset for the sword attack's FollowComponent based on how long
    the attack has been going to give it the sweeping motion
    */
    float dt = ServiceLocator.getTimeSource().getDeltaTime();
    elapsed += Math.max(0f, dt);
    float t = Math.min(1f, elapsed / duration);
    float angle = startAngleDeg + (endAngleDeg - startAngleDeg) * t;
    Vector2 offset = new Vector2(radius, 0f).setAngleDeg(angle);
    follow.setLocalOffset(offset);

    // Turn the blade with the arc so it points along the swing rather than staying axis-aligned.
    RotatingTextureRenderComponent render =
        entity.getComponent(RotatingTextureRenderComponent.class);
    if (render != null) {
      render.setRotation(angle);
    }
  }
}
