package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/**
 * A status effect that simply runs for a fixed time from the moment it is created, on whoever it is
 * put on.
 *
 * <p>Anything that needs a countdown on an entity creates one of these and hands it to that
 * entity's status effects controller, which runs the countdown. It does not care who created it or
 * which entity it landed on, so an effect a player puts on an enemy works exactly like one they put
 * on themselves. A fresh instance is made per application, the same as a burn stack.
 */
public class StatusEffect {
  final GameTime time;
  private final long duration;
  private final long deadline;
  private Runnable onEnded;

  public StatusEffect(GameTime time, long duration) {
    this.time = time;
    this.duration = duration;
    this.deadline = time.getTime() + duration;
  }

  /** Sets the callback run once, after the effect expires or is removed. */
  public void setOnEnded(Runnable onEnded) {
    this.onEnded = onEnded;
  }

  /** Returns how long the effect lasts from creation, in milliseconds. */
  public long getDuration() {
    return duration;
  }

  public boolean update() {
    return isExpired();
  }

  public long getRemainingDuration() {
    return Math.max(0, deadline - time.getTime());
  }

  public void onRemoved() {
    if (onEnded != null) {
      onEnded.run();
    }
  }

  /** Returns whether the effect has run its course. Queries skip expired effects immediately. */
  public boolean isExpired() {
    return getRemainingDuration() <= 0;
  }

  /**
   * Returns whether hostiles should be unable to find, chase or damage the entity while this runs.
   */
  public boolean concealsOwner() {
    return false;
  }

  /**
   * Returns whether the entity should be unable to act at all while this runs: no movement, and no
   * attack the entity's own AI would otherwise decide to make.
   *
   * <p>Separate from a zero movement-speed multiplier, which only says the entity is standing
   * still. This says it is not choosing anything either.
   */
  public boolean immobilisesOwner() {
    return false;
  }

  /**
   * Returns what this effect multiplies the given effective stat by while it runs. The raw stats
   * are never touched, so charms and other adjustments keep working underneath and removing the
   * effect cannot leave a rounding remainder behind.
   */
  public float getStatMultiplier(Stat stat) {
    if (stat == null) {
      throw new IllegalArgumentException();
    }
    return 1f;
  }

  /**
   * Returns the colour this effect multiplies into the sprite of whatever it is on, or null to
   * leave the sprite alone. Lets an effect show itself without the renderer naming it.
   *
   * <p>Treat the returned colour as read only; the caller may be a renderer running every frame.
   */
  public Color getTint() {
    return null;
  }

  /**
   * Returns the colour this effect adds on top of the sprite of whatever it is on, or null to add
   * nothing. Unlike {@link #getTint()}, which multiplies and so cannot lighten a dark sprite at all
   * (black times any colour is still black), a glow shows on every sprite in the game.
   *
   * <p>Treat the returned colour as read only; the caller may be a renderer running every frame.
   * Its alpha is the strength of the glow rather than a transparency.
   */
  public Color getGlow() {
    return null;
  }
}
