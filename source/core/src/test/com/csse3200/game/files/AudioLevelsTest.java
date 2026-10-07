package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AudioLevelsTest {
  @Test
  void clampsVolumeIntoUnitRange() {
    assertEquals(0f, AudioLevels.clamp(-1f));
    assertEquals(0f, AudioLevels.clamp(Float.NaN));
    assertEquals(0.5f, AudioLevels.clamp(0.5f));
    assertEquals(1f, AudioLevels.clamp(2f));
  }

  @Test
  void silencesAudioWhenTheWindowIsInTheBackground() {
    assertEquals(0f, AudioLevels.audible(0.4f, true, false));
    assertEquals(0.4f, AudioLevels.audible(0.4f, true, true));
    assertEquals(0.4f, AudioLevels.audible(0.4f, false, false));
  }
}
