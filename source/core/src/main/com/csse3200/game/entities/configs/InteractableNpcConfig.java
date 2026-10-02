package com.csse3200.game.entities.configs;

/**
 * Declarative definition of one friendly, interactable NPC, loaded from configs/friendlyNpcs.json.
 *
 * Everything that makes one NPC different from another lives here, so new NPCs are added by
 * editing JSON rather than the interaction code. Friendly NPCs are deliberately kept apart from the
 * enemy configs in NPCConfigs.
 */
public class InteractableNpcConfig {
  /** When the cutscene plays relative to the dialogue, if the NPC has both. */
  public enum CutsceneTiming {
    /** Dialogue first, then the cutscene once the dialogue finishes. */
    AFTER_DIALOGUE,
    /** Cutscene first, then the dialogue once the cutscene finishes. */
    BEFORE_DIALOGUE,
    /**
     * The cutscene starts part-way through the dialogue, when the dialogue system fires 
     * dialogueCutsceneCue. If no cue arrives, the cutscene plays after the dialogue instead so it
     * is never skipped.
     */
    DURING_DIALOGUE
  }

  /** Stable id. Used by rooms to place the NPC and to remember completed interactions. */
  public String id;

  /** Display name shown in the interaction prompt, e.g. "Old Sage". */
  public String name;

  /** Verb used in the prompt: "Press E — {promptVerb} {name}". */
  public String promptVerb = "Talk to";

  /** Texture atlas for an animated sprite. Takes priority over texture. */
  public String atlas;

  /** Looping animation to play from atlas. */
  public String animation;

  /** Static texture for NPCs without an atlas. */
  public String texture;

  /** Sprite width in world units; height follows the art's aspect ratio. */
  public float width = 1f;

  /** Distance, in world units between centres, at which the player can interact. */
  public float interactionRange = 1.5f;

  /** Dialogue to start, or null for an NPC that only plays a cutscene. */
  public String dialogueId;

  /** Cutscene to start, or null for an NPC that only has dialogue. */
  public String cutsceneId;

  /** Ordering used when both dialogueId and cutsceneId are set. */
  public CutsceneTiming cutsceneTiming = CutsceneTiming.AFTER_DIALOGUE;

  /** Whether player movement, dashing and attacks are locked while the dialogue runs. */
  public boolean lockMovementDuringDialogue = true;

  /** Whether player movement, dashing and attacks are locked while the cutscene runs. */
  public boolean lockMovementDuringCutscene = true;

  /** If true, the interaction can only be completed once per play session. */
  public boolean once;

  /** If true, the NPC only responds once every enemy in its room has been defeated. */
  public boolean requiresRoomCleared;

  /** Ids of NPCs whose interactions must have been completed first. */
  public String[] requiresCompleted = new String[0];

  /** Prompt shown in range while conditions are unmet; null shows nothing. */
  public String unavailableMessage;

  /** Prompt shown in range after a once-only interaction is used; null shows nothing. */
  public String completedMessage;

  /**
   * @return true if the NPC starts a dialogue
   */
  public boolean hasDialogue() {
    return dialogueId != null && !dialogueId.isBlank();
  }

  /**
   * @return true if the NPC starts a cutscene
   */
  public boolean hasCutscene() {
    return cutsceneId != null && !cutsceneId.isBlank();
  }
}
