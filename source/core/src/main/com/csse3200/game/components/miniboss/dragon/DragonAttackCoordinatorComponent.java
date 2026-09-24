package com.csse3200.game.components.miniboss.dragon;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** Coordinates sequential attacks and enraged combinations. */
public class DragonAttackCoordinatorComponent extends Component {
  private enum Attack {
    ORB,
    DASH,
    STORM,
    COMBO
  }

  private static final float INITIAL_DELAY = 1f;
  private static final float NORMAL_RECOVERY = 1f;
  private static final float ENRAGED_RECOVERY = 0.75f;

  private final Entity target;

  private CombatStatsComponent stats;
  private CombatStatsComponent targetStats;
  private DragonPhaseComponent phase;
  private DragonThunderOrbComponent orb;
  private DragonCloudDashComponent dash;
  private DragonStormZoneComponent storm;

  private Attack next = Attack.ORB;
  private Attack active;
  private float remaining = INITIAL_DELAY;
  private boolean enragedPlan;
  private boolean comboOrbStarted;
  private boolean comboStormStarted;
  private boolean stopped;

  public DragonAttackCoordinatorComponent(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Target is required");
    }
    this.target = target;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    phase = entity.getComponent(DragonPhaseComponent.class);
    orb = entity.getComponent(DragonThunderOrbComponent.class);
    dash = entity.getComponent(DragonCloudDashComponent.class);
    storm = entity.getComponent(DragonStormZoneComponent.class);

    if (stats == null
        || targetStats == null
        || phase == null
        || orb == null
        || dash == null
        || storm == null) {
      throw new IllegalStateException(
          "Dragon coordinator requires combat stats, phase, and all three skills");
    }

    entity.getEvents().addListener("entityDied", this::stop);
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float delta) {
    if (stopped) {
      return;
    }

    if (stats.isDead() || targetStats.isDead()) {
      stop();
      return;
    }

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    if (active != null) {
      updateActiveAttack();
      return;
    }

    remaining = Math.max(0f, remaining - delta);
    if (remaining > 0f) {
      return;
    }

    if (phase.isEnraged() && !enragedPlan) {
      enragedPlan = true;
      next = Attack.COMBO;
    }

    startNextAttack();
  }

  private void startNextAttack() {
    boolean accepted;

    switch (next) {
      case ORB:
        accepted = orb.tryAttack();
        break;
      case DASH:
        accepted = dash.tryAttack();
        break;
      case STORM:
        accepted = storm.tryAttack();
        break;
      case COMBO:
        active = Attack.COMBO;
        comboOrbStarted = false;
        comboStormStarted = false;
        startComboSkills();
        return;
      default:
        throw new IllegalStateException("Unknown attack");
    }

    if (accepted) {
      active = next;
    }
  }

  private void startComboSkills() {
    if (!comboOrbStarted) {
      comboOrbStarted = orb.tryAttack();
    }
    if (!comboStormStarted) {
      comboStormStarted = storm.tryAttack();
    }
  }

  private void updateActiveAttack() {
    if (active == Attack.COMBO) {
      startComboSkills();
    }

    if (active == Attack.DASH && dash.getState() == DragonCloudDashComponent.State.STOPPED) {
      stop();
      return;
    }

    if (hasFinished()) {
      finishAttack();
    }
  }

  private boolean hasFinished() {
    return switch (active) {
      case ORB -> !orb.isBusy();
      case STORM -> !storm.isBusy();
      case DASH -> dash.getState() == DragonCloudDashComponent.State.READY;
      case COMBO -> comboOrbStarted && comboStormStarted && !orb.isBusy() && !storm.isBusy();
    };
  }

  private void finishAttack() {
    Attack finished = active;
    active = null;

    if (enragedPlan) {
      next = finished == Attack.COMBO ? Attack.DASH : Attack.COMBO;
    } else {
      next =
          switch (finished) {
            case ORB -> Attack.DASH;
            case DASH -> Attack.STORM;
            case STORM -> Attack.ORB;
            case COMBO -> Attack.DASH;
          };
    }

    remaining =
        finished == Attack.DASH ? 0f : (phase.isEnraged() ? ENRAGED_RECOVERY : NORMAL_RECOVERY);
  }

  /** Cancels the encounter's attacks and prevents further scheduling. */
  public void stop() {
    if (stopped) {
      return;
    }

    stopped = true;
    active = null;
    orb.stop();
    dash.stop();
    storm.stop();
  }

  @Override
  public void dispose() {
    stop();
    super.dispose();
  }
}
