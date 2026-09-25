package com.csse3200.game.items;

import com.csse3200.game.entities.Entity;

/** An object that can be stored in the players inventory. */
public abstract class Item {
  private final String id;
  private final String name;
  private final String description;
  private final String texture; // used when inventory ui
  private final ItemCategory category;

  protected Item(String name, String description, String texture) {
    this(null, name, description, texture, null);
  }

  protected Item(
      String id, String name, String description, String texture, ItemCategory category) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.texture = texture;
    this.category = category;
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

  /** Stable identity shared by drops, inventory and UI. Legacy items may return null. */
  public String getId() {
    return id;
  }

  public ItemCategory getCategory() {
    return category;
  }

  /** Whether a single world item may represent multiple units. */
  public boolean isStackable() {
    return true;
  }

  /** Number of units represented by this item instance. */
  public int getQuantity() {
    return 1;
  }

  /**
   * A short summary of this item's mechanical effect, for example "Attack +10". Used by the
   * inventory tooltip so the player can see what an item actually does, rather than only its
   * flavour text.
   *
   * <p>Items with no stat effect worth showing keep the default empty string, and the tooltip omits
   * the line entirely.
   *
   * @return effect summary for display, never null
   */
  public String getEffectSummary() {
    return "";
  }

  /**
   * Formats a stat amount for display, dropping the decimal point when the amount is a whole number
   * so that 10f reads as "10" rather than "10.0".
   *
   * @param amount amount to format
   * @return the amount as display text
   */
  protected static String formatAmount(float amount) {
    if (amount == Math.rint(amount)) {
      return String.valueOf((int) amount);
    }
    return String.valueOf(amount);
  }
}
