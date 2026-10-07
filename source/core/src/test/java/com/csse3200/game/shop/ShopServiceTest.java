package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopServiceTest {

  static class FakeWallet implements ShopWallet {
    int gold;
    final List<String> grants = new ArrayList<>();
    boolean spendGoldCalled = false;

    FakeWallet(int startingGold) {
      this.gold = startingGold;
    }

    @Override
    public boolean hasGold(int amount) {
      return gold >= amount;
    }

    @Override
    public void spendGold(int amount) {
      spendGoldCalled = true;
      gold -= amount;
    }

    @Override
    public void grantItem(String itemId, int quantity) {
      for (int i = 0; i < quantity; i++) {
        grants.add(itemId);
      }
    }
  }

  private ShopCatalogue catalogueWithOnePotion() {
    return new ShopCatalogue(List.of(ShopItem.of("health_potion", "Health Potion", 10)));
  }

  @Test
  void successfulPurchaseDeductsGoldAndGrantsItem() {
    FakeWallet wallet = new FakeWallet(50);
    ShopService shop = new ShopService(catalogueWithOnePotion(), wallet);

    PurchaseResult result = shop.purchase("health_potion");

    assertEquals(PurchaseResult.Status.SUCCESS, result.status());
    assertEquals(40, wallet.gold);
    assertEquals(List.of("health_potion"), wallet.grants);
  }

  @Test
  void rejectedWhenNotEnoughGold() {
    FakeWallet wallet = new FakeWallet(5);
    ShopService shop = new ShopService(catalogueWithOnePotion(), wallet);

    PurchaseResult result = shop.purchase("health_potion");

    assertEquals(PurchaseResult.Status.INSUFFICIENT_FUNDS, result.status());
    assertFalse(wallet.spendGoldCalled, "gold must not be touched on a rejected purchase");
    assertEquals(5, wallet.gold);
    assertEquals(0, wallet.grants.size());
  }

  @Test
  void rejectedForUnknownItem() {
    FakeWallet wallet = new FakeWallet(100);
    ShopService shop = new ShopService(catalogueWithOnePotion(), wallet);

    PurchaseResult result = shop.purchase("does_not_exist");

    assertEquals(PurchaseResult.Status.INVALID_ITEM, result.status());
    assertEquals(100, wallet.gold);
  }

  @Test
  void buyingMultipleLotsAtOnceChargesAndGrantsCorrectTotal() {
    FakeWallet wallet = new FakeWallet(100);
    ShopCatalogue catalogue = new ShopCatalogue(
        List.of(new ShopItem("arrow_bundle", "Arrow Bundle", 5, 10)));
    ShopService shop = new ShopService(catalogue, wallet);

    PurchaseResult result = shop.purchase("arrow_bundle", 3);

    assertEquals(PurchaseResult.Status.SUCCESS, result.status());
    assertEquals(100 - (5 * 3), wallet.gold);
    assertEquals(30, wallet.grants.size(), "3 lots x 10 arrows each");
  }
}