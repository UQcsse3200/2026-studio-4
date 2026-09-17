package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Only usable once the snake's own health drops below a threshold (Phase 2). Telegraphs, then spits
 * venom that creates a damaging pool on the ground where the player currently is, then cools down
 * before it can spit again.
 */
public class VenomSpitAttackTask extends DefaultTask implements PriorityTask {
  private static final int PRIORITY = 15;
  private static final float HEALTH_THRESHOLD_PERCENT = 0.5f;
  private static final float TRIGGER_RANGE = 6f;
  private static final float TELEGRAPH_DURATION = 0.6f;
  private static final float COOLDOWN_DURATION = 4f;

  private static final int POOL_DAMAGE = 6;
  private static final float POOL_LIFETIME = 3f;
  private static final Vector2 POOL_SIZE = new Vector2(1.5f, 1.5f);

  private enum Phase {
    TELEGRAPH,
    DONE
  }

  private final Entity target;
  private final GameTime gameTime;

  private Phase phase;
  private long phaseStartTime;
  private long cooldownEndTime = 0;

  public VenomSpitAttackTask(Entity target) {
    this.target = target;
    this.gameTime = ServiceLocator.getTimeSource();
  }

  @Override
  public void start() {
    super.start();
    phase = Phase.TELEGRAPH;
    phaseStartTime = gameTime.getTime();
    owner.getEntity().getEvents().trigger("venomSpitTelegraphStart");
  }

  @Override
  public void update() {
    if (phase == Phase.TELEGRAPH
        && gameTime.getTime() - phaseStartTime >= TELEGRAPH_DURATION * 1000) {
      spitVenom();
    }
  }

  @Override
  public int getPriority() {
    if (status == Status.ACTIVE) {
      return phase == Phase.DONE ? -1 : PRIORITY;
    }

    if (!isBelowHealthThreshold()) {
      return -1;
    }
    long now = gameTime.getTime();
    if (now < cooldownEndTime) {
      return -1;
    }
    if (owner.getEntity().getPosition().dst(target.getPosition()) <= TRIGGER_RANGE) {
      return PRIORITY;
    }
    return -1;
  }

  @Override
  public void setPriority(int status) {
    // Intentional empty method: this task's priority is fixed and computed internally.
  }

  private boolean isBelowHealthThreshold() {
    CombatStatsComponent stats = owner.getEntity().getComponent(CombatStatsComponent.class);
    return stats != null && stats.getHealth() <= stats.getMaxHealth() * HEALTH_THRESHOLD_PERCENT;
  }

  private void spitVenom() {
    Entity pool =
        HitboxFactory.createHitbox(
            new HitboxSpec()
                .position(target.getPosition())
                .size(POOL_SIZE)
                .lifetime(POOL_LIFETIME)
                .targetLayer(PhysicsLayer.PLAYER)
                .damage(POOL_DAMAGE));
    ServiceLocator.getEntityService().register(pool);

    cooldownEndTime = gameTime.getTime() + (long) (COOLDOWN_DURATION * 1000);
    phase = Phase.DONE;
    owner.getEntity().getEvents().trigger("venomSpitEnd");
  }
}
