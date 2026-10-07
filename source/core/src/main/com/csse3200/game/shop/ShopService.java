package com.csse3200.game.shop;

/**
 * The core "Gold → purchase → item in inventory" transaction. Deliberately independent of any
 * UI/NPC/screen code, so it can be fully tested before a single pixel of shop UI exists.
 */
public final class ShopService {

  private final ShopCatalogue catalogue;
  private final ShopWallet wallet;

  public ShopService(ShopCatalogue catalogue, ShopWallet wallet) {
    this.catalogue = catalogue;
    this.wallet = wallet;
  }

  /** Buys one "lot" of {@code itemId}. */
  public PurchaseResult purchase(String itemId) {
    return purchase(itemId, 1);
  }

  /** Buys {@code count} lots of {@code itemId} in a single atomic transaction. */
  public PurchaseResult purchase(String itemId, int count) {
    if (count < 1) {
      return PurchaseResult.invalidQuantity();
    }

    var itemOpt = catalogue.find(itemId);
    if (itemOpt.isEmpty()) {
      return PurchaseResult.invalidItem();
    }
    ShopItem item = itemOpt.get();

    int totalCost = item.price() * count;
    if (!wallet.hasGold(totalCost)) {
      return PurchaseResult.insufficientFunds();
    }

    wallet.spendGold(totalCost);
    wallet.grantItem(item.itemId(), item.quantityPerPurchase() * count);
    return PurchaseResult.success(item);
  }
}
