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
  public int burrowDamage = 5;

  public float stageTwoHealthThreshold = 0.5f;
  public float spitWindupDuration = 0.6f;
  public float spitVolleyInterval = 0.4f;
  public float spitRecoveryDuration = 0.35f;
  public int poisonVolleyCount = 5;
  public int poisonShotsPerVolley = 4;
  public float poisonFanDegrees = 54f;
  public float poisonWaveOffsetDegrees = 6f;
  public float poisonSpeed = 3f;
  public int poisonDamage = 1;
  public float poisonLifetime = 5f;
  public float poisonRadius = 0.12f;
  public float poisonVisualSize = 1.2f;

  public int shieldCapacity = 6;
  public int shieldGemInitialCount = 2;
  public int shieldGemMaxActive = 3;
  public float shieldGemSpawnInterval = 5f;
  public float shieldGemLifetime = 12f;
}
