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
  void shouldRequireMoreSummonsInWaveTwo() {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    config.waveTwoSummonCount = config.waveOneSummonCount;

    assertThrows(IllegalArgumentException.class, config::validate);
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
