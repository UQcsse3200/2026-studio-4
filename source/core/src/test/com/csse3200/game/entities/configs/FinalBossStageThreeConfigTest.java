package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FinalBossStageThreeConfigTest {
  @Test
  void defaultTornadoDamageUsesOneSharedQuarterSecondTickAndNearbyRadius() {
    FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();

    assertEquals(0.5f, config.tornadoDamage);
    assertEquals(0.25f, config.tornadoDamageInterval);
    assertEquals(1.35f, config.tornadoDamageRadius);
    assertDoesNotThrow(config::validate);
  }

  @Test
  void tornadoDamageIntervalMustBeFiniteAndPositive() {
    for (float interval :
        new float[] {0f, -0.25f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
      config.tornadoDamageInterval = interval;

      assertThrows(
          IllegalArgumentException.class, config::validate, "Invalid interval: " + interval);
    }
  }

  @Test
  void tornadoDamageRadiusMustBeFiniteAndPositive() {
    for (float radius :
        new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
      config.tornadoDamageRadius = radius;

      assertThrows(IllegalArgumentException.class, config::validate, "Invalid radius: " + radius);
    }
  }

  @Test
  void negativeOrNonfiniteTornadoDamageIsRejected() {
    for (float damage :
        new float[] {-0.01f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
      config.tornadoDamage = damage;

      assertThrows(IllegalArgumentException.class, config::validate, "Invalid damage: " + damage);
    }
  }

  @Test
  void zeroTornadoDamageCanDisableContactDamage() {
    FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
    config.tornadoDamage = 0;

    assertDoesNotThrow(config::validate);
  }

  @Test
  void fractionalAndIntegerTornadoDamageAreBothSupported() {
    for (float damage : new float[] {0.25f, 0.5f, 1f, 2f}) {
      FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
      config.tornadoDamage = damage;

      assertDoesNotThrow(config::validate);
    }
  }
}
