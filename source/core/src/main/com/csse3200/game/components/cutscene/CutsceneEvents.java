package com.csse3200.game.components.cutscene;

import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.entities.Entity;

/**
 * The public "event handles" of the dialogue and cutscene systems. Everything is triggered on, and
 * listened for on, the <b>player</b> entity's event handler, so any code that has the player can
 * start a dialogue or cutscene without knowing anything about how they are shown:
 *
 * <pre>{@code
 * // a dialogue from configs/dialogues/old_sage.json
 * CutsceneEvents.playDialogue(player, "old_sage");
 *
 * // a cutscene from configs/cutscenes/boss_defeated.json (an mp4 video)
 * CutsceneEvents.playCutscene(player, "boss_defeated");
 *
 * // react when either one ends
 * player.getEvents().addListener(CutsceneEvents.CUTSCENE_FINISHED, (String id) -> openDoor());
 * }</pre>
 *
 * <p>The same events are what the friendly NPC system uses (see {@link NpcInteractionEvents}), so
 * an NPC's {@code dialogueId} / {@code cutsceneId} are simply the file names of a dialogue or
 * cutscene script.
 *
 * <p>Both systems freeze the game world (no enemy moves, no physics, no run timer) and lock the
 * player's controls while they play, and always release both when they end, including when skipped.
 */
public final class CutsceneEvents {
  // ----- Requests (send these) -----

  /**
   * {@code (String dialogueId, Entity npc)}: play a dialogue file. {@code npc} is whoever started
   * it and may be null. Prefer {@link #playDialogue(Entity, String)}.
   */
  public static final String START_DIALOGUE = NpcInteractionEvents.START_DIALOGUE;

  /**
   * {@code (String cutsceneId, Entity npc)}: play a cutscene file. {@code npc} is whoever started
   * it and may be null. Prefer {@link #playCutscene(Entity, String)}.
   */
  public static final String START_CUTSCENE = NpcInteractionEvents.START_CUTSCENE;

  /** {@code (DialogueScript script)}: play a dialogue built in code instead of loaded from file. */
  public static final String START_DIALOGUE_SCRIPT = "startDialogueScript";

  /** {@code (CutsceneScript script)}: play a cutscene built in code instead of loaded from file. */
  public static final String START_CUTSCENE_SCRIPT = "startCutsceneScript";

  // ----- Notifications (listen to these) -----

  /** {@code (String dialogueId)}: a dialogue has opened. */
  public static final String DIALOGUE_STARTED = "dialogueStarted";

  /**
   * {@code (String dialogueId)}: the player left the dialogue early. Followed by DIALOGUE_FINISHED.
   */
  public static final String DIALOGUE_SKIPPED = "dialogueSkipped";

  /** {@code (String dialogueId, Integer lineIndex)}: a new line is now showing. */
  public static final String DIALOGUE_LINE_SHOWN = "dialogueLineShown";

  /** {@code (String dialogueId)}: the dialogue has closed. */
  public static final String DIALOGUE_FINISHED = NpcInteractionEvents.DIALOGUE_FINISHED;

  /**
   * {@code (String dialogueId)}: fired when a line flagged {@code "cue": true} is shown, so a
   * cutscene can start part-way through a dialogue.
   */
  public static final String DIALOGUE_CUTSCENE_CUE = NpcInteractionEvents.DIALOGUE_CUTSCENE_CUE;

  /** {@code (String cutsceneId)}: a cutscene has begun. */
  public static final String CUTSCENE_STARTED = "cutsceneStarted";

  /**
   * {@code (String cutsceneId)}: the player skipped the cutscene. Followed by CUTSCENE_FINISHED.
   */
  public static final String CUTSCENE_SKIPPED = "cutsceneSkipped";

  /** {@code (String cutsceneId)}: the cutscene has ended, however it ended. */
  public static final String CUTSCENE_FINISHED = NpcInteractionEvents.CUTSCENE_FINISHED;

  /** Plays a dialogue from {@code configs/dialogues/<dialogueId>.json}. */
  public static void playDialogue(Entity player, String dialogueId) {
    player.getEvents().trigger(START_DIALOGUE, dialogueId, (Entity) null);
  }

  /** Plays a dialogue built in code. */
  public static void playDialogue(Entity player, DialogueScript script) {
    player.getEvents().trigger(START_DIALOGUE_SCRIPT, script);
  }

  /** Plays a cutscene from {@code configs/cutscenes/<cutsceneId>.json}. */
  public static void playCutscene(Entity player, String cutsceneId) {
    player.getEvents().trigger(START_CUTSCENE, cutsceneId, (Entity) null);
  }

  /** Plays a cutscene built in code. */
  public static void playCutscene(Entity player, CutsceneScript script) {
    player.getEvents().trigger(START_CUTSCENE_SCRIPT, script);
  }

  private CutsceneEvents() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
