package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.consumables.MagnetPotion;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
  void shouldOfferRegisteredMagnetPotion() {
    ShopCatalog catalog = ShopCatalog.load("configs/shops/merchant.json");
    ShopOffer offer = catalog.find(ItemIds.MAGNET_POTION);

    assertNotNull(offer);
    assertEquals(ShopProductKind.CONSUMABLE, offer.kind());
    assertEquals(ItemIds.MAGNET_POTION, offer.productId());
    assertEquals(20, offer.goldPrice());
    MagnetPotion potion =
        assertInstanceOf(MagnetPotion.class, ItemCatalog.create(offer.productId(), 1));
    assertEquals(1, potion.getQuantity());
  }

  @Test
  void shouldOfferEveryRegisteredConsumableAndCharmExactlyOnce() {
    ShopCatalog catalog = ShopCatalog.load("configs/shops/merchant.json");
    Set<String> expected =
        ItemCatalog.ids().stream()
            .filter(
                id -> {
                  ItemCategory category = ItemCatalog.create(id, 1).getCategory();
                  return category == ItemCategory.CONSUMABLE || category == ItemCategory.CHARM;
                })
            .collect(Collectors.toSet());
    Set<String> offered =
        catalog.offers().stream().map(ShopOffer::productId).collect(Collectors.toSet());
    assertEquals(expected, offered);
    assertEquals(expected.size(), catalog.offers().size());
    for (ShopOffer offer : catalog.offers()) {
      assertEquals(
          ItemCatalog.create(offer.productId(), 1).getCategory().name(), offer.kind().name());
    }
  }

  @Test
  void shouldRejectUnknownOrMismatchedCharms() {
    for (String id : new String[] {"missing", ItemIds.HEALTH_POTION, ItemIds.GOLD_COIN}) {
      List<ShopOffer> offers = List.of(new ShopOffer("charm", ShopProductKind.CHARM, id, 40));
      assertThrows(IllegalArgumentException.class, () -> new ShopCatalog("merchant", offers));
    }
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
