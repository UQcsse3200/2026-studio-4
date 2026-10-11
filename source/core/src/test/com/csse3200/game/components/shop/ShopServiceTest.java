package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopServiceTest {
  @Test
  void shouldUseCataloguePrice() {
    InventoryComponent inventory = new InventoryComponent(25);
    ShopService service =
        new ShopService(
            inventory,
            new ShopCatalog(
                "merchant",
                List.of(
                    new ShopOffer(
                        "health", ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, 10))),
            Map.of(ShopProductKind.CONSUMABLE, new ConsumablePurchaseEffect()));
    assertEquals(ShopPurchaseResult.SUCCESS, service.purchase("health"));
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(ShopPurchaseResult.SUCCESS, service.purchase("health"));
    assertEquals(ShopPurchaseResult.INSUFFICIENT_GOLD, service.purchase("health"));
    assertEquals(5, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldRejectUnknownOffer() {
    InventoryComponent inventory = new InventoryComponent(25);
    ShopService service =
        new ShopService(
            inventory,
            ShopCatalog.load("configs/shops/merchant.json"),
            Map.of(ShopProductKind.CONSUMABLE, new ConsumablePurchaseEffect()));
    assertEquals(ShopPurchaseResult.INVALID_OFFER, service.purchase("missing"));
    assertEquals(ShopPurchaseResult.INVALID_OFFER, service.purchase(null));
    assertEquals(25, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldRejectUnregisteredProductKind() {
    InventoryComponent inventory = new InventoryComponent(25);
    for (ShopProductKind kind : List.of(ShopProductKind.CHARM, ShopProductKind.WEAPON_UPGRADE)) {
      ShopService service =
          new ShopService(
              inventory,
              new ShopCatalog("future", List.of(new ShopOffer("future-offer", kind, "future", 10))),
              Map.of(ShopProductKind.CONSUMABLE, new ConsumablePurchaseEffect()));
      assertEquals(ShopPurchaseResult.UNSUPPORTED_PRODUCT, service.purchase("future-offer"));
      assertEquals(25, inventory.getGold());
    }
  }

  @Test
  void shouldDispatchRegisteredFutureHandler() {
    InventoryComponent inventory = new InventoryComponent(25);
    ShopService service =
        new ShopService(
            inventory,
            new ShopCatalog(
                "future",
                List.of(
                    new ShopOffer("upgrade", ShopProductKind.WEAPON_UPGRADE, "sword-level-2", 20))),
            Map.of(
                ShopProductKind.WEAPON_UPGRADE,
                (account, productId, price) -> {
                  assertSame(inventory, account);
                  assertEquals("sword-level-2", productId);
                  assertEquals(20, price);
                  return ShopPurchaseResult.QUANTITY_LIMIT;
                }));
    assertEquals(ShopPurchaseResult.QUANTITY_LIMIT, service.purchase("upgrade"));
    assertEquals(25, inventory.getGold());
  }

  @Test
  void shouldBuyExistingConsumables() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopService service =
        new ShopService(
            inventory,
            ShopCatalog.load("configs/shops/merchant.json"),
            Map.of(ShopProductKind.CONSUMABLE, new ConsumablePurchaseEffect()));
    for (String id :
        List.of(
            ItemIds.HEALTH_POTION,
            ItemIds.SHIELD,
            ItemIds.SPEED_POTION,
            ItemIds.STRENGTH_POTION,
            ItemIds.FREEZE_BOMB)) {
      assertEquals(ShopPurchaseResult.SUCCESS, service.purchase(id));
      assertEquals(1, inventory.getConsumableCount(id));
    }
    assertEquals(20, inventory.getGold());
  }
}
