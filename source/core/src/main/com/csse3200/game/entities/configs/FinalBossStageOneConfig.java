package com.csse3200.game.entities.configs;

/** Configurable gameplay values for Final Boss Stage 1. */
public class FinalBossStageOneConfig {
  // Boss and summon health
  public int bossHealth = 100;
  public int waveOneSummonCount = 4;
  public int waveTwoSummonCount = 6;
  public int summonHealth = 10;
  public int summonExplosionDamage = 10;

  // Summon movement and explosion
  public float waveOneSummonSpeed = 1f;
  public float waveTwoSummonSpeed = 1.6f;
  public float summonTriggerDistance = 0.8f;
  public float summonExplosionRadius = 1.5f;
  public float waveOneWarningDuration = 1f;
  public float summonSpawnRadius = 2f;

  // Seven-second vulnerability window
  public float breakWindowDuration = 7f;
  public float breakWindowDamageMultiplier = 0.25f;
  public float breakWindowHealthFloor = 0.9f;
  public float stageTwoStartingHealth = 0.8f;

  // First-wave proximity damage
  public float proximityDamageRadius = 1.5f;
  public int proximityDamage = 2;
  public float proximityDamageInterval = 1f;

  // Boss movement
  public float bossWanderSpeed = 0.8f;
  public float bossFleeSpeed = 1.2f;
  public float movementTargetRefreshInterval = 0.5f;
  public float summonAvoidanceRadius = 3f;
  public float movementTargetDistance = 3f;

  // Petrification punishment
  public float petrificationWarningDuration = 1f;
  public float petrificationRadius = 1.2f;
  public float petrificationSlowMultiplier = 0.5f;
  public float petrificationSlowDuration = 2f;
  public float petrificationCooldown = 4f;

  /** Validates values required by the Stage 1 runtime. */
  public void validate() {
    if (bossHealth <= 0) {
      throw new IllegalArgumentException("Boss health must be positive");
    }

    if (waveOneSummonCount <= 0 || waveTwoSummonCount <= waveOneSummonCount) {
      throw new IllegalArgumentException("Wave two must contain more summons than wave one");
    }

    if (summonHealth <= 0 || summonExplosionDamage < 0) {
      throw new IllegalArgumentException("Summon health and damage values are invalid");
    }

    if (waveOneSummonSpeed < 0f || waveTwoSummonSpeed <= waveOneSummonSpeed) {
      throw new IllegalArgumentException("Wave two summons must be faster than wave one summons");
    }

    if (summonTriggerDistance < 0f
        || summonExplosionRadius < 0f
        || waveOneWarningDuration < 0f
        || summonSpawnRadius < 0f) {
      throw new IllegalArgumentException("Summon distances and timings must be non-negative");
    }

    if (breakWindowDuration < 0f
        || breakWindowDamageMultiplier < 0f
        || breakWindowHealthFloor < 0f
        || breakWindowHealthFloor > 1f
        || stageTwoStartingHealth < 0f
        || stageTwoStartingHealth > 1f
        || stageTwoStartingHealth >= breakWindowHealthFloor) {
      throw new IllegalArgumentException("Break-window health values are invalid");
    }

    if (proximityDamageRadius < 0f || proximityDamage < 0 || proximityDamageInterval <= 0f) {
      throw new IllegalArgumentException("Proximity damage values are invalid");
    }

    if (bossWanderSpeed < 0f
        || bossFleeSpeed < 0f
        || movementTargetRefreshInterval <= 0f
        || summonAvoidanceRadius < 0f
        || movementTargetDistance < 0f) {
      throw new IllegalArgumentException("Boss movement values are invalid");
    }

    if (petrificationWarningDuration < 0f
        || petrificationRadius < 0f
        || petrificationSlowMultiplier < 0f
        || petrificationSlowMultiplier > 1f
        || petrificationSlowDuration < 0f
        || petrificationCooldown <= 0f) {
      throw new IllegalArgumentException("Petrification values are invalid");
    }
  }
}
