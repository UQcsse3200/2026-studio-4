package com.csse3200.game.physics;

/**
 * Physics layers used as Box2D category bits.
 *
 * <p>{@link #WEAPON} is the category for short-lived attack sensors. Use it as the hitbox layer,
 * not {@link #PLAYER}.
 */
public class PhysicsLayer {
  public static final short NONE = 0;
  public static final short DEFAULT = (1 << 0);
  public static final short PLAYER = (1 << 1);
  // Terrain obstacle, e.g. trees
  public static final short OBSTACLE = (1 << 2);
  // NPC (Non-Playable Character) colliders
  public static final short NPC = (1 << 3);

  /** Floor pit. Not solid to arrows; they fly over it. */
  public static final short HOLE = (1 << 5);

  /**
   * Category for short-lived weapon sensors (slashes, stabs, arrow splash).
   *
   * <p>Use this as the hitbox <em>layer</em>, and {@link #NPC} (or another victim layer) as the
   * {@code targetLayer} passed to {@code TouchAttackComponent}. Do not place weapon sensors on
   * {@link #PLAYER}: NPC touch-attacks that target the player would treat the slash as a player
   * body.
   */
  public static final short WEAPON = (1 << 4);

  public static final short ALL = ~0;

  public static boolean contains(short filterBits, short layer) {
    return (filterBits & layer) != 0;
  }

  private PhysicsLayer() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
