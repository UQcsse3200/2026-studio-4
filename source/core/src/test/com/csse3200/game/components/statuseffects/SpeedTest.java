package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SpeedTest {
  @Test
  void shouldIncreaseMovementSpeedThenRestoreWhenExpired() throws InterruptedException {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    Speed speed = new Speed(40, stats);
    assertEquals(4.5f, stats.getMovementSpeed(), 1e-4f);

    Thread.sleep(60);
    assertTrue(speed.update());
    assertEquals(4f, stats.getMovementSpeed(), 1e-4f);
  }

  @Test
  void factorySpeedShouldBuffMovement() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    StatusEffectsFactory.createSpeed(stats);
    assertEquals(4.5f, stats.getMovementSpeed(), 1e-4f);
  }
}
