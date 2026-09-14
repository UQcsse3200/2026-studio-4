package com.csse3200.game.items;

import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides pickup and drop behaviour for weapons. Player should "unlock" the weapon upon pickup */
public class WeaponItem extends Item {
  public static final Logger logger = LoggerFactory.getLogger(WeaponItem.class);

  // stores the type of the WeaponComponent to access
  public final Class<? extends WeaponComponent> type;

  public enum WeaponType {
    SWORD,
    BOW,
    DAGGER,
  }

  @Override
  public void pickUp(Entity player) {
    logger.info("Picked up weapon: {}", type);
    // requires the player to have a tracker for weapon unlocks
  }

  @Override
  public void drop(Entity player) {
    logger.error("Cannot drop a weapon");
  }

  WeaponItem(
      String name, String description, String texture, Class<? extends WeaponComponent> type) {
    super(name, description, texture);
    this.type = type;
  }

  /** factory function to help create weapon item types based on exisiting WeaponComponents. */
  public static WeaponItem createWeaponItem(WeaponType type) {
    switch (type) {
      case SWORD:
        return new WeaponItem(
            "Sword",
            "Just a boring sword",
            SwordWeaponComponent.TEXTURE,
            SwordWeaponComponent.class);
      case BOW:
        return new WeaponItem(
            "Bow", "Just a boring bow", BowWeaponComponent.TEXTURE, BowWeaponComponent.class);
      case DAGGER:
        return new WeaponItem(
            "Knife",
            "You can butter your toast with this",
            KnifeWeaponComponent.TEXTURE,
            KnifeWeaponComponent.class);
      default:
        throw new IllegalArgumentException();
    }
  }
}
