package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Player spell and passive deadlines, independent of frame ordering and raw combat stats. */
public class PlayerAbilitiesComponent extends Component {
  public static final long INVISIBILITY_DURATION_MS = 15_000;
  public static final long INVISIBILITY_COOLDOWN_MS = 45_000;
  public static final long LAST_STAND_DURATION_MS = 10_000;
  public static final long LAST_STAND_COOLDOWN_MS = 60_000;
  public static final float LAST_STAND_MULTIPLIER = 1.5f;
  private static final int LAST_STAND_HEALTH_PERCENT = 20;

  private GameTime time;
  private CombatStatsComponent stats;
  private boolean lastStandEnabled;
  private boolean disposed;
  private long invisibleUntil;
  private long invisibilityReadyAt;
  private long lastStandUntil;
  private long lastStandReadyAt;

  /** Uses the registered game clock when the entity is created. */
  public PlayerAbilitiesComponent() {}

  /** Uses an injected clock for all effect and cooldown deadlines (milliseconds). */
  public PlayerAbilitiesComponent(GameTime time) {
    this.time = time;
  }

  @Override
  public void create() {
    if (time == null) {
      time = ServiceLocator.getTimeSource();
    }
    stats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("damageTaken", this::onDamageTaken);
    entity.getEvents().addListener("entityDied", this::update);
  }

  /** Safe detection hook: null targets and targets without abilities are visible. */
  public static boolean isInvisible(Entity target) {
    PlayerAbilitiesComponent abilities =
        target == null ? null : target.getComponent(PlayerAbilitiesComponent.class);
    return abilities != null && abilities.isInvisible();
  }

  /** Returns whether hostile damage immunity and undetectability are still active. */
  public boolean isInvisible() {
    update();
    return invisibleUntil != 0;
  }

  /** Returns whether the temporary base-attack and attack-speed multiplier is active. */
  public boolean isLastStandActive() {
    update();
    return lastStandUntil != 0;
  }

  /** Enables the passive once; repeated calls never trigger it or reset its cooldown. */
  public void enableLastStand() {
    if (isAlive()) {
      lastStandEnabled = true;
    }
  }

  /** Casts only when alive and ready; a rejected cast never changes either deadline. */
  public boolean tryInvisibility() {
    update();
    if (!isAlive()) {
      if (!disposed && entity != null) {
        entity.getEvents().trigger("abilityFailed", "invisibility", "Player is not alive");
      }
      return false;
    }
    long now = time.getTime();
    if (now < invisibilityReadyAt) {
      entity.getEvents().trigger("abilityFailed", "invisibility", "Ability is on cooldown");
      return false;
    }
    invisibleUntil = now + INVISIBILITY_DURATION_MS;
    invisibilityReadyAt = now + INVISIBILITY_COOLDOWN_MS;
    entity.getEvents().trigger("abilityUsed", "invisibility");
    return true;
  }

  /** Returns invisibility effect time remaining in milliseconds. */
  public long getInvisibilityRemainingMs() {
    update();
    return isAlive() ? Math.max(0, invisibleUntil - time.getTime()) : 0;
  }

  /** Returns cooldown remaining from the cast, including the active period, in milliseconds. */
  public long getInvisibilityCooldownRemainingMs() {
    update();
    return isAlive() ? Math.max(0, invisibilityReadyAt - time.getTime()) : 0;
  }

  /** Returns Last Stand effect time remaining in milliseconds. */
  public long getLastStandRemainingMs() {
    update();
    return isAlive() ? Math.max(0, lastStandUntil - time.getTime()) : 0;
  }

  /** Returns cooldown remaining from the passive trigger in milliseconds. */
  public long getLastStandCooldownRemainingMs() {
    update();
    return isAlive() ? Math.max(0, lastStandReadyAt - time.getTime()) : 0;
  }

  private void onDamageTaken(Entity attacker, int healthLost, int remainingHealth) {
    update();
    if (!isAlive()
        || !lastStandEnabled
        || healthLost <= 0
        || remainingHealth <= 0
        || (long) remainingHealth * 100 >= (long) stats.getMaxHealth() * LAST_STAND_HEALTH_PERCENT
        || !CombatStatsComponent.isHostileAttacker(attacker)) {
      return;
    }
    long now = time.getTime();
    if (now < lastStandReadyAt) {
      return;
    }
    lastStandUntil = now + LAST_STAND_DURATION_MS;
    lastStandReadyAt = now + LAST_STAND_COOLDOWN_MS;
    entity.getEvents().trigger("abilityUsed", "laststand");
  }

  private boolean isAlive() {
    return !disposed && stats != null && !stats.isDead() && time != null;
  }

  @Override
  public void update() {
    boolean alive = isAlive();
    long now = alive ? time.getTime() : 0;
    boolean endInvisibility = invisibleUntil != 0 && (!alive || now >= invisibleUntil);
    boolean endLastStand = lastStandUntil != 0 && (!alive || now >= lastStandUntil);
    // Clear state before notifying observers, which may query abilities again.
    if (endInvisibility) {
      invisibleUntil = 0;
    }
    if (endLastStand) {
      lastStandUntil = 0;
    }
    if (!alive) {
      invisibilityReadyAt = 0;
      lastStandReadyAt = 0;
      lastStandEnabled = false;
    }
    if (endInvisibility) {
      entity.getEvents().trigger("abilityEnded", "invisibility");
    }
    if (endLastStand) {
      entity.getEvents().trigger("abilityEnded", "laststand");
    }
  }

  @Override
  public void dispose() {
    // EventHandler has no unsubscribe API; retained callbacks become inert after disposal.
    disposed = true;
    update();
  }
}
