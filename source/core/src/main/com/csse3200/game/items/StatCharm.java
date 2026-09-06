package com.csse3200.game.items;

import com.csse3200.game.components.CombatStatsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a charm item the player has that affects a stat
 *
 * @param <T> The type of the stat to apply
 */
public abstract class StatCharm<T extends Number> {
  private static final Logger logger = LoggerFactory.getLogger(StatCharm.class);
  private final String name;
  private final String description;
  private final String texture; // used when for inventory ui

  protected boolean applied = false;
  protected final T value; // The amount of stat to change per owned charm type

  protected StatCharm(String name, String description, T value, String texture) {
    this.name = name;
    this.description = description;
    this.value = value;
    this.texture = texture;
  }

  /**
   * Applies the stat change to the players CombatStatsComponent
   *
   * <p>Implementations should call the correct setter in the combat stats, then sets applied =
   * true.
   *
   * @param combatStats The CombatStatsComponent owned by the player entity
   */
  public abstract void applyStatChange(CombatStatsComponent combatStats);

  public abstract void removeStatChange(CombatStatsComponent combatStats);

  protected boolean checkApplied() {
    if (applied) {
      logger.error("Attempted to apply stat twice");
      return true;
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getTexture() {
    return texture;
  }
}
