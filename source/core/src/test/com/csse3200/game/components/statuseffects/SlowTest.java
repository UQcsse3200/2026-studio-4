package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SlowTest {
  @Test
  void shouldReduceMovementSpeedThenRestoreWhenExpired() throws InterruptedException {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    Slow slow = new Slow(40, stats, -0.5f);
    assertEquals(3.5f, stats.getMovementSpeed(), 1e-4f);

    Thread.sleep(60);
    assertTrue(slow.update());
    assertEquals(4f, stats.getMovementSpeed(), 1e-4f);
  }

  @Test
  void factorySlowShouldReduceSpeed() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    StatusEffectsFactory.createSlow(stats);
    assertEquals(3.5f, stats.getMovementSpeed(), 1e-4f);
  }

  @Test
  void factoryFreezeShouldZeroMovementSpeed() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    StatusEffect freeze = StatusEffectsFactory.createFreeze(stats);
    assertEquals(0f, stats.getMovementSpeed(), 1e-4f);
    assertTrue(freeze.getRemainingDuration() > 0);
  }
}
