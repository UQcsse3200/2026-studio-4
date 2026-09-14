package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FinalBossStageOneConfigTest {
  @Test
  void shouldAcceptDefaultConfiguration() {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();

    assertDoesNotThrow(config::validate);
  }

  @Test
  void shouldAllowIndependentWaveSummonCounts() {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();

    config.waveOneSummonCount = 8;
    config.waveTwoSummonCount = 6;
    assertDoesNotThrow(config::validate);

    config.waveTwoSummonCount = 8;
    assertDoesNotThrow(config::validate);

    config.waveTwoSummonCount = 10;
    assertDoesNotThrow(config::validate);
  }

  @Test
  void shouldRequireStageTwoHealthBelowBreakWindowFloor() {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    config.stageTwoStartingHealth = config.breakWindowHealthFloor;

    assertThrows(IllegalArgumentException.class, config::validate);
  }

  @Test
  void shouldRequireWaveTwoToMoveFaster() {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    config.waveTwoSummonSpeed = config.waveOneSummonSpeed;

    assertThrows(IllegalArgumentException.class, config::validate);
  }
}
