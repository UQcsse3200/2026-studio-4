package com.csse3200.game.items;

import com.csse3200.game.entities.Entity;

/** An object that can be stored in the players inventory. */
public abstract class Item {
  private final String name;
  private final String description;
  private final String texture; // used when inventory ui

  protected Item(String name, String description, String texture) {
    this.name = name;
    this.description = description;
    this.texture = texture;
  }

  /**
   * Called when a player picks up an item
   *
   * <p>Implementations should define their behaviour upon pickup e.g. Add an item to inventory or
   * Set a weapon to unlocked
   */
  public abstract void pickUp(Entity player);

  /**
   * Called when a player drops an item
   *
   * <p>This method is specific to dropping items potentially stored in the inventory.
   */
  public abstract void drop(Entity player);

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
