package com.csse3200.game.items;

/**
 * Item types that can be created by the item factory.
 *
 * <p>Each type owns the display and texture metadata needed by item, inventory, and UI features.
 * This implementation was developed with assistance from OpenAI Codex and reviewed by Yuezhou Wang.
 */
public enum ItemType {
  STRENGTH_CHARM(
      "Strength Charm",
      "A charm that strengthens its bearer.",
      "images/strength_charm_pixel.png",
      ItemCategory.CHARM),
  HEALTH_POTION(
      "Health Potion",
      "Restores health when consumed.",
      "images/health_potion_pixel.png",
      ItemCategory.CONSUMABLE),
  SHIELD(
      "Shield",
      "Provides temporary protection when consumed.",
      "images/shield_consumable_pixel.png",
      ItemCategory.CONSUMABLE),
  SPEED_POTION(
      "Speed Potion",
      "Temporarily increases movement speed.",
      "images/speed_potion_pixel.png",
      ItemCategory.CONSUMABLE),
  STRENGTH_POTION(
      "Strength Potion",
      "Temporarily increases attack strength.",
      "images/strength_potion_pixel.png",
      ItemCategory.CONSUMABLE),
  GOLD_COIN(
      "Gold Coin",
      "Currency dropped by defeated enemies.",
      "images/gold_coin_pixel.png",
      ItemCategory.CURRENCY);

  private final String displayName;
  private final String description;
  private final String texturePath;
  private final ItemCategory category;

  ItemType(String displayName, String description, String texturePath, ItemCategory category) {
    this.displayName = displayName;
    this.description = description;
    this.texturePath = texturePath;
    this.category = category;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public String getTexturePath() {
    return texturePath;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public boolean isConsumable() {
    return category == ItemCategory.CONSUMABLE;
  }

  public boolean isCurrency() {
    return category == ItemCategory.CURRENCY;
  }
}
