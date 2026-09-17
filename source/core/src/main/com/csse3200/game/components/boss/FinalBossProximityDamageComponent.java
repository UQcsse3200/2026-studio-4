package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Applies periodic proximity damage while the first summon wave is active. */
public class FinalBossProximityDamageComponent extends Component {
  private final Entity target;
  private final float damageRadius;
  private final int damage;
  private final float damageInterval;

  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossStageOneComponent stageOneController;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent targetStats;

  private float elapsed;
  private boolean disposed;

  /**
   * Creates the first-wave proximity attack.
   *
   * @param target player who can receive proximity damage
   * @param config Stage 1 gameplay configuration
   */
  public FinalBossProximityDamageComponent(Entity target, FinalBossStageOneConfig config) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Target and config must not be null");
    }

    config.validate();

    this.target = target;
    damageRadius = config.proximityDamageRadius;
    damage = config.proximityDamage;
    damageInterval = config.proximityDamageInterval;
  }

  @Override
  public void create() {
    phaseController = requireComponent(FinalBossPhaseControllerComponent.class);
    stageOneController = requireComponent(FinalBossStageOneComponent.class);
    bossStats = requireComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats == null) {
      throw new IllegalStateException("Proximity damage target requires CombatStatsComponent");
    }
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }

    if (!canDamageTarget()) {
      elapsed = 0f;
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }

    float deltaTime = time.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    elapsed += deltaTime;

    if (elapsed >= damageInterval) {
      // Keep fractional time, but avoid a burst of catch-up hits after a long frame.
      elapsed %= damageInterval;
      targetStats.takeDamage(damage, entity);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    elapsed = 0f;
  }

  private boolean canDamageTarget() {
    if (phaseController.getCurrentPhase() != FinalBossPhase.STAGE_ONE
        || stageOneController.getState() != FinalBossStageOneState.WAVE_ONE) {
      return false;
    }

    if (bossStats.isDead() || targetStats.isDead()) {
      return false;
    }

    return entity.getCenterPosition().dst2(target.getCenterPosition())
        <= damageRadius * damageRadius;
  }

  private <T extends Component> T requireComponent(Class<T> type) {
    T component = entity.getComponent(type);

    if (component == null) {
      throw new IllegalStateException(
          "FinalBossProximityDamageComponent requires " + type.getSimpleName());
    }

    return component;
  }
}
