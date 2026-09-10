package com.csse3200.game.components.boss;

/** Event names shared by all Final Boss stages. */
public final class FinalBossEvents {
  public static final String PHASE_CHANGED = "finalBossPhaseChanged";
  public static final String STAGE_COMPLETED = "finalBossStageCompleted";
  public static final String SHIELD_ACTIVATED = "finalBossShieldActivated";
  public static final String SHIELD_DEACTIVATED = "finalBossShieldDeactivated";
  public static final String SHIELD_HIT = "finalBossShieldHit";
  public static final String STAGE_ONE_STATE_CHANGED = "finalBossStageOneStateChanged";
  public static final String SUMMON_WARNING = "finalBossSummonWarning";
  public static final String SUMMON_EXPLODED = "finalBossSummonExploded";
  public static final String SUMMON_REMOVED = "finalBossSummonRemoved";

  private FinalBossEvents() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
