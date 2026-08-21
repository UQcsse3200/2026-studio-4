package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disposes the entity once {@code lifetime} seconds have elapsed. Used by weapon hitboxes so they
 * do not linger in the world.
 */
public class LifetimeComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(LifetimeComponent.class);

  private float remaining;
  private boolean expired;

  /**
   * @param lifetime seconds until this entity is disposed
   * @require lifetime &gt;= 0
   * @throws IllegalArgumentException if lifetime is negative
   */
  public LifetimeComponent(float lifetime) {
    if (lifetime < 0f) {
      logger.error("Cannot create LifetimeComponent with negative lifetime: {}", lifetime);
      throw new IllegalArgumentException("lifetime must be >= 0");
    }
    this.remaining = lifetime;
  }

  /**
   * Tick remaining lifetime by {@code dt} seconds and dispose when it reaches 0.
   *
   * @param dt seconds since the last frame; negative values are treated as 0
   */
  public void update(float dt) {
    if (expired) {
      return;
    }
    float delta = Math.max(0f, dt);
    remaining -= delta;
    if (remaining <= 0f) {
      expired = true;
      entity.dispose();
    }
  }

  /**
   * Tick remaining lifetime using the registered time source.
   *
   * @require ServiceLocator.getTimeSource() != null
   */
  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }
}
