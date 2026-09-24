package com.csse3200.game.components.miniboss.dragon;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ThunderOrbFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Controls thunder orb firing, cooldown, and owner cleanup. */
public class DragonThunderOrbComponent extends Component {
  private static final float COOLDOWN = 2.5f;

  private final Entity target;
  private final Consumer<Entity> projectileSpawner;
  private final List<Entity> activeOrbs = new ArrayList<>();

  private CombatStatsComponent combatStats;
  private CombatStatsComponent targetStats;
  private float cooldownRemaining;
  private boolean pendingShot;
  private boolean stopped;

  public DragonThunderOrbComponent(Entity target, Consumer<Entity> projectileSpawner) {
    if (target == null || projectileSpawner == null) {
      throw new IllegalArgumentException("Target and projectile spawner are required");
    }
    this.target = target;
    this.projectileSpawner = projectileSpawner;
  }

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);

    if (combatStats == null || targetStats == null) {
      throw new IllegalStateException("Dragon and target require CombatStatsComponent");
    }

    entity.getEvents().addListener("entityDied", this::stop);
  }

  /**
   * Requests one shot. Actual creation is deferred until after entity updates.
   *
   * @return whether the attack request was accepted
   */
  public boolean tryAttack() {
    if (!canFire() || pendingShot || cooldownRemaining > 0f) {
      return false;
    }

    pendingShot = true;
    cooldownRemaining = COOLDOWN;
    ServiceLocator.getEntityService().schedule(this::fireQueuedShot);
    return true;
  }

  private boolean canFire() {
    return !stopped
        && combatStats != null
        && !combatStats.isDead()
        && targetStats != null
        && !targetStats.isDead();
  }

  private void fireQueuedShot() {
    try {
      if (!canFire()) {
        return;
      }

      Entity orb = ThunderOrbFactory.createThunderOrb(entity.getCenterPosition(), target);
      orb.addComponent(
          new Component() {
            @Override
            public void dispose() {
              activeOrbs.remove(orb);
              super.dispose();
            }
          });

      activeOrbs.add(orb);
      projectileSpawner.accept(orb);
    } finally {
      pendingShot = false;
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Advances the cooldown without automatically starting another attack. */
  public void update(float delta) {
    if (stopped || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
  }

  /** Permanently stops firing and cancels all owned thunder orbs. */
  public void stop() {
    if (stopped) {
      return;
    }

    stopped = true;
    for (Entity orb : new ArrayList<>(activeOrbs)) {
      orb.getComponent(ThunderOrbHitComponent.class).cancel();
    }
    activeOrbs.clear();
  }

  @Override
  public void dispose() {
    stop();
    super.dispose();
  }
}
