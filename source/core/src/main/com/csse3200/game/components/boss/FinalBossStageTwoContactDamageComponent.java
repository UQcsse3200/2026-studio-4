package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Applies periodic contact damage when the boss touches the player during Stage 2. */
public class FinalBossStageTwoContactDamageComponent extends Component {
  private final Entity target;
  private final float damageRadius;
  private final int damage;
  private final float damageInterval;

  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossStageTwoComponent stageTwoComponent;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent targetStats;

  private float elapsed;
  private boolean disposed;

  /**
   * Creates the Stage 2 contact damage attack.
   *
   * @param target player who can receive contact damage
   * @param stageTwoConfig Stage 2 gameplay configuration
   */
  public FinalBossStageTwoContactDamageComponent(
      Entity target, FinalBossStageTwoConfig stageTwoConfig) {
    if (target == null || stageTwoConfig == null) {
      throw new IllegalArgumentException("Target and config must not be null");
    }

    stageTwoConfig.validate();

    this.target = target;
    damageRadius = stageTwoConfig.stageTwoContactDamageRadius;
    damage = stageTwoConfig.stageTwoContactDamage;
    damageInterval = stageTwoConfig.stageTwoContactDamageInterval;
  }

  @Override
  public void create() {
    phaseController = requireComponent(FinalBossPhaseControllerComponent.class);
    stageTwoComponent = requireComponent(FinalBossStageTwoComponent.class);
    bossStats = requireComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats == null) {
      throw new IllegalStateException("Contact damage target requires CombatStatsComponent");
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
    if (phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return false;
    }

    if (!stageTwoComponent.isAttacking()) {
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
          "FinalBossStageTwoContactDamageComponent requires " + type.getSimpleName());
    }

    return component;
  }
}
