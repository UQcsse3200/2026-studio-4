package com.csse3200.game.components.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.cutscene.DialogueScript.Line;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DialogueRunnerTest {
  private final List<String> log = new ArrayList<>();
  private DialogueRunner runner;

  @BeforeEach
  void setUp() {
    log.clear();
    DialogueScript script =
        new DialogueScript("test")
            .speaker("a", "Alice", null, null)
            .line("a", "Hello there")
            .line("a", "Bye");
    runner =
        new DialogueRunner(
            script,
            new DialogueRunner.Listener() {
              @Override
              public void onLineShown(int index, Line line) {
                log.add("line" + index);
              }

              @Override
              public void onFinished() {
                log.add("finished");
              }
            });
  }

  @Test
  void startShowsFirstLineWithNothingRevealed() {
    runner.start();
    assertEquals(List.of("line0"), log);
    assertEquals("", runner.getVisibleText());
    assertFalse(runner.isLineComplete());
    assertEquals("Alice", runner.getCurrentSpeaker().name);
  }

  @Test
  void textIsTypedOutOverTime() {
    runner.start();
    runner.update(2f / DialogueRunner.CHARS_PER_SECOND);
    assertEquals("He", runner.getVisibleText());
    runner.update(1f);
    assertEquals("Hello there", runner.getVisibleText());
    assertTrue(runner.isLineComplete());
  }

  @Test
  void advanceFirstCompletesTheLineThenMovesOn() {
    runner.start();
    runner.advance();
    assertEquals("Hello there", runner.getVisibleText());
    assertEquals(List.of("line0"), log);

    runner.advance();
    assertEquals(List.of("line0", "line1"), log);
    assertEquals(1, runner.getLineIndex());
  }

  @Test
  void advancingPastTheLastLineFinishesOnce() {
    runner.start();
    runner.advance();
    runner.advance();
    runner.advance();
    runner.advance();
    assertEquals(List.of("line0", "line1", "finished"), log);
    assertTrue(runner.isFinished());

    runner.advance();
    assertEquals(3, log.size());
  }

  @Test
  void skipAllFinishesImmediately() {
    runner.start();
    runner.skipAll();
    assertEquals(List.of("line0", "finished"), log);
  }

  @Test
  void emptyScriptFinishesOnStart() {
    List<String> events = new ArrayList<>();
    new DialogueRunner(
            new DialogueScript("empty"),
            new DialogueRunner.Listener() {
              @Override
              public void onLineShown(int index, Line line) {
                events.add("line");
              }

              @Override
              public void onFinished() {
                events.add("finished");
              }
            })
        .start();
    assertEquals(List.of("finished"), events);
  }

  @Test
  void unknownSpeakerAndMissingTextAreTolerated() {
    DialogueScript script = new DialogueScript("odd").line("ghost", null);
    DialogueRunner odd =
        new DialogueRunner(
            script,
            new DialogueRunner.Listener() {
              @Override
              public void onLineShown(int index, Line line) {}

              @Override
              public void onFinished() {}
            });
    odd.start();
    assertNull(odd.getCurrentSpeaker());
    assertEquals("", odd.getVisibleText());
    assertTrue(odd.isLineComplete());
  }
}
