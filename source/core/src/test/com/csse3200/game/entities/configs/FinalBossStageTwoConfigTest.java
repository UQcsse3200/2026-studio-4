package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.BiConsumer;
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

  @Test
  void fireballFlightAndCadenceValuesMustBePositiveAndFinite() {
    List<BiConsumer<FinalBossStageTwoConfig, Float>> setters =
        List.of(
            (config, value) -> config.fireballSpeed = value,
            (config, value) -> config.fireVolleyInterval = value,
            (config, value) -> config.fireballLifetime = value,
            (config, value) -> config.fireballRadius = value,
            (config, value) -> config.fireImpactDuration = value);
    for (BiConsumer<FinalBossStageTwoConfig, Float> setter : setters) {
      for (float invalid :
          new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
        FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
        setter.accept(config, invalid);
        assertThrows(IllegalArgumentException.class, config::validate);
      }
    }
  }

  @Test
  void initialShotDelayAllowsZeroButRejectsNegativeOrNonfiniteValues() {
    FinalBossStageTwoConfig immediate = new FinalBossStageTwoConfig();
    immediate.fireballInitialDelay = 0f;
    assertDoesNotThrow(immediate::validate);
    for (float invalid :
        new float[] {-0.01f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.fireballInitialDelay = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void projectileCapacityFitsALargestVolleyAndHasABoundedMaximum() {
    for (int accepted : new int[] {8, 256}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.maxFireballs = accepted;
      assertDoesNotThrow(config::validate);
    }
    for (int invalid : new int[] {-1, 0, 7, 257, Integer.MAX_VALUE}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.maxFireballs = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void fireballDamageMustBePositive() {
    for (int invalid : new int[] {0, -1}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.fireballDamage = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void firingCycleRejectsNonfiniteDurationsThatCouldPreventFutureShots() {
    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      FinalBossStageTwoConfig attack = new FinalBossStageTwoConfig();
      attack.attackDuration = invalid;
      assertThrows(IllegalArgumentException.class, attack::validate);
      FinalBossStageTwoConfig pause = new FinalBossStageTwoConfig();
      pause.pauseDuration = invalid;
      assertThrows(IllegalArgumentException.class, pause::validate);
    }
  }
}
