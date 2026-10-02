package com.csse3200.game.components.friendlynpc;

import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfig.CutsceneTiming;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Runs one NPC interaction: its dialogue and/or cutscene, in the configured order, locking player
 * movement only while a step that asks for it is running.
 *
 * Talks to the outside world through Output, and is told about progress through onDialogueFinished, 
 * onCutsceneFinished and onCutsceneCue.
 *
 * The dialogue or cutscene system may report "finished" synchronously from inside startDialogue/startCutscene
 * (for example when content is missing). Every output call is therefore the last thing a method does.
 */
public class NpcInteractionSequence {
  /** Side effects of the sequence. */
  public interface Output {
    void startDialogue(String dialogueId);

    void startCutscene(String cutsceneId);

    void setMovementLocked(boolean locked);
  }

  private enum Step {
    DIALOGUE,
    CUTSCENE
  }

  private final String dialogueId;
  private final String cutsceneId;
  private final CutsceneTiming timing;
  private final boolean lockDuringDialogue;
  private final boolean lockDuringCutscene;
  private final Output output;
  private final Runnable onFinished;
  private final Deque<Step> pending = new ArrayDeque<>();

  private boolean started;
  private boolean finished;
  private boolean dialogueActive;
  private boolean cutsceneActive;
  private boolean movementLocked;

  public NpcInteractionSequence(InteractableNpcConfig config, Output output, Runnable onFinished) {
    this.dialogueId = config.dialogueId;
    this.cutsceneId = config.cutsceneId;
    this.timing =
        config.cutsceneTiming == null ? CutsceneTiming.AFTER_DIALOGUE : config.cutsceneTiming;
    this.lockDuringDialogue = config.lockMovementDuringDialogue;
    this.lockDuringCutscene = config.lockMovementDuringCutscene;
    this.output = Objects.requireNonNull(output);
    this.onFinished = Objects.requireNonNull(onFinished);

    boolean hasDialogue = config.hasDialogue();
    boolean hasCutscene = config.hasCutscene();
    if (hasDialogue && hasCutscene && timing == CutsceneTiming.BEFORE_DIALOGUE) {
      pending.add(Step.CUTSCENE);
      pending.add(Step.DIALOGUE);
    } else {
      // DURING_DIALOGUE also queues the cutscene last: the cue pulls it forward, and if the cue
      // never arrives it still plays once the dialogue ends
      if (hasDialogue) pending.add(Step.DIALOGUE);
      if (hasCutscene) pending.add(Step.CUTSCENE);
    }
  }

  /** Begins the first step. Calling it again has no effect */
  public void start() {
    if (started || finished) {
      return;
    }
    started = true;
    advance();
  }

  public boolean onDialogueFinished(String finishedDialogueId) {
    if (finished || !dialogueActive || !Objects.equals(finishedDialogueId, dialogueId)) {
      return false;
    }
    dialogueActive = false;
    advance();
    refreshMovementLock();
    return true;
  }

  public boolean onCutsceneFinished(String finishedCutsceneId) {
    if (finished || !cutsceneActive || !Objects.equals(finishedCutsceneId, cutsceneId)) {
      return false;
    }
    dialogueActive = false;
    advance();
    refreshMovementLock();
    return true;
  }

  /**
   * Starts a during dialogue cutscene while its dialogue is still open
   *
   * @param cueDialogueId dialogue that reached its cue
   * @return true if the cutscene was started
   */
  public boolean onCutsceneCue(String cueDialogueId) {
    if (finished
        || timing != CutsceneTiming.DURING_DIALOGUE
        || !dialogueActive
        || !Objects.equals(cueDialogueId, dialogueId)
        || !pending.remove(Step.CUTSCENE)) {
      return false;
    }
    beginCutscene();
    return true;
  }

  /** Stops the sequence without completing it and releases the movement lock */
  public void cancel() {
    if (finished) {
      return;
    }
    finished = true;
    pending.clear();
    dialogueActive = false;
    cutsceneActive = false;
    refreshMovementLock();
  }

  public boolean isStarted() {
    return started;
  }

  public boolean isFinished() {
    return finished;
  }

  public boolean isDialogueActive() {
    return dialogueActive;
  }

  public boolean isCutsceneActive() {
    return cutsceneActive;
  }

  public boolean isMovementLocked() {
    return movementLocked;
  }

  /** Starts the next queued step once nothing is running, or finishes when nothing is left */
  private void advance() {
    if (finished || dialogueActive || cutsceneActive) {
      return;
    }
    Step next = pending.poll();
    if (next == null) {
      finish();
    } else if (next == Step.DIALOGUE) {
      beginDialogue();
    } else {
      beginCutscene();
    }
  }

  private void beginDialogue() {
    dialogueActive = true;
    refreshMovementLock();
    output.startDialogue(dialogueId);
  }

  private void beginCutscene() {
    cutsceneActive = true;
    refreshMovementLock();
    output.startCutscene(cutsceneId);
  }

  private void finish() {
    finished = true;
    refreshMovementLock();
    onFinished.run();
  }

  /** Locks while any running step needs it, and only calls out when the answer changes */
  private void refreshMovementLock() {
    boolean shouldLock =
        !finished
            && ((dialogueActive && lockDuringDialogue) || (cutsceneActive && lockDuringCutscene));
    if (shouldLock != movementLocked) {
      movementLocked = shouldLock;
      output.setMovementLocked(shouldLock);
    }
  }
}