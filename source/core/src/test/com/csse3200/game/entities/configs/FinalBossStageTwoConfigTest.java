package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

class FinalBossStageTwoConfigTest {
  @Test
  void playerIceDamageFlightCadenceAndEffectTimingsMustBePositiveAndFinite() {
    List<BiConsumer<FinalBossStageTwoConfig, Float>> setters =
        List.of(
            (config, value) -> config.iceProjectileSpeed = value,
            (config, value) -> config.iceProjectileDamage = value,
            (config, value) -> config.iceFireInterval = value,
            (config, value) -> config.iceProjectileLifetime = value,
            (config, value) -> config.iceProjectileRadius = value,
            (config, value) -> config.iceHomingTurnRate = value,
            (config, value) -> config.iceImpactDuration = value,
            (config, value) -> config.iceBuffEndDuration = value);
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
  void playerIceCapacitiesArePositiveAndBounded() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
    for (int value : new int[] {1, 3, 128}) {
      config.iceShotsPerCharge = value;
      assertDoesNotThrow(config::validate);
    }
    for (int value : new int[] {-1, 0, 129}) {
      config.iceShotsPerCharge = value;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
    config.iceShotsPerCharge = 16;
    for (int value : new int[] {1, 256}) {
      config.maxIceProjectiles = value;
      assertDoesNotThrow(config::validate);
    }
    for (int value : new int[] {-1, 0, 257}) {
      config.maxIceProjectiles = value;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void fractionalPlayerIceDamageIsValid() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
    config.iceProjectileDamage = 0.25f;
    assertDoesNotThrow(config::validate);
  }

  @Test
  void icePickupCapacityIsBoundedAndCanBeDisabled() {
    for (int accepted : new int[] {0, 1, 8}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.icePickupCount = accepted;
      assertDoesNotThrow(config::validate);
    }
    for (int invalid : new int[] {-1, 9, Integer.MAX_VALUE}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.icePickupCount = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void icePickupRadiusAndTimingsMustBePositiveAndFinite() {
    List<BiConsumer<FinalBossStageTwoConfig, Float>> setters =
        List.of(
            (config, value) -> config.icePickupRadius = value,
            (config, value) -> config.icePickupSpawnInterval = value,
            (config, value) -> config.icePickupLifetime = value,
            (config, value) -> config.icePickupEffectDuration = value);
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
  void icePickupGapAllowsZeroButRejectsNegativeOrNonfiniteValues() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
    config.icePickupGap = 0f;
    assertDoesNotThrow(config::validate);
    for (float invalid :
        new float[] {-0.01f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      config.icePickupGap = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

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

  @Test
  void iceCoverSizeAndTimingsMustBePositiveAndFinite() {
    List<BiConsumer<FinalBossStageTwoConfig, Float>> setters =
        List.of(
            (config, value) -> config.iceCoverWidth = value,
            (config, value) -> config.iceCoverHeight = value,
            (config, value) -> config.iceCoverRespawnInterval = value,
            (config, value) -> config.iceCoverLifetime = value,
            (config, value) -> config.iceCoverNearMinDistance = value,
            (config, value) -> config.iceCoverNearMaxDistance = value,
            (config, value) -> config.iceShatterDuration = value,
            (config, value) -> config.iceSpawnDuration = value);
    for (BiConsumer<FinalBossStageTwoConfig, Float> setter : setters) {
      for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
        FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
        setter.accept(config, invalid);
        assertThrows(IllegalArgumentException.class, config::validate);
      }
    }
  }

  @Test
  void nearbyCoverDistanceRangeAllowsEqualBoundsButCannotBeReversed() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
    config.iceCoverNearMinDistance = 3f;
    config.iceCoverNearMaxDistance = 3f;
    assertDoesNotThrow(config::validate);
    config.iceCoverNearMaxDistance = 2.9f;
    assertThrows(IllegalArgumentException.class, config::validate);
  }

  @Test
  void iceCoverCountIsBoundedAndDurabilityIsPositive() {
    for (int accepted : new int[] {0, 3, 8}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.iceCoverCount = accepted;
      assertDoesNotThrow(config::validate);
    }
    for (int invalid : new int[] {-1, 9, Integer.MAX_VALUE}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.iceCoverCount = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
    for (int invalid : new int[] {0, -1}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.iceCoverHits = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
    for (int invalid : new int[] {-1, 0, 9, Integer.MAX_VALUE}) {
      FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
      config.iceCoverRespawnBatch = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }

  @Test
  void iceCoverGapAllowsZeroButRejectsNegativeOrNonfiniteValues() {
    FinalBossStageTwoConfig config = new FinalBossStageTwoConfig();
    config.iceCoverGap = 0f;
    assertDoesNotThrow(config::validate);
    for (float invalid : new float[] {-0.01f, Float.NaN, Float.POSITIVE_INFINITY}) {
      config.iceCoverGap = invalid;
      assertThrows(IllegalArgumentException.class, config::validate);
    }
  }
}
