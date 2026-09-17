package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.Damage;

/** Indicator interface saying that the object does something when damaged. */
public interface Damageable {

  /**
   * Does the thing when damaged. Returns true if the object should be removed.
   *
   * @param damage the damage object to interact with
   * @return true if the object should be removed. False otherwise.
   */
  public boolean damage(Damage damage);
}
