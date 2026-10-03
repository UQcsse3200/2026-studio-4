package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RunTimerTest {
  @Test
  void shouldAccumulateTimeSeparatelyForEachDungeon() {
    RunTimer timer = new RunTimer(fixedGameTime());
    timer.startRun();
    timer.registerDungeon("dungeonOne");
    timer.registerDungeon("dungeonTwo");

    timer.startDungeon("dungeonOne");
    timer.update();
    timer.update();
    timer.stopDungeon();

    timer.startDungeon("dungeonTwo");
    timer.update();
    timer.update();
    timer.update();
    timer.stopDungeon();

    timer.startDungeon("dungeonOne");
    timer.update();

    assertEquals(3f, timer.getDungeonTimes().get("dungeonOne"));
    assertEquals(3f, timer.getDungeonTimes().get("dungeonTwo"));
  }

  @Test
  void shouldContinueDungeonTimesAfterRestore() {
    RunTimer timer = new RunTimer(fixedGameTime());
    timer.restoreRun(125f, Map.of("dungeonOne", 30f, "dungeonTwo", 45f));
    timer.startDungeon("dungeonOne");
    timer.update();

    assertEquals(126f, timer.getTotalTime());
    assertEquals(31f, timer.getDungeonTimes().get("dungeonOne"));
    assertEquals(45f, timer.getDungeonTimes().get("dungeonTwo"));
  }

  private GameTime fixedGameTime() {
    return new GameTime() {
      @Override
      public float getDeltaTime() {
        return 1f;
      }
    };
  }
}
