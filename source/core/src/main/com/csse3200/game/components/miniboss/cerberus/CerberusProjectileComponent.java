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

  public CerberusProjectileComponent(Entity target, Consumer<Entity> projectileSpawner) {
    this.target = target;
    this.projectileSpawner = projectileSpawner;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
  }

  @Override
  public void update() {
    if (disposed || stats.isDead() || targetStats == null || targetStats.isDead()) {
      return;
    }

    remaining =
        Math.max(0f, remaining - Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime()));

    if (remaining > 0f
        || entity.getCenterPosition().dst2(target.getCenterPosition())
            > ATTACK_RANGE * ATTACK_RANGE) {
      return;
    }

    remaining = COOLDOWN;

    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (disposed || stats.isDead() || targetStats.isDead()) {
                return;
              }

              Entity projectile =
                  FloatingDemonProjectileFactory.createHomingProjectile(
                      entity.getCenterPosition().sub(0.4f, 0.4f), target, stats.getBaseAttack());

              projectileSpawner.accept(projectile);
            });
  }

  @Override
  public void dispose() {
    disposed = true;
  }
}
