package com.csse3200.game.components.friendlynpc;

/**
 * Event contract between the friendly NPC system and the dialogue and cutscene systems.
 *
 * <p>All events are triggered on, and listened for on, the <b>player</b> entity's event handler,
 * because the player outlives rooms and NPCs.
 *
 * <p>Outgoing (this system → other systems):
 *
 * <ul>
 *   <li>{@link #START_DIALOGUE} {@code (String dialogueId, Entity npc)}
 *   <li>{@link #START_CUTSCENE} {@code (String cutsceneId, Entity npc)}
 *   <li>{@link #INTERACTION_STARTED} {@code (String npcId, Entity npc)}
 *   <li>{@link #INTERACTION_FINISHED} {@code (String npcId, Entity npc)}
 *   <li>{@link #INTERACTION_CANCELLED} {@code (String npcId, Entity npc)}
 * </ul>
 *
 * <p>Incoming (other systems → this system):
 *
 * <ul>
 *   <li>{@link #DIALOGUE_FINISHED} {@code (String dialogueId)}: the dialogue has closed.
 *   <li>{@link #CUTSCENE_FINISHED} {@code (String cutsceneId)}: the cutscene has ended.
 *   <li>{@link #DIALOGUE_CUTSCENE_CUE} {@code (String dialogueId)}: optional; start a {@code
 *       DURING_DIALOGUE} cutscene now.
 * </ul>
 *
 * <p>Player controls stay locked until the relevant "finished" event arrives, so the dialogue and
 * cutscene systems must always send it, including when the player skips.
 *
 * <p>On the NPC entity itself, {@link #INDICATOR_SHOWN} and {@link #INDICATOR_HIDDEN} (no
 * arguments) fire when the NPC becomes, or stops being, the one the player can interact with.
 */
public final class NpcInteractionEvents {
  public static final String START_DIALOGUE = "startDialogue";
  public static final String START_CUTSCENE = "startCutscene";
  public static final String DIALOGUE_FINISHED = "dialogueFinished";
  public static final String CUTSCENE_FINISHED = "cutsceneFinished";
  public static final String DIALOGUE_CUTSCENE_CUE = "dialogueCutsceneCue";

  public static final String INTERACTION_STARTED = "npcInteractionStarted";
  public static final String INTERACTION_FINISHED = "npcInteractionFinished";
  public static final String INTERACTION_CANCELLED = "npcInteractionCancelled";

  public static final String INDICATOR_SHOWN = "npcIndicatorShown";
  public static final String INDICATOR_HIDDEN = "npcIndicatorHidden";

  private NpcInteractionEvents() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
