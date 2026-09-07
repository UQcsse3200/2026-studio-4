package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.items.StatCharm;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Currently only stores the gold amount but can be extended for more advanced functionality such
 * as storing items. Can also be used as a more generic component for other entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);
  private int gold;
  // Stores charms currently held by the player
  private final List<StatCharm<?>> charms;

  public InventoryComponent(int gold) {
    setGold(gold);
    this.charms = new ArrayList<>();
  }

  /**
   * Returns the player's gold.
   *
   * @return entity's health
   */
  public int getGold() {
    return this.gold;
  }

  /**
   * Returns if the player has a certain amount of gold.
   *
   * @param gold required amount of gold
   * @return player has greater than or equal to the required amount of gold
   */
  public Boolean hasGold(int gold) {
    return this.gold >= gold;
  }

  /**
   * Sets the player's gold. Gold has a minimum bound of 0.
   *
   * @param gold gold
   */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);
  }

  /**
   * Adds to the player's gold. The amount added can be negative.
   *
   * @param gold gold to add
   */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }

  /**
   * Returns the charms currently stored in the inventory.
   *
   * @return stored charms
   */
  public List<StatCharm<?>> getCharms() {
    return this.charms;
  }

  /**
   * Adds a charm to the player's inventory. Used when the player picks up a charm.
   *
   * @param charm charm to add
   */
  public void addCharm(StatCharm<?> charm) {
    this.charms.add(charm);

    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats != null) {
      charm.applyStatChange(combatStats);
    }
  }

  /**
   * Removes a charm from the player's inventory. Used when a charm is dropped or removed.
   *
   * @param charm charm to remove
   * @return true if the charm was successfully removed
   */
  public boolean removeCharm(StatCharm<?> charm) {
    boolean removed = this.charms.remove(charm);
    if (!removed) {
      return false;
    }

    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats != null) {
      charm.removeStatChange(combatStats);
    }
    return true;
  }

  /** Returns the entity's combat stats, or null if unavailable (e.g. in isolated unit tests). */
  private CombatStatsComponent getCombatStats() {
    return entity == null ? null : entity.getComponent(CombatStatsComponent.class);
  }

  /**
   * Checks whether the player currently has a specific charm. This can be used later when checking
   * charm effects or buffs.
   *
   * @param charm charm to check
   * @return true if the charm is stored in the inventory
   */
  public boolean hasCharm(StatCharm<?> charm) {
    return this.charms.contains(charm);
  }

  /**
   * Returns the number of charms currently held by the player. Useful for checking and testing the
   * inventory.
   *
   * @return number of stored charms
   */
  public int getCharmCount() {
    return this.charms.size();
  }
}
