package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.WeaponItem.WeaponType;
import java.util.Objects;

/** Remembers which wielder and weapon created an attack, independently of later weapon switches. */
public class WeaponAttackSourceComponent extends Component {
  private final Entity owner;
  private final WeaponType weaponType;

  /** Records the source when a weapon creates its hitbox. */
  public WeaponAttackSourceComponent(Entity owner, WeaponType weaponType) {
    this.owner = Objects.requireNonNull(owner, "owner");
    this.weaponType = Objects.requireNonNull(weaponType, "weaponType");
  }

  public Entity getOwner() {
    return owner;
  }

  public WeaponType getWeaponType() {
    return weaponType;
  }
}
