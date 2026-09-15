package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;

// Damage class for the game. Handles passing damage relevant values between systems.
public class Damage {
  private int damage;
  private final Entity attacker;

  public Damage(int damage, Entity attacker) {
    this.damage = damage;
    this.attacker = attacker;
  }

  public int getDamage() {
    return damage;
  }

  public void setDamage(int damage) {
    this.damage = damage;
  }

  public Entity getAttacker() {
    return attacker;
  }
}
