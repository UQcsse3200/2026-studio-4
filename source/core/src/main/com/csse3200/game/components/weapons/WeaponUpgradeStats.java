package com.csse3200.game.components.weapons;

/**
 * Multipliers a weapon gains once upgraded. Held by {@link WeaponUpgradeComponent}, one entry per
 * weapon class.
 *
 * @param lightDamageMultiplier scales the light (J) attack's hitbox damage
 * @param heavyDamageMultiplier scales the heavy (K) attack's hitbox damage
 * @param heavyCooldownMultiplier scales the shared cooldown triggered by a heavy attack
 */
public record WeaponUpgradeStats(
    float lightDamageMultiplier, float heavyDamageMultiplier, float heavyCooldownMultiplier) {

  /**
   * @throws IllegalArgumentException if any multiplier is negative
   */
  public WeaponUpgradeStats {
    if (lightDamageMultiplier < 0f || heavyDamageMultiplier < 0f || heavyCooldownMultiplier < 0f) {
      throw new IllegalArgumentException("upgrade multipliers must be >= 0");
    }
  }
}
