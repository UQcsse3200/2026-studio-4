package com.csse3200.game.components.abilities;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.abilities.targeting.EnemyTargetingStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreezeSpellComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(LightningSpellComponent.class);

  private final float cooldown;
  private final float freezeDuration;
  private EnemyTargetingStrategy targetingStrategy;
  private float remainingCooldown;

  public FreezeSpellComponent(
      float cooldown, float freezeDuration, EnemyTargetingStrategy targetingStrategy) {
    if (cooldown < 0f || freezeDuration < 0f) {
      logger.error(
          "Invalid FreezeSpellComponent args: cooldown={}, freezeDuration={}",
          cooldown,
          freezeDuration);
      throw new IllegalArgumentException("cooldown and freezeDuration must be >= 0");
    }
    if (targetingStrategy == null) {
      throw new IllegalArgumentException("targetingStrategy must not be null");
    }
    this.cooldown = cooldown;
    this.freezeDuration = freezeDuration;
    this.targetingStrategy = targetingStrategy;
  }

  public void setTargetingStrategy(EnemyTargetingStrategy targetingStrategy) {
    if (targetingStrategy == null) {
      throw new IllegalArgumentException("targetingStrategy must not be null");
    }
    this.targetingStrategy = targetingStrategy;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("specialAttack", this::cast);
  }

  @Override
  public void update() {
    if (remainingCooldown <= 0f) {
      return;
    }
    float dt = ServiceLocator.getTimeSource().getDeltaTime();
    remainingCooldown = Math.max(0f, remainingCooldown - Math.max(0f, dt));
  }

  public boolean canCast() {
    return remainingCooldown <= 0f;
  }

  private void cast() {
    if (!canCast()) {
      return;
    }
    remainingCooldown = cooldown;
    strike(targetingStrategy.selectTargets(entity));
  }

  private void strike(Array<Entity> targets) {
    for (Entity target : targets) {
      CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
      if (targetStats != null) {
        // Apply freeze status effect
        // new Slow(freezeDuration, targetStats, 0);
      }
    }
  }
}
