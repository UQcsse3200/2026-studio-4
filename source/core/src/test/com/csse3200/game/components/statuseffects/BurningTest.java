package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BurningTest {
  @Test
  void shouldDealDamageOnCooldown() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    Burning burn = new Burning(7, 0, 10_000, stats);

    assertTrue(burn.getRemainingDuration() > 0);
    waitUntil(
        () -> {
          burn.update();
          return stats.getHealth() < 100;
        });
    assertTrue(stats.getHealth() <= 93);
  }

  private static void waitUntil(java.util.function.BooleanSupplier done) {
    long deadline = System.currentTimeMillis() + 250;
    while (System.currentTimeMillis() < deadline && !done.getAsBoolean()) {
      // polling
    }
    assertTrue(done.getAsBoolean());
  }
}
