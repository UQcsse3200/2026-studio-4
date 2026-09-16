package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.WeaponItem;
import com.csse3200.game.items.WeaponItem.WeaponType;
import java.util.List;

/** Owns the player's equipped weapon; keyboard, terminal, and UI share this state. */
public class WeaponSelectionComponent extends Component {
  public static final List<WeaponType> SLOT_WEAPONS =
      List.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.BOW);
  private final List<WeaponItem> weapons =
      SLOT_WEAPONS.stream().map(WeaponItem::createWeaponItem).toList();
  private WeaponType selectedWeapon;

  @Override
  public void create() {
    entity.getEvents().addListener("equipWeapon", this::equip);
    equip(WeaponType.SWORD);
  }

  /**
   * Equips an available weapon without changing shared combat stats or cooldown.
   *
   * @param type requested weapon, or null for an invalid selection
   * @return whether the requested weapon is equipped
   */
  public boolean equip(WeaponType type) {
    int index = type == null ? -1 : SLOT_WEAPONS.indexOf(type);
    if (index < 0 || entity.getComponent(weapons.get(index).type) == null) {
      return false;
    }
    if (type == selectedWeapon) {
      return true;
    }
    for (int i = 0; i < weapons.size(); i++) {
      WeaponComponent weapon = entity.getComponent(weapons.get(i).type);
      if (weapon != null) {
        weapon.setEnabled(i == index);
      }
    }
    selectedWeapon = type;
    entity.getEvents().trigger("weaponSelected", type);
    return true;
  }

  /**
   * @return selected weapon, or null if none could be equipped
   */
  public WeaponType getSelectedWeapon() {
    return selectedWeapon;
  }

  /**
   * @return read-only item descriptors in hotbar order: sword, knife, bow
   */
  public List<WeaponItem> getWeapons() {
    return weapons;
  }
}
