package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Fires homing projectiles while the right head is alive. */
public class CerberusProjectileComponent extends Component {
  private static final float COOLDOWN = 2.5f;
  private static final float ATTACK_RANGE = 7f;

  private final Entity target;
  private final Consumer<Entity> projectileSpawner;

  private CombatStatsComponent stats;
  private CombatStatsComponent targetStats;
  private float remaining = COOLDOWN;
  private boolean disposed;
  private boolean pendingShot;
  private CerberusAttackCoordinator attackCoordinator;

  public CerberusProjectileComponent(Entity target, Consumer<Entity> projectileSpawner) {
    this.target = target;
    this.projectileSpawner = projectileSpawner;
  }

  /** Connects this skill after it has been added to its head entity. */
  public void setAttackCoordinator(CerberusAttackCoordinator coordinator) {
    attackCoordinator = coordinator;
    coordinator.register(entity, this::canStartAttack);
  }

  private boolean canStartAttack() {
    return !disposed
        && !pendingShot
        && stats != null
        && !stats.isDead()
        && targetStats != null
        && !targetStats.isDead()
        && remaining <= 0f
        && entity.getCenterPosition().dst2(target.getCenterPosition())
            <= ATTACK_RANGE * ATTACK_RANGE;
  }

  private void finishAttack() {
    if (attackCoordinator != null) {
      attackCoordinator.finish(entity);
    }
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("entityDied", this::finishAttack);
  }

  @Override
  public void update() {
    if (disposed || stats.isDead() || targetStats == null || targetStats.isDead()) {
      finishAttack();
      return;
    }

    if (pendingShot) {
      return;
    }

    remaining =
        Math.max(0f, remaining - Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime()));

    if (!canStartAttack()) {
      return;
    }

    if (attackCoordinator != null && !attackCoordinator.tryStart(entity)) {
      return;
    }

    remaining = COOLDOWN;
    pendingShot = true;
    ServiceLocator.getEntityService().schedule(this::fireQueuedShot);
  }

  private void fireQueuedShot() {
    try {
      if (disposed || stats.isDead() || targetStats == null || targetStats.isDead()) {
        return;
      }

      Entity projectile =
          FloatingDemonProjectileFactory.createHomingProjectile(
              entity.getCenterPosition().sub(0.4f, 0.4f), target, stats.getBaseAttack());

      projectileSpawner.accept(projectile);
    } finally {
      pendingShot = false;
      finishAttack();
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    finishAttack();
    super.dispose();
  }
}
