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
    List<ShopOffer> emptyOffers = List.of();
    List<ShopOffer> validOffers = List.of(health());
    List<ShopOffer> duplicateOffers = List.of(health(), health());
    assertThrows(IllegalArgumentException.class, () -> new ShopCatalog("merchant", emptyOffers));
    assertThrows(IllegalArgumentException.class, () -> new ShopCatalog(null, validOffers));
    assertThrows(
        IllegalArgumentException.class, () -> new ShopCatalog("merchant", duplicateOffers));
    for (int price : new int[] {0, -1}) {
      List<ShopOffer> invalidPriceOffers =
          List.of(
              new ShopOffer("health", ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, price));
      assertThrows(
          IllegalArgumentException.class, () -> new ShopCatalog("merchant", invalidPriceOffers));
    }
    for (String id : new String[] {"missing", ItemIds.STRENGTH_CHARM}) {
      List<ShopOffer> invalidProductOffers =
          List.of(new ShopOffer("offer", ShopProductKind.CONSUMABLE, id, 10));
      assertThrows(
          IllegalArgumentException.class, () -> new ShopCatalog("merchant", invalidProductOffers));
    }
    List<ShopOffer> missingIdOffers =
        List.of(new ShopOffer(null, ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, 10));
    assertThrows(
        IllegalArgumentException.class, () -> new ShopCatalog("merchant", missingIdOffers));
    List<ShopOffer> missingKindOffers =
        List.of(new ShopOffer("offer", null, ItemIds.HEALTH_POTION, 10));
    assertThrows(
        IllegalArgumentException.class, () -> new ShopCatalog("merchant", missingKindOffers));
  }

  @Test
  void shouldRejectMissingConfiguration() {
    assertThrows(
        IllegalArgumentException.class, () -> ShopCatalog.load("configs/shops/missing.json"));
  }

  @Test
  void shouldPreventCatalogueMutation() {
    ShopCatalog catalog = new ShopCatalog("merchant", List.of(health()));
    List<ShopOffer> offers = catalog.offers();
    assertThrows(UnsupportedOperationException.class, offers::clear);
    assertEquals(10, catalog.find("health").goldPrice());
    assertNull(catalog.find("missing"));
  }

  private static ShopOffer health() {
    return new ShopOffer("health", ShopProductKind.CONSUMABLE, ItemIds.HEALTH_POTION, 10);
  }
}
