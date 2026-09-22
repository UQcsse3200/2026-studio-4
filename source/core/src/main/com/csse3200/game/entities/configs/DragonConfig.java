package com.csse3200.game.entities.configs;

/** Combat and display settings for the Chinese Dragon mini-boss. */
public class DragonConfig extends BaseEntityConfig {
  public float width = 2f;
  public float height = width * 208f / 192f;
  public float healthBarScale = 2f;

  public DragonConfig() {
    health = 500;
    baseAttack = 20;
  }
}
