package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;

/** Purchases the player's existing weapon upgrades through inventory gold. */
public class WeaponUpgradePurchaseEffect implements ShopPurchaseEffect {
  private final WeaponUpgradeComponent upgrades;

  public WeaponUpgradePurchaseEffect(WeaponUpgradeComponent upgrades) {
    this.upgrades = upgrades;
  }

  @Override
  public ShopPurchaseResult purchase(
      InventoryComponent inventory, String productId, int goldPrice) {
    if (upgrades == null) return ShopPurchaseResult.UNSUPPORTED_PRODUCT;
    return switch (inventory.tryPurchaseWeaponUpgrade(
        upgrades, WeaponUpgradeCatalog.weaponClass(productId), goldPrice)) {
      case SUCCESS -> ShopPurchaseResult.SUCCESS;
      case INSUFFICIENT_GOLD -> ShopPurchaseResult.INSUFFICIENT_GOLD;
      case ALREADY_UPGRADED -> ShopPurchaseResult.ALREADY_UPGRADED;
      case INVALID_WEAPON, INVALID_PRICE -> ShopPurchaseResult.INVALID_OFFER;
    };
  }
}
