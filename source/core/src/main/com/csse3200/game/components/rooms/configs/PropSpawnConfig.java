package com.csse3200.game.components.rooms.configs;

/**
 * Places an interactive training prop (a dummy to hit, a sign to read, a cannon to dodge) at a tile
 * in a room. Kept apart from EnemySpawnConfig so props never count towards clearing a room.
 */
public class PropSpawnConfig extends PositionConfig {
  /** What kind of prop to build; each kind uses only the optional fields documented beside it. */
  public enum PropType {
    /** A straw dummy that soaks up melee hits and shows how hard each one landed. */
    TRAINING_DUMMY,
    /** An archery target that soaks up arrows and keeps score of hits. */
    ARCHERY_TARGET,
    /** A notice board that plays {@link #dialogueId} when the player presses E. */
    SIGN,
    /**
     * A pedestal that lends the player one {@link #itemId} for as long as they stay in the room.
     */
    SUPPLY_STAND,
    /** A cannon that fires harmless orbs downwards so the player can practise dodging. */
    DODGE_CANNON
  }

  public PropType type;

  /** Name shown in interaction prompts and score messages, e.g. "Notice Board". */
  public String label;

  /** SIGN: id of the dialogue in configs/dialogues to play. */
  public String dialogueId;

  /** SUPPLY_STAND: id of the consumable on offer, from ItemIds. */
  public String itemId;

  /** DODGE_CANNON: seconds between shots. */
  public float interval = 2.4f;

  /** DODGE_CANNON: seconds before the first shot, so neighbouring cannons can be staggered. */
  public float delay;

  /** DODGE_CANNON: how many tiles an orb travels before it fades. */
  public float range = 20f;
}
