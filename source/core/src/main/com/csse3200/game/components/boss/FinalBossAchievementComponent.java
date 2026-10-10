package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;

/** Reports Final Boss challenge results to the shared achievement system. */
public class FinalBossAchievementComponent extends Component {
  private final Entity target;
  private final BreakAttempt attempt = new BreakAttempt();
  private FinalBossStageOneComponent stageOne;
  private FinalBossStageThreeComponent stageThree;
  private FinalBossPhaseControllerComponent phases;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private boolean snowQueenReported;
  private boolean disposed;

  /** Creates an achievement observer for the player participating in this boss encounter. */
  public FinalBossAchievementComponent(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Final Boss achievement target must not be null");
    }
    this.target = target;
  }

  @Override
  public void create() {
    stageOne = entity.getComponent(FinalBossStageOneComponent.class);
    stageThree = entity.getComponent(FinalBossStageThreeComponent.class);
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    bossStats = entity.getComponent(CombatStatsComponent.class);
    playerStats = target.getComponent(CombatStatsComponent.class);
    if (stageOne == null || phases == null || bossStats == null) {
      throw new IllegalStateException(
          "FinalBossAchievementComponent requires Stage 1, phase and combat stats components");
    }

    entity.getEvents().addListener(FinalBossEvents.STAGE_ONE_STATE_CHANGED, this::onStateChanged);
    entity.getEvents().addListener("damageAttempted", this::onDamageAttempted);
    entity.getEvents().addListener(FinalBossEvents.STAGE_THREE_ICE_HIT, this::onStageThreeIceHit);
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::onPhaseChanged);
    entity.getEvents().addListener("entityDied", attempt::finish);
    // EventHandler has no removal API. Retain only the small attempt state on the player, not the
    // boss.
    target.getEvents().addListener("entityDied", attempt::finish);
  }

  private void onStateChanged(FinalBossStageOneState state) {
    if (attempt.finished) {
      return;
    }
    if (!isEncounterActive()) {
      attempt.finish();
      return;
    }
    if (state != stageOne.getState()) {
      return;
    }
    if (state == FinalBossStageOneState.BREAK_WINDOW) {
      attempt.windowObserved = true;
      return;
    }
    if (!attempt.windowObserved) {
      return;
    }

    // Consume the attempt before notifying listeners, which can re-enter or dispose this entity.
    attempt.finish();
    if (state == FinalBossStageOneState.SUMMONING_TWO
        && stageOne.getBreakRemaining() <= 0f
        && !attempt.attacked) {
      AchievementService achievements = ServiceLocator.getAchievementService();
      if (achievements != null) {
        AchievementContext context = new AchievementContext();
        context.finalBossBreakRespected = true;
        achievements.update(context);
      }
    }
  }

  private void onDamageAttempted(int damage, Entity source) {
    // Check the controller's state: another state listener can deal damage before ours runs.
    // The source may be a weapon, projectile or null (for example a spell or damage-over-time
    // tick).
    if (!attempt.finished
        && damage > 0
        && stageOne.getState() == FinalBossStageOneState.BREAK_WINDOW) {
      attempt.attacked = true;
    }
  }

  private void onPhaseChanged(FinalBossPhase phase) {
    if (phase != FinalBossPhase.STAGE_ONE) {
      attempt.finish();
    }
  }

  private void onStageThreeIceHit(Entity player) {
    if (disposed
        || snowQueenReported
        || player != target
        || playerStats == null
        || playerStats.isDead()
        || bossStats.isDead()
        || phases.getCurrentPhase() != FinalBossPhase.STAGE_THREE
        || phases.isTransitioning()
        || stageThree == null
        || stageThree.getState() != FinalBossStageThreeState.WAVE_ONE) {
      return;
    }

    // Stage 1's break attempt has already ended; this achievement has its own one-shot result.
    snowQueenReported = true;
    AchievementService achievements = ServiceLocator.getAchievementService();
    if (achievements != null) {
      AchievementContext context = new AchievementContext();
      context.finalBossStageThreeIceHit = true;
      achievements.update(context);
    }
  }

  private boolean isEncounterActive() {
    return phases.getCurrentPhase() == FinalBossPhase.STAGE_ONE
        && !phases.isTransitioning()
        && !bossStats.isDead()
        && playerStats != null
        && !playerStats.isDead();
  }

  @Override
  public void dispose() {
    disposed = true;
    attempt.finish();
  }

  /** Encounter-local flags; keeping this static avoids retaining a disposed boss via the player. */
  private static class BreakAttempt {
    private boolean windowObserved;
    private boolean attacked;
    private boolean finished;

    private void finish() {
      finished = true;
    }
  }
}
