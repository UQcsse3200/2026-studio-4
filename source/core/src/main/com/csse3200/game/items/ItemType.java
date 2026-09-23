package com.csse3200.game.items;

import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import java.util.function.BiFunction;

/**
 * Item types that can be created by the item factory.
 *
 * <p>Each type owns the display and texture metadata needed by item, inventory, and UI features.
 * This implementation was developed with assistance from OpenAI Codex and reviewed by Yuezhou Wang.
 */
public enum ItemType {
  STRENGTH_CHARM(
      "Strength Charm",
      "You feel yourself getting stronger.",
      "images/strength_charm_pixel.png",
      ItemCategory.CHARM,
      (type, quantity) -> new StrengthCharm()),
  SPEED_CHARM(
      "Speed Charm",
      "You feel yourself getting faster.",
      SpeedCharm.TEXTURE,
      ItemCategory.CHARM,
      (type, quantity) -> new SpeedCharm()),
  ATTACK_SPEED_CHARM(
      "Attack Speed Charm",
      "You feel yourself getting faster hands I guess?",
      AttackSpeedCharm.TEXTURE,
      ItemCategory.CHARM,
      (type, quantity) -> new AttackSpeedCharm()),
  HEALTH_POTION(
      "Health Potion",
      "Restores health when consumed.",
      "images/health_potion_pixel.png",
      ItemCategory.CONSUMABLE,
      ConsumableItem::new),
  SHIELD(
      "Shield",
      "Provides temporary protection when consumed.",
      "images/shield_consumable_pixel.png",
      ItemCategory.CONSUMABLE,
      ConsumableItem::new),
  SPEED_POTION(
      "Speed Potion",
      "Temporarily increases movement speed.",
      "images/speed_potion_pixel.png",
      ItemCategory.CONSUMABLE,
      ConsumableItem::new),
  STRENGTH_POTION(
      "Strength Potion",
      "Temporarily increases attack strength.",
      "images/strength_potion_pixel.png",
      ItemCategory.CONSUMABLE,
      ConsumableItem::new),
  GOLD_COIN(
      "Gold Coin",
      "Currency dropped by defeated enemies.",
      "images/gold_coin_pixel.png",
      ItemCategory.CURRENCY,
      CurrencyItem::new);

  private final String displayName;
  private final String description;
  private final String texturePath;
  private final ItemCategory category;
  private final BiFunction<ItemType, Integer, Item> creator;

  ItemType(
      String displayName,
      String description,
      String texturePath,
      ItemCategory category,
      BiFunction<ItemType, Integer, Item> creator) {
    this.displayName = displayName;
    this.description = description;
    this.texturePath = texturePath;
    this.category = category;
    this.creator = creator;
  }

  /** The sole registration of how a stable item ID becomes a fresh item instance. */
  public Item createItem(int quantity) {
    if (quantity <= 0 || (category == ItemCategory.CHARM && quantity != 1)) {
      throw new IllegalArgumentException("Invalid quantity for " + this + ": " + quantity);
    }
    return creator.apply(this, quantity);
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
