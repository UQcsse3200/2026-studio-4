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
  public static final String ATTACK_STARTED = "dragonThunderOrbStarted";
  private static final float COOLDOWN = 2.5f;
  private static final float BURST_INTERVAL = 0.35f;

  private int shotsRemaining;
  private float burstRemaining;
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
    if (!canFire() || pendingShot || shotsRemaining > 0 || cooldownRemaining > 0f) {
      return false;
    }

    DragonPhaseComponent phase = entity.getComponent(DragonPhaseComponent.class);
    shotsRemaining = phase != null && phase.isEnraged() ? 2 : 1;
    cooldownRemaining = COOLDOWN;
    queueShot();
    entity.getEvents().trigger(ATTACK_STARTED);
    return true;
  }

  private void queueShot() {
    pendingShot = true;
    ServiceLocator.getEntityService().schedule(this::fireQueuedShot);
  }

  /** Includes queued shots, the second-shot delay, and surviving projectiles. */
  public boolean isBusy() {
    return pendingShot || shotsRemaining > 0 || !activeOrbs.isEmpty();
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
        shotsRemaining = 0;
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

      if (!stopped) {
        shotsRemaining--;
        burstRemaining = BURST_INTERVAL;
      }
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
    if (stopped) {
      return;
    }

    if (!canFire()) {
      stop();
      return;
    }

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    cooldownRemaining = Math.max(0f, cooldownRemaining - delta);

    if (pendingShot || shotsRemaining == 0) {
      return;
    }

    burstRemaining = Math.max(0f, burstRemaining - delta);
    if (burstRemaining == 0f) {
      queueShot();
    }
  }

  /** Permanently stops firing and cancels all owned thunder orbs. */
  public void stop() {
    if (stopped) {
      return;
    }

    stopped = true;
    shotsRemaining = 0;
    burstRemaining = 0f;
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
