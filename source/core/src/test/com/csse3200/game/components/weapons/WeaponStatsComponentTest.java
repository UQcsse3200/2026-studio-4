package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponStatsComponentTest {
  @Test
  void shouldGateAttackUntilCooldownElapses() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 1f, 0f);
    assertTrue(stats.canAttack());
    stats.triggerCooldown();
    assertFalse(stats.canAttack());
    stats.update(0.2f);
    assertFalse(stats.canAttack());
    stats.update(0.2f);
    assertTrue(stats.canAttack());
  }

  @Test
  void shouldScaleHitboxDamageByBaseAttackAndMultiplier() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 0.8f, 0f);
    assertEquals(8, stats.resolveHitboxDamage(10));
  }
}
