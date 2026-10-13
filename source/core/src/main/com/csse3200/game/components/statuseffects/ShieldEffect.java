package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.Damage;
import com.csse3200.game.services.GameTime;

/**
 * Owns the consumable duration while delegating mitigation to the existing shield implementation.
 * The delegate is not registered or ticked: its rechargeable ability lifecycle does not apply to a
 * consumable. Only this wrapper's deadline decides when protection ends.
 */
public class ShieldEffect extends TimedStatusEffect implements Damageable {
  private final Shield protection = StatusEffectsFactory.createShield();

  public ShieldEffect(GameTime time, long durationMs) {
    super(time, durationMs);
    protection.activateTimed();
  }

  @Override
  public boolean damage(Damage damage) {
    if (!isExpired()) {
      damage.setDamage(protection.modifyIncomingDamage(damage.getDamage()));
    }
    return false;
  }
}
