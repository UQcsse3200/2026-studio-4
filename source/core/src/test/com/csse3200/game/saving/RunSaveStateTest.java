package com.csse3200.game.saving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunSaveStateTest {
  @BeforeEach
  void resetStateBeforeTest() {
    RunSaveState.reset();
  }

  @AfterEach
  void resetStateAfterTest() {
    RunSaveState.reset();
  }

  @Test
  void storesAndReturnsCurrentRoomId() {
    RunSaveState.setCurrentRoomId("dungeonOneMiddle");

    assertEquals("dungeonOneMiddle", RunSaveState.getCurrentRoomId());
  }

  @Test
  void returnsTheMostRecentlySetRoomId() {
    RunSaveState.setCurrentRoomId("dungeonOneMiddle");
    RunSaveState.setCurrentRoomId("dungeonTwoEntrance");

    assertEquals("dungeonTwoEntrance", RunSaveState.getCurrentRoomId());
  }

  @Test
  void throwsWhenNoCurrentRoomHasBeenSet() {
    assertThrows(IllegalStateException.class, RunSaveState::getCurrentRoomId);
  }

  @Test
  void resetClearsTheCurrentRoomId() {
    RunSaveState.setCurrentRoomId("dungeonOneMiddle");

    RunSaveState.reset();

    assertThrows(IllegalStateException.class, RunSaveState::getCurrentRoomId);
  }
}
