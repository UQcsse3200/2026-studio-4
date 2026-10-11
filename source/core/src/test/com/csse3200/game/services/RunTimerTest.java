package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunTimerTest {
  private GameTime gameTime;
  private RunTimer timer;

  @BeforeEach
  void setUp() {
    gameTime = mock(GameTime.class);
    timer = new RunTimer(gameTime);
  }

  /** Advances the timer by stubbing the next frame's delta and calling update(). */
  private void tick(float delta) {
    when(gameTime.getDeltaTime()).thenReturn(delta);
    timer.update();
  }

  /**
   * Advances the timer by a total amount of time, split across several sub-frame ticks no larger
   * than RunTimer's clamp. Mirrors how a real game loop accumulates time across many frames, rather
   * than tripping the single-frame clamp meant for loading-stall spikes.
   */
  private void advance(float totalSeconds) {
    final float step = 0.2f; // comfortably under RunTimer's clamp
    float remaining = totalSeconds;
    while (remaining > step) {
      tick(step);
      remaining -= step;
    }
    if (remaining > 0f) {
      tick(remaining);
    }
  }

  @Test
  void totalTimeDoesNotAdvanceBeforeRunStarts() {
    tick(1f);
    assertEquals(0f, timer.getTotalTime());
  }

  @Test
  void startRunBeginsAccumulatingTotalTime() {
    timer.startRun();
    advance(1.5f);
    advance(2f);
    assertEquals(3.5f, timer.getTotalTime(), 0.01f);
  }

  @Test
  void startRunIsANoOpWhileAlreadyRunning() {
    timer.startRun();
    advance(2f);
    timer.startRun(); // must not reset totalTime back to 0
    advance(1f);
    assertEquals(3f, timer.getTotalTime(), 0.01f);
  }

  @Test
  void dungeonTimeIsZeroWhenNoDungeonActive() {
    timer.startRun();
    advance(5f);
    assertNull(timer.getCurrentDungeonId());
    assertEquals(0f, timer.getDungeonTime());
    assertEquals(0f, timer.getDungeonSyncedTime());
  }

  @Test
  void startDungeonTracksTimeRelativeToRunStart() {
    timer.startRun();
    advance(10f); // totalTime = 10
    timer.startDungeon("dungeonOne");
    advance(3f); // totalTime = 13, dungeon has been running 3s
    assertEquals("dungeonOne", timer.getCurrentDungeonId());
    assertEquals(3f, timer.getDungeonTime(), 0.01f);
  }

  @Test
  void startDungeonSwitchesAndSavesPreviousDungeonWhileAlreadyRunning() {
    timer.startRun();
    advance(1f);
    timer.startDungeon("dungeonOne");
    advance(2f);
    timer.startDungeon("dungeonTwo");
    assertEquals("dungeonTwo", timer.getCurrentDungeonId());
    assertEquals(2f, timer.getDungeonTimes().get("dungeonOne"), 0.01f);
  }

  @Test
  void stopDungeonClearsDungeonStateButRunKeepsGoing() {
    timer.startRun();
    timer.startDungeon("dungeonOne");
    advance(4f);
    timer.stopDungeon();
    assertNull(timer.getCurrentDungeonId());
    advance(2f);
    assertEquals(6f, timer.getTotalTime(), 0.01f);
  }

  @Test
  void stopRunFreezesBothTotalAndDungeonTime() {
    timer.startRun();
    timer.startDungeon("dungeonOne");
    advance(2f);
    timer.stopRun();
    advance(5f); // nothing should move after stopping
    assertEquals(2f, timer.getTotalTime(), 0.01f);
    assertEquals(2f, timer.getDungeonTime(), 0.01f);
  }

  @Test
  void dungeonSyncedTimeUsesWholeSecondBoundariesOfTheRunClock() {
    timer.startRun();
    advance(0.7f); // totalTime = 0.7
    timer.startDungeon("dungeonOne"); // dungeonStartTotal = 0.7 -> floors to 0
    advance(0.6f); // totalTime = 1.3 -> floors to 1; synced = 1 - 0 = 1
    assertEquals(1f, timer.getDungeonSyncedTime(), 0.01f);
  }

  @Test
  void clampsAbnormallyLargeDeltaSpikes() {
    // Simulates a loading stall: the game loop reports one huge delta on the first frame
    // after construction. The displayed time must not jump by the full stall duration.
    timer.startRun();
    tick(3f); // single oversized frame, as seen right after a blocking asset load
    assertEquals(0.25f, timer.getTotalTime(), 0.001f);
  }

  @Test
  void formatTimeIncludesHundredths() {
    assertEquals("01:05.50", timer.formatTime(65.5f));
    assertEquals("00:00.00", timer.formatTime(0f));
  }

  @Test
  void formatTimeWithNoMilliSecDropsFraction() {
    assertEquals("01:05", timer.formatTimeWithNoMilliSec(65.9f));
    assertEquals("00:09", timer.formatTimeWithNoMilliSec(9.999f));
  }

  @Test
  void pauseStopsTimeFromAdvancing() {
    timer.startRun();
    advance(5f);
    timer.requestPause();
    advance(10f); // should have no effect
    assertEquals(5f, timer.getTotalTime(), 0.01f);
  }

  @Test
  void resumeContinuesFromWhereItLeftOff() {
    timer.startRun();
    advance(5f);
    timer.requestPause();
    advance(10f);
    timer.requestResume();
    advance(3f);
    assertEquals(8f, timer.getTotalTime(), 0.01f);
  }
}
