package com.csse3200.game.entities.configs;

/** Stage 3 tuning. Durations are seconds and distances are world units. */
public class FinalBossStageThreeConfig {
  public float waveTwoHealthThreshold = 0.2f;
  public float chargeDuration = 3f;
  public float bossSpeed = 2.4f;
  public float preferredDistance = 5f;
  public float boltSpeed = 5f;
  public float boltInterval = 1.6f;
  // Each volley has a central bolt and one bolt at either side of this angle.
  public float boltSpreadAngle = 30f;
  public int volleysBeforeTeleport = 2;
  public float volleyTeleportDelay = 0.25f;
  // Pause from the second volley until the first volley of the next cycle, including teleport.
  public float volleyRecoveryDuration = 3f;
  public float repositionDistance = 6f;
  public float boltLifetime = 4f;
  // Maximum freeze time; the teleport strike releases the player earlier on contact.
  public float freezeDuration = 3f;
  // Roam for one second after freezing the player before teleporting in to strike.
  public float teleportDelay = 1f;
  public float strikeDelay = 0.25f;
  public int strikeDamage = 10;
  public int statueCount = 5;
  public int statueHits = 10;
  public int statueEvadeHits = 3;
  public float statueEvadeDistance = 3.5f;
  public float statueMinSpacing = 3f;
  public float statueClusterSpacing = 2f;
  public float statueSpeed = 1.1f;
  public float statueMaxSpeed = 1.5f;
  public float statueStepDistance = 2f;
  public float statuePause = 2f;
  public float statueMinPause = 0.2f;
  public float statueSlamInitialDelay = 3f;
  // Encounter-wide gap between slam warnings: five statues take turns every 3s,
  // ramping to a 1.8s gap when only one remains. Warning and jump times stay unchanged.
  public float statueSlamInterval = 3f;
  public float statueSlamMinInterval = 1.8f;
  public float statueSlamWarning = 0.6f;
  public float statueJumpDuration = 0.9f;
  public float statueJumpHeight = 1.6f;
  public float shockwaveSpeed = 4.5f;
  public float shockwaveWidth = 0.24f;
  public int shockwaveDamage = 6;
  public float returnTransformDuration = 1.2f;
  public float disappearanceDuration = 0.6f;
  // Draft dialogue: replace these lines with the team's approved story text.
  public String[] dialogue = {
    "Grandpa: The storm has passed. Thank you.",
    "Player: Are you all right?",
    "Grandpa: Yes... I can finally rest."
  };

  public void validate() {
    positive(
        chargeDuration,
        bossSpeed,
        preferredDistance,
        boltSpeed,
        boltInterval,
        volleyTeleportDelay,
        volleyRecoveryDuration,
        repositionDistance,
        boltLifetime,
        freezeDuration,
        teleportDelay,
        strikeDelay,
        statueSpeed,
        statueMaxSpeed,
        statueMinSpacing,
        statueClusterSpacing,
        statueEvadeDistance,
        statueStepDistance,
        statuePause,
        statueMinPause,
        statueSlamInitialDelay,
        statueSlamInterval,
        statueSlamMinInterval,
        statueSlamWarning,
        statueJumpDuration,
        statueJumpHeight,
        shockwaveSpeed,
        shockwaveWidth,
        returnTransformDuration,
        disappearanceDuration);
    if (!Float.isFinite(waveTwoHealthThreshold)
        || waveTwoHealthThreshold <= 0f
        || waveTwoHealthThreshold >= 1f
        || !Float.isFinite(boltSpreadAngle)
        || boltSpreadAngle <= 0f
        || boltSpreadAngle >= 90f
        || volleysBeforeTeleport <= 0
        || volleyRecoveryDuration < volleyTeleportDelay
        || statueCount <= 0
        || statueHits <= 0
        || statueEvadeHits <= 0
        || strikeDamage < 0
        || shockwaveDamage < 0
        || teleportDelay + strikeDelay >= freezeDuration) {
      throw new IllegalArgumentException("Invalid Stage 3 combat configuration");
    }
    if (dialogue == null || dialogue.length == 0) {
      throw new IllegalArgumentException("Stage 3 requires ending dialogue");
    }
    if (statueSlamMinInterval > statueSlamInterval
        || statueSlamMinInterval <= statueSlamWarning + statueJumpDuration) {
      throw new IllegalArgumentException(
          "Statue slam intervals must preserve grounded recovery time");
    }
    if (statueClusterSpacing > statueMinSpacing
        || statueClusterSpacing < 2f
        || statueMaxSpeed < statueSpeed
        || statueMinPause > statuePause) {
      throw new IllegalArgumentException(
          "Statue clustering must preserve spacing and movement bounds");
    }
    for (String line : dialogue) {
      if (line == null || line.isBlank())
        throw new IllegalArgumentException("Dialogue must not be blank");
    }
  }

  private static void positive(float... values) {
    for (float value : values) {
      if (!Float.isFinite(value) || value <= 0f) {
        throw new IllegalArgumentException(
            "Stage 3 timings, speeds and distances must be finite and positive");
      }
    }
  }
}
