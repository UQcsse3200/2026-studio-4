package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.components.maingame.InventoryDisplay;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A component used by the player to track Gold, charms, and consumable quantities. */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);
  private int gold;
  private final List<Charm> charms;
  private final Map<ItemType, Integer> consumables;

  private InventoryDisplay display;

  private boolean displayed;

  public InventoryComponent(int gold) {
    setGold(gold);
    this.charms = new ArrayList<>();
    this.consumables = new EnumMap<>(ItemType.class);
  }

  public int getGold() {
    return this.gold;
  }

  public Boolean hasGold(int gold) {
    return this.gold >= gold;
  }

  /** Sets the player's Gold, with a minimum value of zero. */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);
  }

  /** Adds to the player's Gold. The amount may be negative. */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }

  public List<Charm> getCharms() {
    return this.charms;
  }

  /** Adds a charm and publishes the existing inventory event when attached to an entity. */
  public void addCharm(Charm charm) {
    this.charms.add(charm);
    if (entity != null && entity.getEvents() != null) {
      entity.getEvents().trigger("charmAdded", charm);
    }
  }

  /** Removes a charm and publishes the existing inventory event when removal succeeds. */
  public boolean removeCharm(Charm charm) {
    boolean removed = this.charms.remove(charm);
    if (removed && entity != null && entity.getEvents() != null) {
      entity.getEvents().trigger("charmRemoved", charm);
    }
    return removed;
  }

  public boolean hasCharm(Charm charm) {
    return this.charms.contains(charm);
  }

  public int getCharmCount() {
    return this.charms.size();
  }

  /** Returns the stored quantity for a consumable type. */
  public int getConsumableCount(ItemType type) {
    if (type == null || !type.isConsumable()) {
      return 0;
    }
    return consumables.getOrDefault(type, 0);
  }

  public boolean hasConsumable(ItemType type) {
    return getConsumableCount(type) > 0;
  }

  /** Adds one consumable. */
  public void addConsumable(ItemType type) {
    addConsumable(type, 1);
  }

  /** Adds a positive quantity of one consumable type and emits one final-count event. */
  public void addConsumable(ItemType type, int quantity) {
    if (type == null || !type.isConsumable() || quantity <= 0) {
      return;
    }
    int newCount = getConsumableCount(type) + quantity;
    consumables.put(type, newCount);
    if (entity != null && entity.getEvents() != null) {
      entity.getEvents().trigger("consumableInventoryChanged", type, newCount);
    }
  }

  /** Removes one consumable if available. */
  public boolean removeConsumable(ItemType type) {
    if (type == null || !type.isConsumable()) {
      return false;
    }
    int currentCount = getConsumableCount(type);
    if (currentCount <= 0) {
      return false;
    }
    int newCount = currentCount - 1;
    if (newCount == 0) {
      consumables.remove(type);
    } else {
      consumables.put(type, newCount);
    }
    if (entity != null && entity.getEvents() != null) {
      entity.getEvents().trigger("consumableInventoryChanged", type, newCount);
    }
    return true;
  }

  public void setDisplay(InventoryDisplay display) {
    this.display = display;
  }

  public void toggleDisplay() {
    if (displayed) {
      this.display.setVisible(false);
      displayed = false;
    } else {
      this.display.setVisible(true);
      displayed = true;
    }
    ServiceLocator.getEntityService().toggleUpdate();
  }
}
