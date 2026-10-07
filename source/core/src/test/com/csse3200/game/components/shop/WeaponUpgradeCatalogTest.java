package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponUpgradeCatalogTest {
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
