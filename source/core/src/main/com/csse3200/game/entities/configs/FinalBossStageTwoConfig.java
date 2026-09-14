package com.csse3200.game.entities.configs;

public class FinalBossStageTwoConfig {
  // Stage 2 charge attacks
  public float bossChargeAttackDelay = 0.5f;
  public float bossChargeDistance = 6f;
  public float stageThreeHealthThreshold = 0.4f;

  // Stage 2 contact damage
  public float stageTwoContactDamageRadius = 1f;
  public int stageTwoContactDamage = 1;
  public float stageTwoContactDamageInterval = 0.05f;

  // Stage 2 attack/pause cycle
  public float attackDuration = 20f; // Boss attacks for 20 seconds
  public float pauseDuration = 5f; // Boss stands still for 7 seconds

  public void validate() {
    validateChargeAttack();
    validateStageThreeThreshold();
    validateStageTwoContactDamage();
    validateAttackCycle();
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
