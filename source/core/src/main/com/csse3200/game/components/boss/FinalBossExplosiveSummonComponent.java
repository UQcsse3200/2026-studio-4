package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.weapons.ProjectileComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Controls an explosive summon used during Final Boss Stage 1.
 *
 * <p>The summon follows the player, begins detonating at close range, deals area damage and can be
 * detonated early by ranged attacks.
 */
public class FinalBossExplosiveSummonComponent extends Component {
  private final Entity target;
  private final float movementSpeed;
  private final float triggerDistance;
  private final float explosionRadius;
  private final int explosionDamage;
  private final float warningDuration;

  private PhysicsMovementComponent movement;
  private CombatStatsComponent stats;

  private boolean warningActive;
  private boolean detonated;
  private boolean removalReported;
  private float warningRemaining;

  public FinalBossExplosiveSummonComponent(
      Entity target,
      float movementSpeed,
      float triggerDistance,
      float explosionRadius,
      int explosionDamage,
      float warningDuration) {
    if (target == null
        || movementSpeed < 0f
        || triggerDistance < 0f
        || explosionRadius < 0f
        || explosionDamage < 0
        || warningDuration < 0f) {
      throw new IllegalArgumentException("Explosive summon values are invalid");
    }

    this.target = target;
    this.movementSpeed = movementSpeed;
    this.triggerDistance = triggerDistance;
    this.explosionRadius = explosionRadius;
    this.explosionDamage = explosionDamage;
    this.warningDuration = warningDuration;
  }

  @Override
  public void create() {
    movement = entity.getComponent(PhysicsMovementComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);

    if (movement == null || stats == null) {
      throw new IllegalStateException(
          "FinalBossExplosiveSummonComponent requires movement and combat stats");
    }

    movement.setMaxSpeed(new Vector2(movementSpeed, movementSpeed));

    entity.getEvents().addListener("entityDied", this::detonate);
    entity.getEvents().addListener("hitReaction", this::onHit);
    entity.getEvents().addListener("triggerExplosion", this::detonate);
  }

  @Override
  public void update() {
    if (detonated) {
      return;
    }

    if (!warningActive) {
      if (Invisibility.isUntargetable(target)) {
        movement.setMoving(false);
        return;
      }
      movement.setTarget(target.getPosition());
      movement.setMoving(true);
      if (entity.getCenterPosition().dst(target.getCenterPosition()) <= triggerDistance) {
        beginDetonation();
      }
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();

    warningRemaining -= Math.max(deltaTime, 0f);

    if (warningRemaining <= 0f) {
      detonate();
    }
  }

  @Override
  public void dispose() {
    reportRemoval();
  }

  /** Starts the explosion warning, or immediately detonates when the warning time is zero. */
  public void beginDetonation() {
    if (detonated || warningActive) {
      return;
    }

    if (warningDuration == 0f) {
      detonate();
      return;
    }

    warningActive = true;
    warningRemaining = warningDuration;
    movement.setMoving(false);

    entity.getEvents().trigger(FinalBossEvents.SUMMON_WARNING, warningDuration);
  }

  /** Detonates once and damages the player when they are inside the configured radius. */
  public void detonate() {
    if (detonated) {
      return;
    }

    detonated = true;
    movement.setMoving(false);

    if (entity.getCenterPosition().dst(target.getCenterPosition()) <= explosionRadius) {
      CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

      if (targetStats != null) {
        targetStats.takeDamage(explosionDamage, entity);
      }
    }

    entity.getEvents().trigger(FinalBossEvents.SUMMON_EXPLODED);
    reportRemoval();

    if (!stats.isDead()) {
      stats.setHealth(0);
    }
  }

  public boolean isWarningActive() {
    return warningActive && !detonated;
  }

  public boolean hasDetonated() {
    return detonated;
  }

  public float getMovementSpeed() {
    return movementSpeed;
  }

  public int getExplosionDamage() {
    return explosionDamage;
  }

  public float getWarningDuration() {
    return warningDuration;
  }

  private void onHit(Entity attacker) {
    if (attacker != null && attacker.getComponent(ProjectileComponent.class) != null) {
      detonate();
    }
  }

  private void reportRemoval() {
    if (removalReported) {
      return;
    }

    removalReported = true;
    entity.getEvents().trigger(FinalBossEvents.SUMMON_REMOVED, entity);
  }
}
