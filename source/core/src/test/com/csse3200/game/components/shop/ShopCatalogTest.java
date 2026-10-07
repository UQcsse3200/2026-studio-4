package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopCatalogTest {
  @Test
  void shouldRejectInvalidCatalogue() {
    assertThrows(IllegalArgumentException.class, () -> new ShopCatalog("merchant", List.of()));
    assertThrows(IllegalArgumentException.class, () -> new ShopCatalog(null, List.of(health())));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ShopCatalog("merchant", List.of(health(), health())));
    for (int price : new int[] {0, -1}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new ShopCatalog(
                  "merchant",
                  List.of(
                      new ShopOffer(
                          "health", ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, price))));
    }
    for (String id : new String[] {"missing", ItemIds.STRENGTH_CHARM}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new ShopCatalog(
                  "merchant", List.of(new ShopOffer("offer", ShopProductKind.CONSUMABLE, id, 10))));
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ShopCatalog(
                "merchant",
                List.of(
                    new ShopOffer(null, ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, 10))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ShopCatalog(
                "merchant", List.of(new ShopOffer("offer", null, ItemIds.HEALTH_POTION, 10))));
  }

  @Test
  void shouldRejectMissingConfiguration() {
    assertThrows(
        IllegalArgumentException.class, () -> ShopCatalog.load("configs/shops/missing.json"));
  }

  @Test
  void shouldPreventCatalogueMutation() {
    ShopCatalog catalog = new ShopCatalog("merchant", List.of(health()));
    assertThrows(UnsupportedOperationException.class, () -> catalog.offers().clear());
    assertEquals(10, catalog.find("health").goldPrice());
    assertNull(catalog.find("missing"));
  }

  private static ShopOffer health() {
    return new ShopOffer("health", ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, 10);
  }
}
