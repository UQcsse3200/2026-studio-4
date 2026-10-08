package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponUpgradeCatalogTest {
  @Test
  void descriptionsRejectMissingAndUnsupportedProductIds() {
    assertThrows(IllegalArgumentException.class, () -> WeaponUpgradeCatalog.describe(null));
    assertThrows(IllegalArgumentException.class, () -> WeaponUpgradeCatalog.describe("axe"));
  }

  @Test
  void configuredUpgradeOffersHaveCompletePreviewDescriptions() {
    WeaponUpgradeCatalog catalog =
        WeaponUpgradeCatalog.load("configs/shops/merchant-upgrades.json");
    for (ShopOffer offer : catalog.offers()) {
      WeaponUpgradeCatalog.Descriptor descriptor = WeaponUpgradeCatalog.describe(offer.productId());
      assertNotNull(descriptor);
      assertFalse(descriptor.name().isBlank());
      assertFalse(descriptor.texture().isBlank());
      assertFalse(descriptor.upgradedTexture().isBlank());
      assertFalse(descriptor.summary().isBlank());
    }
  }

  @Test
  void rejectsUnsupportedUpgradeProduct() {
    ShopCatalog catalog =
        new ShopCatalog(
            "bad", List.of(new ShopOffer("bad", ShopProductKind.WEAPON_UPGRADE, "axe", 60)));
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeCatalog(catalog));
  }

  @Test
  void rejectsOtherProductCategory() {
    ShopCatalog catalog =
        new ShopCatalog("bad", List.of(new ShopOffer("bad", ShopProductKind.CHARM, "sword", 60)));
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeCatalog(catalog));
  }

  @Test
  void rejectsMultipleOffersForSameOneTimeUpgrade() {
    ShopCatalog catalog =
        new ShopCatalog(
            "bad",
            List.of(
                new ShopOffer("one", ShopProductKind.WEAPON_UPGRADE, "sword", 60),
                new ShopOffer("two", ShopProductKind.WEAPON_UPGRADE, "sword", 80)));
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeCatalog(catalog));
  }
}
