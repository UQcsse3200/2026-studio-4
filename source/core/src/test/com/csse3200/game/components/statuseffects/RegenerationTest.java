package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RegenerationTest {
  @Test
  void shouldHealLivingHostOnCooldown() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setHealth(40);
    Regeneration regen = new Regeneration(5, 0, 80, stats);

    waitUntil(
        () -> {
          regen.update();
          return stats.getHealth() > 40;
        });
    assertTrue(stats.getHealth() >= 45);
  }

  @Test
  void shouldNotHealWhenDead() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setHealth(0);
    Regeneration regen = new Regeneration(5, 0, 80, stats);
    regen.update();
    assertTrue(stats.isDead());
    assertEquals(0, stats.getHealth());
  }

  private static void waitUntil(java.util.function.BooleanSupplier done) {
    long deadline = System.currentTimeMillis() + 250;
    while (System.currentTimeMillis() < deadline && !done.getAsBoolean()) {
      // polling
    }
    assertTrue(done.getAsBoolean());
  }
}
