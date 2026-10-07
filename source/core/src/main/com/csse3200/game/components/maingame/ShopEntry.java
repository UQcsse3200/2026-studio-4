package com.csse3200.game.components.maingame;

/**
 * One purchasable line in the shop display: an item ID, a label to show the player, and its Gold
 * price.
 *
 * <p>This is a deliberately minimal placeholder data shape so {@link ShopDisplay} can be built and
 * tested today without depending on the real shop catalogue (Sprint 3 #196 work package B1), which
 * is being designed separately. Swap the source of this list for the agreed catalogue once it
 * lands; nothing else about the display needs to change as long as entries keep this shape.
 *
 * @param itemId stable {@code ItemIds} constant the entry sells
 * @param displayName label shown to the player
 * @param price Gold cost of one unit; must be positive
 */
public record ShopEntry(String itemId, String displayName, int price) {
  public ShopEntry {
    if (itemId == null || itemId.isBlank()) {
      throw new IllegalArgumentException("itemId must not be blank");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (price <= 0) {
      throw new IllegalArgumentException("price must be positive");
    }
  }
}
