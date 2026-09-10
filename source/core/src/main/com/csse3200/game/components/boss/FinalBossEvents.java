package com.csse3200.game.components.boss;

/** Event names shared by all Final Boss stages. */
public final class FinalBossEvents {
  public static final String PHASE_CHANGED = "finalBossPhaseChanged";
  public static final String STAGE_COMPLETED = "finalBossStageCompleted";

  private FinalBossEvents() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
