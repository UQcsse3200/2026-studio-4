package com.csse3200.game.shop;

/**
 * A single catalogue entry: {@code quantityPerPurchase} units of {@code itemId}, sold for {@code
 * price} gold per "lot". E.g. (itemId="arrow_bundle", price=5, quantityPerPurchase=10) means 5 gold
 * buys 10 arrows at once.
 */
public record ShopItem(String itemId, String displayName, int price, int quantityPerPurchase) {

  public ShopItem {
    if (price < 0) {
      throw new IllegalArgumentException("price must not be negative: " + price);
    }
    if (quantityPerPurchase < 1) {
      throw new IllegalArgumentException(
          "quantityPerPurchase must be at least 1: " + quantityPerPurchase);
    }
  }

  /** Convenience factory for the common case of buying a single unit per purchase. */
  public static ShopItem of(String itemId, String displayName, int price) {
    return new ShopItem(itemId, displayName, price, 1);
  }
}
