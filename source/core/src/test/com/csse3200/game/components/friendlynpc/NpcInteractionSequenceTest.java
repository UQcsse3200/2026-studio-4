package com.csse3200.game.components.friendlynpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfig.CutsceneTiming;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NpcInteractionSequenceTest {
  private List<String> log;
  private int finishedCount;
  private Recorder output;

  @BeforeEach
  void setUp() {
    log = new ArrayList<>();
    finishedCount = 0;
    output = new Recorder();
  }

  @Test
  void dialogueOnlyNpcLocksUntilDialogueFinishes() {
    NpcInteractionSequence sequence = sequence(config("talk", null, CutsceneTiming.AFTER_DIALOGUE));

    sequence.start();
    assertEquals(List.of("lock", "dialogue:talk"), log);
    assertTrue(sequence.isMovementLocked());
    assertEquals(0, finishedCount);

    assertTrue(sequence.onDialogueFinished("talk"));
    assertEquals(List.of("lock", "dialogue:talk", "unlock"), log);
    assertTrue(sequence.isFinished());
    assertEquals(1, finishedCount);
  }

  @Test
  void cutsceneOnlyNpcPlaysCutsceneWithoutDialogue() {
    NpcInteractionSequence sequence =
        sequence(config(null, "scene", CutsceneTiming.AFTER_DIALOGUE));

    sequence.start();
    assertEquals(List.of("lock", "cutscene:scene"), log);

    sequence.onCutsceneFinished("scene");
    assertEquals(List.of("lock", "cutscene:scene", "unlock"), log);
    assertEquals(1, finishedCount);
  }

  @Test
  void cutsceneAfterDialogueKeepsControlsLockedBetweenSteps() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE));

    sequence.start();
    sequence.onDialogueFinished("talk");
    // No unlock/relock flicker between the two steps.
    assertEquals(List.of("lock", "dialogue:talk", "cutscene:scene"), log);
    assertEquals(0, finishedCount);

    sequence.onCutsceneFinished("scene");
    assertEquals(List.of("lock", "dialogue:talk", "cutscene:scene", "unlock"), log);
    assertEquals(1, finishedCount);
  }

  @Test
  void cutsceneBeforeDialogue() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.BEFORE_DIALOGUE));

    sequence.start();
    assertEquals(List.of("lock", "cutscene:scene"), log);
    sequence.onCutsceneFinished("scene");
    assertEquals(List.of("lock", "cutscene:scene", "dialogue:talk"), log);
    sequence.onDialogueFinished("talk");
    assertEquals(1, finishedCount);
  }

  @Test
  void duringDialogueCueStartsCutsceneAndWaitsForBoth() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.DURING_DIALOGUE));

    sequence.start();
    assertTrue(sequence.onCutsceneCue("talk"));
    assertTrue(sequence.isDialogueActive());
    assertTrue(sequence.isCutsceneActive());
    assertEquals(List.of("lock", "dialogue:talk", "cutscene:scene"), log);

    sequence.onDialogueFinished("talk");
    assertFalse(sequence.isFinished(), "Still waiting on the cutscene");
    sequence.onCutsceneFinished("scene");
    assertTrue(sequence.isFinished());
    assertEquals(1, finishedCount);
  }

  @Test
  void duringDialogueWithoutCueStillPlaysCutsceneAfterwards() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.DURING_DIALOGUE));

    sequence.start();
    sequence.onDialogueFinished("talk");
    assertEquals(List.of("lock", "dialogue:talk", "cutscene:scene"), log);
    sequence.onCutsceneFinished("scene");
    assertEquals(1, finishedCount);
  }

  @Test
  void cueIsIgnoredForOtherTimingsAndRepeatedCues() {
    NpcInteractionSequence after = sequence(config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE));
    after.start();
    assertFalse(after.onCutsceneCue("talk"));
    assertFalse(after.isCutsceneActive());

    NpcInteractionSequence during =
        sequence(config("talk", "scene", CutsceneTiming.DURING_DIALOGUE));
    during.start();
    assertTrue(during.onCutsceneCue("talk"));
    assertFalse(during.onCutsceneCue("talk"));
  }

  @Test
  void onlyLocksForStepsThatAskForIt() {
    InteractableNpcConfig config = config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE);
    config.lockMovementDuringDialogue = false;
    NpcInteractionSequence sequence = sequence(config);

    sequence.start();
    assertFalse(sequence.isMovementLocked());
    sequence.onDialogueFinished("talk");
    assertTrue(sequence.isMovementLocked());
    sequence.onCutsceneFinished("scene");
    assertEquals(List.of("dialogue:talk", "lock", "cutscene:scene", "unlock"), log);
  }

  @Test
  void neverLocksWhenNothingAsksForIt() {
    InteractableNpcConfig config = config("talk", null, CutsceneTiming.AFTER_DIALOGUE);
    config.lockMovementDuringDialogue = false;
    NpcInteractionSequence sequence = sequence(config);

    sequence.start();
    sequence.onDialogueFinished("talk");
    assertEquals(List.of("dialogue:talk"), log);
    assertEquals(1, finishedCount);
  }

  @Test
  void ignoresFinishEventsForOtherContent() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE));
    sequence.start();

    assertFalse(sequence.onDialogueFinished("someoneElse"));
    assertFalse(sequence.onCutsceneFinished("scene"), "Cutscene has not started yet");
    assertTrue(sequence.isDialogueActive());
    assertEquals(0, finishedCount);
  }

  @Test
  void handlesContentThatFinishesSynchronously() {
    output.finishImmediately = true;
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE));
    output.sequence = sequence;

    sequence.start();

    assertTrue(sequence.isFinished());
    assertFalse(sequence.isMovementLocked());
    assertEquals(1, finishedCount);
    assertEquals(List.of("lock", "dialogue:talk", "cutscene:scene", "unlock"), log);
  }

  @Test
  void cancelUnlocksWithoutFinishing() {
    NpcInteractionSequence sequence =
        sequence(config("talk", "scene", CutsceneTiming.AFTER_DIALOGUE));
    sequence.start();

    sequence.cancel();

    assertTrue(sequence.isFinished());
    assertFalse(sequence.isMovementLocked());
    assertEquals(0, finishedCount);
    assertFalse(sequence.onDialogueFinished("talk"));
    assertEquals(List.of("lock", "dialogue:talk", "unlock"), log);
  }

  @Test
  void startIsIdempotent() {
    NpcInteractionSequence sequence = sequence(config("talk", null, CutsceneTiming.AFTER_DIALOGUE));
    sequence.start();
    sequence.start();
    assertEquals(List.of("lock", "dialogue:talk"), log);
  }

  private NpcInteractionSequence sequence(InteractableNpcConfig config) {
    return new NpcInteractionSequence(config, output, () -> finishedCount++);
  }

  private static InteractableNpcConfig config(
      String dialogueId, String cutsceneId, CutsceneTiming timing) {
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = "npc";
    config.name = "NPC";
    config.dialogueId = dialogueId;
    config.cutsceneId = cutsceneId;
    config.cutsceneTiming = timing;
    return config;
  }

  private class Recorder implements NpcInteractionSequence.Output {
    private boolean finishImmediately;
    private NpcInteractionSequence sequence;

    @Override
    public void startDialogue(String dialogueId) {
      log.add("dialogue:" + dialogueId);
      if (finishImmediately) sequence.onDialogueFinished(dialogueId);
    }

    @Override
    public void startCutscene(String cutsceneId) {
      log.add("cutscene:" + cutsceneId);
      if (finishImmediately) sequence.onCutsceneFinished(cutsceneId);
    }

    @Override
    public void setMovementLocked(boolean locked) {
      log.add(locked ? "lock" : "unlock");
    }
  }
}
