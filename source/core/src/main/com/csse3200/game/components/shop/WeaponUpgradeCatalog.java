package com.csse3200.game.components.shop;

import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Team 5 shop descriptions and validated offers for existing Team 3 upgrades. */
public class WeaponUpgradeCatalog {
  private final ShopCatalog catalog;

  public WeaponUpgradeCatalog(ShopCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    Set<String> products = new HashSet<>();
    for (ShopOffer offer : catalog.offers()) {
      if (offer.kind() != ShopProductKind.WEAPON_UPGRADE
          || weaponClass(offer.productId()) == null
          || !products.add(offer.productId())) {
        throw new IllegalArgumentException("Expected one offer per supported weapon upgrade");
      }
    }
  }

  public static WeaponUpgradeCatalog load(String path) {
    return new WeaponUpgradeCatalog(ShopCatalog.load(path));
  }

  public ShopCatalog catalog() {
    return catalog;
  }

  public List<ShopOffer> offers() {
    return catalog.offers();
  }

  public static Class<? extends WeaponComponent> weaponClass(String productId) {
    if (productId == null) return null;
    return switch (productId) {
      case "sword" -> SwordWeaponComponent.class;
      case "knife" -> KnifeWeaponComponent.class;
      case "bow" -> BowWeaponComponent.class;
      default -> null;
    };
  }

  public static Descriptor describe(String productId) {
    if (productId == null) return null;
    return switch (productId) {
      case "sword" ->
          new Descriptor(
              "Sword",
              SwordWeaponComponent.TEXTURE,
              SwordWeaponComponent.UPGRADED_TEXTURE,
              "+20% light damage. Unlocks a 360-degree heavy sweep on K with +35% damage and twice the cooldown.");
      case "knife" ->
          new Descriptor(
              "Knife",
              KnifeWeaponComponent.TEXTURE,
              KnifeWeaponComponent.UPGRADED_TEXTURE,
              "+20% light damage. Unlocks a three-strike flurry on K, ending with a stronger stab. Heavy attacks have three times the cooldown.");
      case "bow" ->
          new Descriptor(
              "Bow",
              BowWeaponComponent.TEXTURE,
              BowWeaponComponent.UPGRADED_TEXTURE,
              "+20% light damage. Unlocks three spread arrows on K. Each heavy arrow deals normal damage; heavy attacks have twice the cooldown.");
      default -> null;
    };
  }

  public record Descriptor(String name, String texture, String upgradedTexture, String summary) {}
}
