package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Map;
import java.util.Objects;

/** Resolves trusted catalogue prices and dispatches purchases to registered category handlers. */
public class ShopService {
  private final InventoryComponent inventory;
  private final ShopCatalog catalog;
  private final Map<ShopProductKind, ShopPurchaseEffect> effects;

  public ShopService(
      InventoryComponent inventory,
      ShopCatalog catalog,
      Map<ShopProductKind, ShopPurchaseEffect> effects) {
    this.inventory = Objects.requireNonNull(inventory);
    this.catalog = Objects.requireNonNull(catalog);
    this.effects = Map.copyOf(effects);
  }

  public ShopPurchaseResult purchase(String offerId) {
    ShopOffer offer = catalog.find(offerId);
    if (offer == null) {
      return ShopPurchaseResult.INVALID_OFFER;
    }
    ShopPurchaseEffect effect = effects.get(offer.kind());
    if (effect == null) {
      return ShopPurchaseResult.UNSUPPORTED_PRODUCT;
    }
    return effect.purchase(inventory, offer.productId(), offer.goldPrice());
  }
}
