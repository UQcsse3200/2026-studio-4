package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Controls the summon waves and vulnerability window used during Final Boss Stage 1. */
public class FinalBossStageOneComponent extends Component {
  private final Entity target;
  private final Consumer<Entity> summonSpawner;
  private final FinalBossStageOneConfig config;
  private final Set<Entity> activeSummons = new HashSet<>();

  private FinalBossStageOneState state = FinalBossStageOneState.WAVE_ONE;
  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossDamageControllerComponent damageController;
  private FinalBossMovementComponent movementController;
  private CombatStatsComponent bossStats;

  private float breakRemaining;
  private boolean transitionSent;
  private boolean cleanupStarted;

  /** Creates the Stage 1 controller. */
  public FinalBossStageOneComponent(
      Entity target, Consumer<Entity> summonSpawner, FinalBossStageOneConfig config) {
    if (target == null || summonSpawner == null || config == null) {
      throw new IllegalArgumentException("Target, summon spawner and config must not be null");
    }

    config.validate();

    this.target = target;
    this.summonSpawner = summonSpawner;
    this.config = config;
  }

  @Override
  public void create() {
    phaseController = requireComponent(FinalBossPhaseControllerComponent.class);
    damageController = requireComponent(FinalBossDamageControllerComponent.class);
    movementController = requireComponent(FinalBossMovementComponent.class);
    bossStats = requireComponent(CombatStatsComponent.class);

    bossStats.setHealth(bossStats.getMaxHealth());
    damageController.enableShield();

    movementController.setMode(FinalBossMovementComponent.Mode.WANDER_AVOID_SUMMONS);

    changeState(FinalBossStageOneState.WAVE_ONE);

    spawnWave(config.waveOneSummonCount, config.waveOneSummonSpeed, config.waveOneWarningDuration);
  }

  @Override
  public void update() {
    if (cleanupStarted || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_ONE) {
      return;
    }

    if (state != FinalBossStageOneState.BREAK_WINDOW) {
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : Math.max(time.getDeltaTime(), 0f);

    breakRemaining -= deltaTime;

    if (breakRemaining <= 0f) {
      beginWaveTwo();
    }
  }

  @Override
  public void dispose() {
    cleanupActiveSummons();
  }

  /** Returns the current internal Stage 1 state. */
  public FinalBossStageOneState getState() {
    return state;
  }

  /** Returns the number of summons that are still active. */
  public int getActiveSummonCount() {
    return activeSummons.size();
  }

  /** Returns the remaining break-window duration. */
  public float getBreakRemaining() {
    return breakRemaining;
  }

  private void spawnWave(int count, float movementSpeed, float warningDuration) {
    activeSummons.clear();

    for (int i = 0; i < count; i++) {
      Entity summon =
          FinalBossFactory.createExplosiveSummon(target, config, movementSpeed, warningDuration);

      summon.getEvents().addListener(FinalBossEvents.SUMMON_REMOVED, this::onSummonRemoved);

      float angle = count == 0 ? 0f : (360f * i) / count;

      Vector2 offset = new Vector2(config.summonSpawnRadius, 0f).setAngleDeg(angle);

      Vector2 spawnPosition =
          entity.getCenterPosition().add(offset).sub(summon.getScale().scl(0.5f));

      summon.setPosition(spawnPosition);
      activeSummons.add(summon);
      summonSpawner.accept(summon);
    }

    movementController.setActiveSummons(activeSummons);

    if (activeSummons.isEmpty()) {
      onWaveCleared();
    }
  }

  private void onSummonRemoved(Entity summon) {
    if (cleanupStarted || !activeSummons.remove(summon)) {
      return;
    }

    movementController.setActiveSummons(activeSummons);

    if (activeSummons.isEmpty()) {
      onWaveCleared();
    }
  }

  private void onWaveCleared() {
    if (state == FinalBossStageOneState.WAVE_ONE) {
      beginBreakWindow();
    } else if (state == FinalBossStageOneState.WAVE_TWO) {
      completeStageOne();
    }
  }

  private void beginBreakWindow() {
    changeState(FinalBossStageOneState.BREAK_WINDOW);
    breakRemaining = config.breakWindowDuration;

    movementController.setMode(FinalBossMovementComponent.Mode.STOPPED);

    int healthFloor = Math.round(bossStats.getMaxHealth() * config.breakWindowHealthFloor);

    damageController.openVulnerabilityWindow(config.breakWindowDamageMultiplier, healthFloor);
  }

  private void beginWaveTwo() {
    damageController.enableShield();

    movementController.setMode(FinalBossMovementComponent.Mode.FLEE_PLAYER);

    changeState(FinalBossStageOneState.WAVE_TWO);

    spawnWave(config.waveTwoSummonCount, config.waveTwoSummonSpeed, 0f);
  }

  private void completeStageOne() {
    if (transitionSent) {
      return;
    }

    transitionSent = true;
    changeState(FinalBossStageOneState.COMPLETE);

    movementController.setActiveSummons(null);
    movementController.setMode(FinalBossMovementComponent.Mode.STOPPED);

    damageController.disableStageOneProtection();

    int stageTwoHealth = Math.round(bossStats.getMaxHealth() * config.stageTwoStartingHealth);

    bossStats.setHealth(stageTwoHealth);

    entity.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_ONE);
  }

  private void cleanupActiveSummons() {
    if (cleanupStarted) {
      return;
    }

    cleanupStarted = true;

    if (movementController != null) {
      movementController.setActiveSummons(null);
      movementController.setMode(FinalBossMovementComponent.Mode.STOPPED);
    }

    if (ServiceLocator.getEntityService() != null) {
      for (Entity summon : new ArrayList<>(activeSummons)) {
        ServiceLocator.getEntityService().scheduleDisposal(summon);
      }
    }

    activeSummons.clear();
  }

  private void changeState(FinalBossStageOneState nextState) {
    state = nextState;

    entity.getEvents().trigger(FinalBossEvents.STAGE_ONE_STATE_CHANGED, state);
  }

  private <T extends Component> T requireComponent(Class<T> type) {
    T component = entity.getComponent(type);

    if (component == null) {
      throw new IllegalStateException(
          "FinalBossStageOneComponent requires " + type.getSimpleName());
    }

    return component;
  }
}
