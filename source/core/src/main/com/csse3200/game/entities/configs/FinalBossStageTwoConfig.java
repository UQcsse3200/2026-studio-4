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

  // Nearby protective ice cover. Zero count disables cover for isolated encounter tests.
  public int iceCoverCount = 4;
  public float iceCoverWidth = 0.9f;
  public float iceCoverHeight = 1.8f;
  public float iceCoverRespawnInterval = 3.5f;
  public int iceCoverRespawnBatch = 1;
  public float iceCoverLifetime = 7f;
  public int iceCoverHits = 4;
  public float iceCoverGap = 1.25f;
  public float iceCoverNearMinDistance = 2.5f;
  public float iceCoverNearMaxDistance = 4.5f;
  public float iceShatterDuration = 0.55f;
  public float iceSpawnDuration = 0.6f;

  // Ground pickups are separate from solid cover and supply up to two ice-energy reserves.
  public int icePickupCount = 2;
  public float icePickupSpawnInterval = 4f;
  public float icePickupLifetime = 7f;
  public float icePickupRadius = 0.4f;
  public float icePickupGap = 0.75f;
  public float icePickupEffectDuration = 0.5f;

  // Player ice magic: one reserve powers a stream of shots, spent only when a shot is emitted.
  public float iceProjectileSpeed = 6f;
  public float iceProjectileDamage = 0.25f;
  public float iceFireInterval = 0.18f;
  public float iceProjectileLifetime = 4f;
  public float iceProjectileRadius = 0.12f;
  public float iceHomingTurnRate = 240f;
  public int iceShotsPerCharge = 8;
  public int maxIceProjectiles = 64;
  public float iceImpactDuration = 0.35f;
  public float iceBuffEndDuration = 0.5f;

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
    validateIcePickups();
    validatePlayerIce();
    validateChargeAttack();
    validateStageThreeThreshold();
    validateStageTwoContactDamage();
    validateAttackCycle();
  }

  private void validatePlayerIce() {
    if (!positiveFinite(iceProjectileSpeed)
        || !positiveFinite(iceProjectileDamage)
        || !positiveFinite(iceFireInterval)
        || !positiveFinite(iceProjectileLifetime)
        || !positiveFinite(iceProjectileRadius)
        || !positiveFinite(iceHomingTurnRate)
        || iceShotsPerCharge < 1
        || iceShotsPerCharge > 128
        || maxIceProjectiles < 1
        || maxIceProjectiles > 256
        || !positiveFinite(iceImpactDuration)
        || !positiveFinite(iceBuffEndDuration)) {
      throw new IllegalArgumentException("Stage 2 player ice values are invalid");
    }
  }

  private void validateIcePickups() {
    if (icePickupCount < 0
        || icePickupCount > 8
        || !positiveFinite(icePickupSpawnInterval)
        || !positiveFinite(icePickupLifetime)
        || !positiveFinite(icePickupRadius)
        || !positiveFinite(icePickupEffectDuration)
        || !Float.isFinite(icePickupGap)
        || icePickupGap < 0f) {
      throw new IllegalArgumentException("Stage 2 ice pickup values are invalid");
    }
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
        || !positiveFinite(iceCoverNearMinDistance)
        || !positiveFinite(iceCoverNearMaxDistance)
        || iceCoverNearMaxDistance < iceCoverNearMinDistance
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
