package com.csse3200.game.entities.configs;

public class FinalBossStageTwoConfig {
  // Fixed Stage 2 arena and continuous slow movement.
  public float arenaMargin = 0.25f;
  public float bossMoveSpeed = 1.2f;
  public float bossRoamRetargetInterval = 3f;

  // Outward fire volleys. The pause stops new shots, not existing projectiles.
  public float fireballSpeed = 2.4f;
  public int fireballDamage = 8;
  public float fireVolleyInterval = 0.3f;
  public float fireballLifetime = 6f;
  public float fireballRadius = 0.16f;
  public float fireballInitialDelay = 0.6f;
  public int maxFireballs = 128;
  public float fireImpactDuration = 0.35f;

  // Random ice cover. Zero count disables cover for isolated encounter tests.
  public int iceCoverCount = 6;
  public float iceCoverWidth = 0.9f;
  public float iceCoverHeight = 1.8f;
  public float iceCoverRespawnInterval = 2f;
  public int iceCoverRespawnBatch = 2;
  public float iceCoverLifetime = 7f;
  public int iceCoverHits = 4;
  public float iceCoverGap = 1.25f;
  public float iceShatterDuration = 0.55f;
  public float iceSpawnDuration = 0.6f;

  // Stage 2 charge attacks
  public float bossChargeAttackDelay = 0.5f;
  public float bossChargeDistance = 6f;
  public float stageThreeHealthThreshold = 0.6f;

  // Stage 2 contact damage
  public float stageTwoContactDamageRadius = 1f;
  public int stageTwoContactDamage = 1;
  public float stageTwoContactDamageInterval = 0.05f;

  // Stage 2 attack/pause cycle
  public float attackDuration = 20f; // Boss attacks for 20 seconds
  public float pauseDuration = 3f; // Firing pauses; slow movement continues

  public void validate() {
    validateArenaMovement();
    validateFireballs();
    validateIceCover();
    validateChargeAttack();
    validateStageThreeThreshold();
    validateStageTwoContactDamage();
    validateAttackCycle();
  }

  private void validateIceCover() {
    if (iceCoverCount < 0
        || iceCoverCount > 8
        || iceCoverRespawnBatch < 1
        || iceCoverRespawnBatch > 8
        || iceCoverHits <= 0
        || !positiveFinite(iceCoverWidth)
        || !positiveFinite(iceCoverHeight)
        || !positiveFinite(iceCoverRespawnInterval)
        || !positiveFinite(iceCoverLifetime)
        || !positiveFinite(iceShatterDuration)
        || !positiveFinite(iceSpawnDuration)
        || !Float.isFinite(iceCoverGap)
        || iceCoverGap < 0f) {
      throw new IllegalArgumentException("Stage 2 ice cover values are invalid");
    }
  }

  private static boolean positiveFinite(float value) {
    return Float.isFinite(value) && value > 0f;
  }

  private void validateFireballs() {
    if (!Float.isFinite(fireballSpeed)
        || fireballSpeed <= 0f
        || fireballDamage <= 0
        || !Float.isFinite(fireVolleyInterval)
        || fireVolleyInterval <= 0f
        || !Float.isFinite(fireballLifetime)
        || fireballLifetime <= 0f
        || !Float.isFinite(fireballRadius)
        || fireballRadius <= 0f
        || !Float.isFinite(fireballInitialDelay)
        || fireballInitialDelay < 0f
        || maxFireballs < 8
        || maxFireballs > 256
        || !Float.isFinite(fireImpactDuration)
        || fireImpactDuration <= 0f) {
      throw new IllegalArgumentException("Stage 2 fireball values are invalid");
    }
  }

  private void validateArenaMovement() {
    if (!Float.isFinite(arenaMargin)
        || arenaMargin < 0f
        || !Float.isFinite(bossMoveSpeed)
        || bossMoveSpeed <= 0f
        || !Float.isFinite(bossRoamRetargetInterval)
        || bossRoamRetargetInterval <= 0f) {
      throw new IllegalArgumentException("Stage 2 arena and movement values are invalid");
    }
  }

  private void validateChargeAttack() {
    if (!Float.isFinite(bossChargeAttackDelay)
        || !Float.isFinite(bossChargeDistance)
        || bossChargeAttackDelay < 0f
        || bossChargeDistance <= 0f) {
      throw new IllegalArgumentException("Boss charge attack values are invalid");
    }
  }

  private void validateStageThreeThreshold() {
    if (!Float.isFinite(stageThreeHealthThreshold)
        || stageThreeHealthThreshold <= 0f
        || stageThreeHealthThreshold > 1f) {
      throw new IllegalArgumentException("Stage Three health threshold must be between 0 and 1");
    }
  }

  private void validateStageTwoContactDamage() {
    if (stageTwoContactDamageRadius < 0f
        || stageTwoContactDamage < 0
        || stageTwoContactDamageInterval <= 0f) {
      throw new IllegalArgumentException("Stage 2 contact damage values are invalid");
    }
  }

  private void validateAttackCycle() {
    if (!Float.isFinite(attackDuration)
        || !Float.isFinite(pauseDuration)
        || attackDuration <= 0f
        || pauseDuration <= 0f) {
      throw new IllegalArgumentException("Attack cycle durations must be positive finite values");
    }
  }
}
