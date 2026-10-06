package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.cutscene.CutsceneScript.TimedEvent;
import com.csse3200.game.components.cutscene.DialogueScript.Line;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs dialogues and cutscenes. It answers the requests in {@link CutsceneEvents} (sent on the
 * player's event handler), and for as long as something is playing it
 *
 * <ul>
 *   <li>freezes the game world: no enemy, projectile or trap updates, no physics, no run timer;
 *   <li>locks the player's controls; and
 *   <li>reports back with the matching "finished" event, however the sequence ended.
 * </ul>
 *
 * <p>A dialogue and a cutscene may run at the same time (a cutscene started from a dialogue line's
 * cue), and each releases only its own freeze and lock. This component sits on its own entity that
 * keeps updating while the world is frozen, not on the player.
 */
public class NarrativeManagerComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(NarrativeManagerComponent.class);

  private final Entity player;
  private final DialogueView dialogueView;
  private final CutsceneView cutsceneView;
  private final Object dialogueOwner = new Object();
  private final Object cutsceneOwner = new Object();
  private final Set<String> playedOnce = new HashSet<>();

  private DialogueScript activeDialogue;
  private CutsceneScript activeCutscene;
  private float cutsceneElapsed;
  private int nextTimedEvent;
  private TimedEvent[] sortedEvents = new TimedEvent[0];

  public NarrativeManagerComponent(
      Entity player, DialogueView dialogueView, CutsceneView cutsceneView) {
    this.player = player;
    this.dialogueView = dialogueView;
    this.cutsceneView = cutsceneView;
  }

  @Override
  public void create() {
    // EventHandler casts every listener of an event to one arity, so these must stay (id, npc).
    player
        .getEvents()
        .addListener(CutsceneEvents.START_DIALOGUE, (String id, Entity npc) -> playDialogue(id));
    player
        .getEvents()
        .addListener(CutsceneEvents.START_CUTSCENE, (String id, Entity npc) -> playCutscene(id));
    player
        .getEvents()
        .addListener(
            CutsceneEvents.START_DIALOGUE_SCRIPT, (DialogueScript script) -> playDialogue(script));
    player
        .getEvents()
        .addListener(
            CutsceneEvents.START_CUTSCENE_SCRIPT, (CutsceneScript script) -> playCutscene(script));
  }

  @Override
  public void update() {
    update(Gdx.graphics == null ? 0f : Gdx.graphics.getDeltaTime());
  }

  /** Advances timed cutscene events. Package-private so tests can drive time directly. */
  void update(float deltaSeconds) {
    if (activeCutscene == null) {
      return;
    }
    cutsceneElapsed += deltaSeconds;
    while (nextTimedEvent < sortedEvents.length
        && sortedEvents[nextTimedEvent].at <= cutsceneElapsed) {
      TimedEvent timed = sortedEvents[nextTimedEvent++];
      fireTimedEvent(timed);
      if (activeCutscene == null) {
        return;
      }
    }
  }

  private void fireTimedEvent(TimedEvent timed) {
    if (timed.event == null || timed.event.isBlank()) {
      return;
    }
    if (timed.arg == null) {
      player.getEvents().trigger(timed.event);
    } else {
      player.getEvents().trigger(timed.event, timed.arg);
    }
  }

  // ---------------------------------------------------------------- dialogue

  /**
   * Plays {@code configs/dialogues/<id>.json}. If the file is missing or invalid, nothing is shown
   * but DIALOGUE_FINISHED is still sent so callers never wait forever.
   *
   * @return true if the dialogue started
   */
  public boolean playDialogue(String id) {
    DialogueScript script = DialogueScript.load(id);
    if (script == null) {
      logger.warn("No dialogue named '{}' in {}", id, DialogueScript.DIRECTORY);
      player.getEvents().trigger(CutsceneEvents.DIALOGUE_FINISHED, id);
      return false;
    }
    return playDialogue(script);
  }

  /**
   * Plays a dialogue.
   *
   * @return true if the dialogue started. False if another dialogue is already open (in which case
   *     nothing is sent), or the script is empty or once-only and already played (in which case
   *     DIALOGUE_FINISHED is sent straight away).
   */
  public boolean playDialogue(DialogueScript script) {
    if (script == null || !script.hasLines()) {
      logger.warn("Ignoring empty dialogue '{}'", script == null ? null : script.id);
      if (script != null) {
        player.getEvents().trigger(CutsceneEvents.DIALOGUE_FINISHED, script.id);
      }
      return false;
    }
    if (activeDialogue != null) {
      logger.warn("Dialogue '{}' ignored: '{}' is still open", script.id, activeDialogue.id);
      return false;
    }
    if (script.once && !playedOnce.add("dialogue:" + script.id)) {
      logger.info("Dialogue '{}' is once-only and has already played", script.id);
      player.getEvents().trigger(CutsceneEvents.DIALOGUE_FINISHED, script.id);
      return false;
    }

    activeDialogue = script;
    hold(dialogueOwner, true);
    player.getEvents().trigger(CutsceneEvents.DIALOGUE_STARTED, script.id);
    dialogueView.show(
        script,
        new DialogueRunner.Listener() {
          @Override
          public void onLineShown(int index, Line line) {
            player.getEvents().trigger(CutsceneEvents.DIALOGUE_LINE_SHOWN, script.id, index);
            if (line.cue) {
              player.getEvents().trigger(CutsceneEvents.DIALOGUE_CUTSCENE_CUE, script.id);
            }
          }

          @Override
          public void onFinished() {
            finishDialogue(script);
          }
        });
    return true;
  }

  private void finishDialogue(DialogueScript script) {
    if (activeDialogue != script) {
      return;
    }
    // Clear state before notifying, so a listener can immediately start the next thing.
    activeDialogue = null;
    dialogueView.close();
    hold(dialogueOwner, false);
    player.getEvents().trigger(CutsceneEvents.DIALOGUE_FINISHED, script.id);
  }

  // ---------------------------------------------------------------- cutscene

  /**
   * Plays {@code configs/cutscenes/<id>.json}. If the file is missing or invalid nothing is shown
   * but CUTSCENE_FINISHED is still sent so callers never wait forever.
   *
   * @return true if the cutscene started
   */
  public boolean playCutscene(String id) {
    CutsceneScript script = CutsceneScript.load(id);
    if (script == null) {
      logger.warn("No cutscene named '{}' in {}", id, CutsceneScript.DIRECTORY);
      player.getEvents().trigger(CutsceneEvents.CUTSCENE_FINISHED, id);
      return false;
    }
    return playCutscene(script);
  }

  /**
   * Plays a cutscene.
   *
   * @return true if the cutscene started. False if another cutscene is already running (nothing is
   *     sent), or the script is once-only and already played (CUTSCENE_FINISHED is sent straight
   *     away).
   */
  public boolean playCutscene(CutsceneScript script) {
    if (script == null) {
      return false;
    }
    if (activeCutscene != null) {
      logger.warn("Cutscene '{}' ignored: '{}' is still playing", script.id, activeCutscene.id);
      return false;
    }
    if (script.once && !playedOnce.add("cutscene:" + script.id)) {
      logger.info("Cutscene '{}' is once-only and has already played", script.id);
      player.getEvents().trigger(CutsceneEvents.CUTSCENE_FINISHED, script.id);
      return false;
    }

    activeCutscene = script;
    cutsceneElapsed = 0f;
    nextTimedEvent = 0;
    sortedEvents = sortedEvents(script);
    hold(cutsceneOwner, true);
    player.getEvents().trigger(CutsceneEvents.CUTSCENE_STARTED, script.id);
    // The view may finish synchronously (e.g. a missing video), so nothing may follow this call.
    cutsceneView.play(script, () -> finishCutscene(script));
    return true;
  }

  private static TimedEvent[] sortedEvents(CutsceneScript script) {
    if (script.events == null) {
      return new TimedEvent[0];
    }
    return java.util.Arrays.stream(script.events)
        .filter(e -> e != null)
        .sorted(java.util.Comparator.comparingDouble(e -> e.at))
        .toArray(TimedEvent[]::new);
  }

  private void finishCutscene(CutsceneScript script) {
    if (activeCutscene != script) {
      return;
    }
    // Events scheduled for the very end still count if the cutscene ran to completion.
    activeCutscene = null;
    hold(cutsceneOwner, false);
    player.getEvents().trigger(CutsceneEvents.CUTSCENE_FINISHED, script.id);
  }

  // ------------------------------------------------------------------ input

  /**
   * @return true while a dialogue or cutscene is on screen, i.e. gameplay input must be blocked
   */
  public boolean isNarrativeActive() {
    return activeDialogue != null || activeCutscene != null;
  }

  public boolean isDialogueActive() {
    return activeDialogue != null;
  }

  public boolean isCutsceneActive() {
    return activeCutscene != null;
  }

  /**
   * The advance key (SPACE / ENTER / click): next dialogue line, otherwise skip the cutscene if it
   * allows it.
   *
   * @return true if the input was used
   */
  public boolean advance() {
    if (activeDialogue != null) {
      dialogueView.advance();
      return true;
    }
    return skipCutscene();
  }

  /**
   * Skips the running cutscene if it is skippable.
   *
   * @return true if there was a cutscene to skip
   */
  public boolean skipCutscene() {
    CutsceneScript script = activeCutscene;
    if (script == null) {
      return false;
    }
    if (script.skippable) {
      player.getEvents().trigger(CutsceneEvents.CUTSCENE_SKIPPED, script.id);
      cutsceneView.stop();
    }
    return true;
  }

  /** Forgets which once-only dialogues and cutscenes have played (e.g. on a new game). */
  public void resetPlayed() {
    playedOnce.clear();
  }

  // ----------------------------------------------------- freeze and controls

  private void hold(Object owner, boolean held) {
    if (ServiceLocator.getEntityService() != null) {
      ServiceLocator.getEntityService().setFrozen(owner, held);
    }
    PlayerActions actions = player.getComponent(PlayerActions.class);
    if (actions != null) {
      actions.setControlsLocked(owner, held);
    }
    InteractionPromptDisplay prompt = player.getComponent(InteractionPromptDisplay.class);
    if (held && prompt != null) {
      // The room does not refresh the "Press E" prompt while frozen, so don't leave it stale
      prompt.clearPrompt();
    }
  }

  @Override
  public void dispose() {
    // Never leave the game frozen if the entity is torn down mid-sequence.
    if (activeDialogue != null) {
      activeDialogue = null;
      hold(dialogueOwner, false);
    }
    if (activeCutscene != null) {
      activeCutscene = null;
      hold(cutsceneOwner, false);
    }
    super.dispose();
  }
}
