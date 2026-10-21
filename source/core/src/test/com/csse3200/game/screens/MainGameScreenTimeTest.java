package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainGameScreenTimeTest {
  @Test
  void playContinuesAtNormalSpeed() {
    assertEquals(1f, MainGameScreen.timeScaleWhileHeld(false, false));
  }

  @Test
  void inventoryPauseStopsScaledTime() {
    assertEquals(0f, MainGameScreen.timeScaleWhileHeld(false, true));
  }

  @Test
  void aFreezeStopsScaledTime() {
    assertEquals(0f, MainGameScreen.timeScaleWhileHeld(true, false));
  }

  @Test
  void releasingTheInventoryWhileFrozenKeepsTimeStopped() {
    assertEquals(0f, MainGameScreen.timeScaleWhileHeld(true, true));
    assertEquals(0f, MainGameScreen.timeScaleWhileHeld(true, false));
  }
}
