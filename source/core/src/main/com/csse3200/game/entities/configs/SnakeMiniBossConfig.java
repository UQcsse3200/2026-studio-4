package com.csse3200.game.entities.configs;

import com.badlogic.gdx.math.Vector2;

/**
 * Defines the properties stored in the snake mini-boss config file to be loaded by the NPC Factory.
 */
public class SnakeMiniBossConfig extends BaseEntityConfig {
  public Vector2 movement = new Vector2(2f, 2f);
  public float burrowDuration = 0.45f;
  public float undergroundDuration = 2.4f;
  public float warningDuration = 0.9f;
  public float exposedDuration = 1.5f;
  public float burrowSpeed = 3.5f;
  public float orbitRadius = 1.8f;
  public float burrowAttackRadius = 0.9f;
  public int burrowDamage = 2;
}
