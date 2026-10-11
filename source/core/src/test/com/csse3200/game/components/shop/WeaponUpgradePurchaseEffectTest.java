package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.extensions.GameExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponUpgradePurchaseEffectTest {
  @Test
  void configuredOffersPurchaseAllThreeExistingUpgradesOnce() {
    InventoryComponent inventory = new InventoryComponent(200);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    ShopService service =
        new ShopService(
            inventory, loadCatalog(), Map.of(ShopProductKind.WEAPON_UPGRADE, effect(upgrades)));
    assertEquals(ShopPurchaseResult.SUCCESS, service.purchase("sword"));
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertEquals(140, inventory.getGold());
    assertEquals("ALREADY_UPGRADED", service.purchase("sword").toString());
    assertEquals(140, inventory.getGold());
    assertEquals(ShopPurchaseResult.SUCCESS, service.purchase("knife"));
    assertEquals(ShopPurchaseResult.SUCCESS, service.purchase("bow"));
    assertTrue(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertTrue(upgrades.isUpgraded(BowWeaponComponent.class));
    assertEquals(20, inventory.getGold());
  }

  @Test
  void rejectsInvalidProductsAndPricesWithoutPayment() {
    InventoryComponent inventory = new InventoryComponent(100);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    ShopPurchaseEffect effect = effect(upgrades);
    assertEquals(ShopPurchaseResult.INVALID_OFFER, effect.purchase(inventory, "missing", 60));
    assertEquals(ShopPurchaseResult.INVALID_OFFER, effect.purchase(inventory, null, 60));
    assertEquals(ShopPurchaseResult.INVALID_OFFER, effect.purchase(inventory, "sword", 0));
    assertEquals(100, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void insufficientAndUnsupportedPurchasesDoNotCharge() {
    InventoryComponent inventory = new InventoryComponent(59);
    assertEquals(
        ShopPurchaseResult.INSUFFICIENT_GOLD,
        effect(new WeaponUpgradeComponent()).purchase(inventory, "sword", 60));
    assertEquals(59, inventory.getGold());
    inventory.setGold(100);
    assertEquals(
        ShopPurchaseResult.INVALID_OFFER,
        effect(new WeaponUpgradeComponent(Map.of())).purchase(inventory, "sword", 60));
    assertEquals(100, inventory.getGold());
  }

  @Test
  void missingPlayerUpgradeComponentRejectsWithoutCharging() {
    InventoryComponent inventory = new InventoryComponent(100);
    assertEquals(
        ShopPurchaseResult.UNSUPPORTED_PRODUCT, effect(null).purchase(inventory, "sword", 60));
    assertEquals(100, inventory.getGold());
  }

  private ShopPurchaseEffect effect(WeaponUpgradeComponent upgrades) {
    return new WeaponUpgradePurchaseEffect(upgrades);
  }

  private ShopCatalog loadCatalog() {
    return WeaponUpgradeCatalog.load("configs/shops/merchant-upgrades.json").catalog();
  }
}
