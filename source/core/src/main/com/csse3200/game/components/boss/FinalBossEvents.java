package com.csse3200.game.components.boss;

/** Event names shared by all Final Boss stages. */
public final class FinalBossEvents {
  public static final String PHASE_CHANGED = "finalBossPhaseChanged";
  public static final String STAGE_COMPLETED = "finalBossStageCompleted";
  public static final String SHIELD_ACTIVATED = "finalBossShieldActivated";
  public static final String SHIELD_DEACTIVATED = "finalBossShieldDeactivated";
  public static final String SHIELD_HIT = "finalBossShieldHit";
  public static final String STAGE_ONE_STATE_CHANGED = "finalBossStageOneStateChanged";

  /**
   * Stage 2 player ice projectile reached the boss before an obstacle. Argument: firing player
   * (Entity). Emitted before damage, which may accumulate fractionally or start Stage 3.
   */
  public static final String STAGE_TWO_ICE_HIT = "finalBossStageTwoIceHit";

  /** First active Stage 2 start, after transformation. Argument: participating player (Entity). */
  public static final String STAGE_TWO_STARTED = "finalBossStageTwoStarted";

  /** Stage 2 fireball contact before damage or protection checks. Argument: hit player (Entity). */
  public static final String STAGE_TWO_FIRE_HIT = "finalBossStageTwoFireHit";

  /**
   * Active Stage 2 reached its health floor, immediately before the Stage 3 handover. Argument:
   * participating player (Entity). Forced phase changes do not emit this event.
   */
  public static final String STAGE_TWO_COMPLETED = "finalBossStageTwoCompleted";

  /**
   * Stage 3 Wave 1 ice-bolt contact. Argument: the player hit (Entity), regardless of health loss.
   */
  public static final String STAGE_THREE_ICE_HIT = "finalBossStageThreeIceHit";

  /**
   * Player sword/knife contact with the unshielded Stage 3 Wave 1 boss. Argument: attacking player
   * (Entity). Emitted before damage can end Wave 1.
   */
  public static final String STAGE_THREE_MELEE_HIT = "finalBossStageThreeMeleeHit";

  public static final String SUMMON_WARNING = "finalBossSummonWarning";
  public static final String SUMMON_EXPLODED = "finalBossSummonExploded";
  public static final String SUMMON_REMOVED = "finalBossSummonRemoved";
  public static final String PETRIFICATION_WARNING = "finalBossPetrificationWarning";

  public static final String PETRIFICATION_HIT = "finalBossPetrificationHit";

  public static final String PETRIFICATION_MISSED = "finalBossPetrificationMissed";

  /**
   * Player-side integration event for the Status Effects team.
   *
   * <p>Arguments: movement multiplier (Float), duration in seconds (Float).
   */
  public static final String PETRIFICATION_EFFECT_REQUESTED =
      "finalBossPetrificationEffectRequested";

  /** Player-side integration event requesting removal of any active petrification effect. */
  public static final String PETRIFICATION_EFFECT_CLEAR_REQUESTED =
      "finalBossPetrificationEffectClearRequested";

  private FinalBossEvents() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
