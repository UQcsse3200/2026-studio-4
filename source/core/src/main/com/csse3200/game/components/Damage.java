package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;

// Damage class for the game. Handles passing damage relevant values between systems.
public class Damage {
  private int damageValue;
  private final Entity attacker;

  public Damage(int damage, Entity attacker) {
    this.damageValue = damage;
    this.attacker = attacker;
  }

  public int getDamage() {
    return damageValue;
  }

  public void setDamage(int damage) {
    this.damageValue = damage;
  }

  public Entity getAttacker() {
    return attacker;
  }
}
