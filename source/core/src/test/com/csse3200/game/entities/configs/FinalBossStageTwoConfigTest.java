package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FinalBossStageTwoConfigTest {
  @Test
  void defaultsGiveASlowBossAndThreeSecondPause() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();

    assertDoesNotThrow(config::validate);
    assertEquals(1.2f, config.bossMoveSpeed);
    assertEquals(3f, config.pauseDuration);
  }

  @Test
  void movementSpeedMustBePositiveAndFinite() {
    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.bossMoveSpeed = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void roamRetargetIntervalMustBePositiveAndFinite() {
    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.bossRoamRetargetInterval = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void arenaMarginCanBeZeroButMustBeNonnegativeAndFinite() {
    FinalBossStageTwoConfig noMargin = new FinalBossStageTwoConfig();
    noMargin.arenaMargin = 0f;
    assertDoesNotThrow(noMargin::validate);

    for (float invalid : new float[] {-0.01f, Float.NaN, Float.POSITIVE_INFINITY}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.arenaMargin = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }
}
