package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Cooldown, damage multiplier, and knockback. Hitbox damage is round(baseAttack * multiplier). */
public class WeaponStatsComponent extends Component {
  private float cooldown;
  private float multiplier;
  private float knockback;
  private float remainingCooldown;

  public WeaponStatsComponent(float cooldown, float multiplier, float knockback) {
    if (cooldown < 0f || multiplier < 0f || knockback < 0f) {
      throw new IllegalArgumentException("cooldown, multiplier, and knockback must be >= 0");
    }
    this.cooldown = cooldown;
    this.multiplier = multiplier;
    this.knockback = knockback;
  }

  public boolean canAttack() {
    return remainingCooldown <= 0f;
  }

  public void triggerCooldown() {
    remainingCooldown = cooldown;
  }

  public void update(float dt) {
    if (remainingCooldown > 0f) {
      remainingCooldown = Math.max(0f, remainingCooldown - dt);
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public float getCooldown() {
    return cooldown;
  }

  public float getMultiplier() {
    return multiplier;
  }

  public float getKnockback() {
    return knockback;
  }

  public float getRemainingCooldown() {
    return remainingCooldown;
  }

  public int resolveHitboxDamage(int baseAttack) {
    return Math.round(baseAttack * multiplier);
  }
}
