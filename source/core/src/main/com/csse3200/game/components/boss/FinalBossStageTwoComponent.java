package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Controls Stage 2's single encounter, slow roaming, firing rhythm and Stage 3 health floor. */
public class FinalBossStageTwoComponent extends Component {
  private static final float ARRIVAL_DISTANCE = 0.15f;
  private static final int DESTINATION_ATTEMPTS = 4;

  private final FinalBossStageTwoConfig stageTwoConfig;
  private FinalBossPhaseControllerComponent phaseController;
  private CombatStatsComponent bossStats;
  private FinalBossMovementComponent movementComponent;
  private PhysicsMovementComponent physicsMovement;
  private FinalBossStageTwoArenaComponent arena;
  private boolean transitionSent;
  private boolean encounterStarted;
  private boolean disposed;
  private boolean isAttacking;
  private float cycleTimer;
  private float retargetRemaining;
  private Vector2 roamDestination;

  public FinalBossStageTwoComponent(FinalBossStageTwoConfig stageTwoConfig) {
    if (stageTwoConfig == null) {
      throw new IllegalArgumentException("Configs must not be null");
    }
    stageTwoConfig.validate();
    this.stageTwoConfig = stageTwoConfig;
  }

  @Override
  public void create() {
    phaseController = entity.getComponent(FinalBossPhaseControllerComponent.class);
    bossStats = entity.getComponent(CombatStatsComponent.class);
    movementComponent = entity.getComponent(FinalBossMovementComponent.class);
    physicsMovement = entity.getComponent(PhysicsMovementComponent.class);
    arena = entity.getComponent(FinalBossStageTwoArenaComponent.class);
    if (phaseController == null || bossStats == null || movementComponent == null) {
      throw new IllegalStateException(
          "FinalBossStageTwoComponent requires FinalBossPhaseControllerComponent, "
              + "CombatStatsComponent, and FinalBossMovementComponent");
    }
    entity.getEvents().addListener("updateHealth", this::onBossHealthChanged);
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::onPhaseChanged);
  }

  /** Begins one continuous encounter only after the phase-transition protection ends. */
  public void startEncounter() {
    if (disposed || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    applyStageThreeHealthFloor();
    encounterStarted = true;
    transitionSent = false;
    isAttacking = true;
    cycleTimer = 0f;
    retargetRemaining = 0f;
    roamDestination = null;
    movementComponent.disableChargeAttacks();
    movementComponent.setMode(FinalBossMovementComponent.Mode.STOPPED);
  }

  @Override
  public void update() {
    if (disposed || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    if (!encounterStarted || phaseController.isTransitioning() || bossStats.isDead()) {
      stopRoaming();
      return;
    }
    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      stopRoaming();
      return;
    }
    // Retain overshoot so the three-second pause does not depend on frame boundaries.
    double cycleDuration = (double) stageTwoConfig.attackDuration + stageTwoConfig.pauseDuration;
    cycleTimer = (float) ((cycleTimer + (double) deltaTime) % cycleDuration);
    isAttacking = cycleTimer < stageTwoConfig.attackDuration;
    updateRoaming(deltaTime);
  }

  private void updateRoaming(float deltaTime) {
    Rectangle allowed = arena == null ? null : arena.getMovementBounds(entity);
    if (physicsMovement == null || allowed == null) {
      stopRoaming();
      return;
    }
    PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
    if (physics != null) {
      physics.earlyUpdate();
    }
    Vector2 position = entity.getPosition();
    retargetRemaining -= deltaTime;
    if (roamDestination != null) {
      roamDestination.set(
          MathUtils.clamp(roamDestination.x, allowed.x, allowed.x + allowed.width),
          MathUtils.clamp(roamDestination.y, allowed.y, allowed.y + allowed.height));
    }
    if (roamDestination == null
        || retargetRemaining <= 0f
        || position.dst2(roamDestination) <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
      roamDestination = chooseDestination(allowed, position);
      retargetRemaining = stageTwoConfig.bossRoamRetargetInterval;
    }
    float distance = position.dst(roamDestination);
    if (distance <= ARRIVAL_DISTANCE) {
      stopRoaming();
      return;
    }
    // PhysicsMovementComponent normalises direction; reduce speed close to a destination.
    float speed = Math.min(stageTwoConfig.bossMoveSpeed, distance / deltaTime);
    physicsMovement.setMaxSpeed(new Vector2(speed, speed));
    physicsMovement.setTarget(roamDestination.cpy());
    physicsMovement.setMoving(true);
  }

  private Vector2 chooseDestination(Rectangle allowed, Vector2 position) {
    Vector2 candidate = new Vector2();
    for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
      candidate.set(
          MathUtils.random(allowed.x, allowed.x + allowed.width),
          MathUtils.random(allowed.y, allowed.y + allowed.height));
      if (position.dst2(candidate) > ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
        return candidate;
      }
    }
    // A tiny viewport can leave no useful random choice; try the opposite corner safely.
    return candidate.set(
        position.x < allowed.x + allowed.width / 2f ? allowed.x + allowed.width : allowed.x,
        position.y < allowed.y + allowed.height / 2f ? allowed.y + allowed.height : allowed.y);
  }

  private void stopRoaming() {
    if (physicsMovement != null) {
      physicsMovement.setMoving(false);
    }
  }

  private void onPhaseChanged(FinalBossPhase phase) {
    encounterStarted = false;
    isAttacking = false;
    cycleTimer = 0f;
    roamDestination = null;
    retargetRemaining = 0f;
    if (phase == FinalBossPhase.STAGE_TWO) {
      transitionSent = false;
    }
  }

  /** Prevents a powerful hit from skipping the Stage 3 encounter entirely. */
  public void applyStageThreeHealthFloor() {
    bossStats.setMinimumHealth(
        Math.max(1, Math.round(bossStats.getMaxHealth() * stageTwoConfig.stageThreeHealthThreshold)));
  }

  /** Returns whether the active encounter is in its firing interval rather than its pause. */
  public boolean isAttacking() {
    return encounterStarted
        && !disposed
        && phaseController.getCurrentPhase() == FinalBossPhase.STAGE_TWO
        && !phaseController.isTransitioning()
        && !bossStats.isDead()
        && isAttacking;
  }

  private void onBossHealthChanged(Integer health) {
    if (transitionSent
        || health <= 0
        || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    int healthThreshold =
        Math.round(bossStats.getMaxHealth() * stageTwoConfig.stageThreeHealthThreshold);
    if (health <= healthThreshold) {
      transitionSent = true;
      movementComponent.disableChargeAttacks();
      entity.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_TWO);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    encounterStarted = false;
    roamDestination = null;
    // Entity disposal may already have destroyed its physics body; do not steer it here.
  }
}
