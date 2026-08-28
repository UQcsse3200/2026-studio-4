package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;

/**
 * Final {@link #attack} gates cooldown, then {@link #createAttack} spawns the hitbox. Same entity
 * needs {@link WeaponStatsComponent}. Damage is round(wielder.baseAttack * multiplier).
 */
public abstract class WeaponComponent extends Component {
  private WeaponStatsComponent stats;

  @Override
  public void create() {
    stats = entity.getComponent(WeaponStatsComponent.class);
    if (stats == null) {
      throw new IllegalStateException("WeaponComponent requires a WeaponStatsComponent");
    }
  }

  public final boolean attack(Vector2 origin, Vector2 direction) {
    if (!stats.canAttack()) {
      return false;
    }
    createAttack(origin, direction);
    stats.triggerCooldown();
    return true;
  }

  /** Knife/sword/bow only decide where the sensor appears. */
  protected abstract void createAttack(Vector2 origin, Vector2 direction);

  /** round(wielder.baseAttack * weapon.multiplier); 0 if the wielder has no combat stats. */
  protected int resolveHitboxDamage() {
    CombatStatsComponent combat = entity.getComponent(CombatStatsComponent.class);
    int baseAttack = combat == null ? 0 : combat.getBaseAttack();
    return stats.resolveHitboxDamage(baseAttack);
  }
}
