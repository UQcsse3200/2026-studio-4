package com.csse3200.game.items.charms;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A charm applies an effect on pickup and removes it when dropped. */
public abstract class Charm extends Item {
  private static final Logger logger = LoggerFactory.getLogger(Charm.class);
  private boolean applied = false;

  Charm(String id, String name, String description, String texture) {
    super(id, name, description, texture, ItemCategory.CHARM);
  }

  @Override
  public boolean isStackable() {
    return false;
  }

  protected abstract void applyEffect(Entity player);

  protected abstract void removeEffect(Entity player);

  /** Wraps the applyEffect component to also guard against applying twice */
  @Override
  public void pickUp(Entity player) {
    if (applied) {
      logger.error("Attempted to apply effect twice");
      return;
    }
    player.getComponent(InventoryComponent.class).addCharm(this);
    applyEffect(player);
    applied = true;
  }

  @Override
  public void drop(Entity player) {
    removeEffect(player);
    player.getComponent(InventoryComponent.class).removeCharm(this);
    // can make it drop on the floor in the future
  }
}
