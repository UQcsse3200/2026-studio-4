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

  @Test
  void totalTimeDoesNotAdvanceBeforeRunStarts() {
    tick(1f);
    assertEquals(0f, timer.getTotalTime());
  }

  @Test
  void startRunBeginsAccumulatingTotalTime() {
    timer.startRun();
    tick(1.5f);
    tick(2f);
    assertEquals(3.5f, timer.getTotalTime(), 0.001f);
  }

  @Test
  void startRunIsANoOpWhileAlreadyRunning() {
    timer.startRun();
    tick(2f);
    timer.startRun(); // must not reset totalTime back to 0
    tick(1f);
    assertEquals(3f, timer.getTotalTime(), 0.001f);
  }

  @Test
  void dungeonTimeIsZeroWhenNoDungeonActive() {
    timer.startRun();
    tick(5f);
    assertNull(timer.getCurrentDungeonId());
    assertEquals(0f, timer.getDungeonTime());
    assertEquals(0f, timer.getDungeonSyncedTime());
  }

  @Test
  void startDungeonTracksTimeRelativeToRunStart() {
    timer.startRun();
    tick(10f); // totalTime = 10
    timer.startDungeon("dungeonOne");
    tick(3f); // totalTime = 13, dungeon has been running 3s
    assertEquals("dungeonOne", timer.getCurrentDungeonId());
    assertEquals(3f, timer.getDungeonTime(), 0.001f);
  }

  @Test
  void startDungeonIsANoOpWhileAlreadyRunning() {
    timer.startRun();
    tick(1f);
    timer.startDungeon("dungeonOne");
    tick(2f);
    timer.startDungeon("dungeonTwo"); // must be ignored; dungeon already active
    tick(1f);
    assertEquals("dungeonOne", timer.getCurrentDungeonId());
    assertEquals(3f, timer.getDungeonTime(), 0.001f);
  }

  @Test
  void stopDungeonClearsDungeonStateButRunKeepsGoing() {
    timer.startRun();
    timer.startDungeon("dungeonOne");
    tick(4f);
    timer.stopDungeon();
    assertNull(timer.getCurrentDungeonId());
    tick(2f);
    assertEquals(6f, timer.getTotalTime(), 0.001f);
  }

  @Test
  void stopRunFreezesBothTotalAndDungeonTime() {
    timer.startRun();
    timer.startDungeon("dungeonOne");
    tick(2f);
    timer.stopRun();
    tick(5f); // nothing should move after stopping
    assertEquals(2f, timer.getTotalTime(), 0.001f);
    assertEquals(2f, timer.getDungeonTime(), 0.001f);
  }

  @Test
  void dungeonSyncedTimeUsesWholeSecondBoundariesOfTheRunClock() {
    timer.startRun();
    tick(0.7f); // totalTime = 0.7
    timer.startDungeon("dungeonOne"); // dungeonStartTotal = 0.7 -> floors to 0
    tick(0.6f); // totalTime = 1.3 -> floors to 1; synced = 1 - 0 = 1
    assertEquals(1f, timer.getDungeonSyncedTime(), 0.001f);
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
}
