package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores cooldown, damage, and knockback for a wielder's current weapon.
 *
 * <p>Cooldown is a countdown in seconds. Call {@link #triggerCooldown()} after a successful attack
 * and {@link #update(float)} (or the no-arg {@link #update()} from the entity loop) to tick it
 * down.
 */
public class WeaponStatsComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(WeaponStatsComponent.class);

  private float cooldown;
  private int damage;
  private float knockback;
  private float remainingCooldown;

  /**
   * Create weapon stats.
   *
   * @param cooldown seconds between attacks
   * @param damage damage applied by spawned hitboxes
   * @param knockback knockback impulse magnitude; 0 for none
   * @require cooldown &gt;= 0 &amp;&amp; damage &gt;= 0 &amp;&amp; knockback &gt;= 0
   * @throws IllegalArgumentException if any argument is negative
   */
  public WeaponStatsComponent(float cooldown, int damage, float knockback) {
    setCooldown(cooldown);
    setDamage(damage);
    setKnockback(knockback);
  }

  /**
   * @return true if no cooldown remains
   * @ensure result == (getRemainingCooldown() &lt;= 0)
   */
  public boolean canAttack() {
    return remainingCooldown <= 0f;
  }

  /**
   * Start the cooldown countdown from {@link #getCooldown()}.
   *
   * @ensure getRemainingCooldown() == getCooldown()
   */
  public void triggerCooldown() {
    remainingCooldown = cooldown;
  }

  /**
   * Tick remaining cooldown by {@code dt} seconds.
   *
   * @param dt seconds since the last frame; negative values are treated as 0
   * @ensure getRemainingCooldown() &gt;= 0
   */
  public void update(float dt) {
    if (remainingCooldown <= 0f) {
      return;
    }
    float delta = Math.max(0f, dt);
    remainingCooldown = Math.max(0f, remainingCooldown - delta);
  }

  /**
   * Tick remaining cooldown using the registered time source.
   *
   * @require ServiceLocator.getTimeSource() != null
   */
  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /**
   * @return cooldown duration in seconds
   */
  public float getCooldown() {
    return cooldown;
  }

  /**
   * @param cooldown seconds between attacks
   * @require cooldown &gt;= 0
   * @throws IllegalArgumentException if cooldown is negative
   */
  public void setCooldown(float cooldown) {
    if (cooldown < 0f) {
      logger.error("Cannot set weapon cooldown to a negative value: {}", cooldown);
      throw new IllegalArgumentException("cooldown must be >= 0");
    }
    this.cooldown = cooldown;
  }

  /**
   * @return damage dealt by this weapon's hitboxes
   */
  public int getDamage() {
    return damage;
  }

  /**
   * @param damage damage dealt by this weapon's hitboxes
   * @require damage &gt;= 0
   * @throws IllegalArgumentException if damage is negative
   */
  public void setDamage(int damage) {
    if (damage < 0) {
      logger.error("Cannot set weapon damage to a negative value: {}", damage);
      throw new IllegalArgumentException("damage must be >= 0");
    }
    this.damage = damage;
  }

  /**
   * @return knockback impulse magnitude
   */
  public float getKnockback() {
    return knockback;
  }

  /**
   * @param knockback knockback impulse magnitude
   * @require knockback &gt;= 0
   * @throws IllegalArgumentException if knockback is negative
   */
  public void setKnockback(float knockback) {
    if (knockback < 0f) {
      logger.error("Cannot set weapon knockback to a negative value: {}", knockback);
      throw new IllegalArgumentException("knockback must be >= 0");
    }
    this.knockback = knockback;
  }

  /**
   * @return seconds remaining before the next attack is allowed
   */
  public float getRemainingCooldown() {
    return remainingCooldown;
  }
}
