package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores cooldown, damage multiplier, and knockback for a wielder's current weapon.
 *
 * <p>Hitbox damage is {@code round(wielder.baseAttack * multiplier)}, so buffs to the wielder's
 * base attack (e.g. the Strength Charm) scale every weapon's damage. Likewise the effective
 * cooldown is {@code cooldown / wielder.attackSpeed}, so attack-speed buffs speed up every weapon.
 *
 * <p>Cooldown is a countdown in seconds. Call {@link #triggerCooldown()} after a successful attack
 * and {@link #update(float)} (or the no-arg {@link #update()} from the entity loop) to tick it
 * down.
 */
public class WeaponStatsComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(WeaponStatsComponent.class);

  private float cooldown;
  private float multiplier;
  private float knockback;
  private float remainingCooldown;

  /**
   * Create weapon stats.
   *
   * @param cooldown seconds between attacks
   * @param multiplier scales the wielder's base attack onto spawned hitboxes
   * @param knockback knockback impulse magnitude; 0 for none
   * @require cooldown &gt;= 0 &amp;&amp; multiplier &gt;= 0 &amp;&amp; knockback &gt;= 0
   * @throws IllegalArgumentException if any argument is negative
   */
  public WeaponStatsComponent(float cooldown, float multiplier, float knockback) {
    setCooldown(cooldown);
    setMultiplier(multiplier);
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
    triggerCooldown(cooldown);
  }

  /**
   * Start the cooldown countdown from a resolved duration, e.g. {@link #resolveCooldown(float)}.
   *
   * @param seconds cooldown duration; negative values are treated as 0
   */
  public void triggerCooldown(float seconds) {
    remainingCooldown = Math.max(0f, seconds);
  }

  /**
   * Cooldown scaled by the wielder's attack speed: faster wielders attack more often.
   *
   * @param attackSpeed wielder's {@code CombatStatsComponent.getAttackSpeed()}; values &lt;= 0 are
   *     treated as 1 (unscaled) so a zeroed stat cannot disable attacking forever
   * @return {@code cooldown / attackSpeed}
   */
  public float resolveCooldown(float attackSpeed) {
    return attackSpeed > 0f ? cooldown / attackSpeed : cooldown;
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
   * @return multiplier applied to the wielder's base attack
   */
  public float getMultiplier() {
    return multiplier;
  }

  /**
   * @param multiplier scales the wielder's base attack onto spawned hitboxes
   * @require multiplier &gt;= 0
   * @throws IllegalArgumentException if multiplier is negative
   */
  public void setMultiplier(float multiplier) {
    if (multiplier < 0f) {
      logger.error("Cannot set weapon multiplier to a negative value: {}", multiplier);
      throw new IllegalArgumentException("multiplier must be >= 0");
    }
    this.multiplier = multiplier;
  }

  /**
   * Hitbox damage copied onto a spawned sensor.
   *
   * @param baseAttack wielder's {@code CombatStatsComponent.getBaseAttack()}
   * @return {@code round(baseAttack * multiplier)}
   */
  public int resolveHitboxDamage(int baseAttack) {
    return Math.round(baseAttack * multiplier);
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
