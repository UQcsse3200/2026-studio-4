package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Switches which of the wielder's weapons is equipped. Listens for {@code "switchWeapon"} (the L
 * key) and cycles sword &rarr; knife &rarr; bow &rarr; sword, skipping weapons the wielder does not
 * carry.
 *
 * <p>The equipped weapon is the enabled one, since only enabled weapons respond to attack input.
 * Nothing else is stored, so this stays in sync with anything else that enables weapons, such as
 * the {@code weapon} terminal command.
 *
 * <p>Triggers {@code "weaponSwitched"} with the newly equipped weapon class.
 */
public class WeaponSwitchComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(WeaponSwitchComponent.class);

  /** Order the weapons are cycled through. */
  static final List<Class<? extends WeaponComponent>> WEAPON_ORDER =
      List.of(SwordWeaponComponent.class, KnifeWeaponComponent.class, BowWeaponComponent.class);

  @Override
  public void create() {
    entity.getEvents().addListener("switchWeapon", this::switchToNext);
  }

  /**
   * Equip the next weapon the wielder carries, wrapping back to the first.
   *
   * @return the weapon class now equipped, or null if the wielder carries no weapons
   */
  public Class<? extends WeaponComponent> switchToNext() {
    Class<? extends WeaponComponent> equipped = getEquipped();
    // Immutable lists reject indexOf(null); with nothing equipped, start from the first weapon.
    int current = equipped == null ? -1 : WEAPON_ORDER.indexOf(equipped);
    for (int step = 1; step <= WEAPON_ORDER.size(); step++) {
      Class<? extends WeaponComponent> next =
          WEAPON_ORDER.get(Math.floorMod(current + step, WEAPON_ORDER.size()));
      if (equip(next)) {
        return next;
      }
    }
    return null;
  }

  /**
   * Equip a weapon, unequipping every other one.
   *
   * @param weapon weapon class to equip
   * @return false if the wielder does not carry that weapon
   */
  public boolean equip(Class<? extends WeaponComponent> weapon) {
    if (entity.getComponent(weapon) == null) {
      return false;
    }
    for (Class<? extends WeaponComponent> weaponClass : WEAPON_ORDER) {
      WeaponComponent carried = entity.getComponent(weaponClass);
      if (carried != null) {
        carried.setEnabled(weaponClass == weapon);
      }
    }
    logger.debug("Equipped {}", weapon.getSimpleName());
    entity.getEvents().trigger("weaponSwitched", weapon);
    return true;
  }

  /**
   * @return the equipped weapon class, or null if no weapon is equipped
   */
  public Class<? extends WeaponComponent> getEquipped() {
    for (Class<? extends WeaponComponent> weaponClass : WEAPON_ORDER) {
      WeaponComponent carried = entity.getComponent(weaponClass);
      if (carried != null && carried.isEnabled()) {
        return weaponClass;
      }
    }
    return null;
  }
}
