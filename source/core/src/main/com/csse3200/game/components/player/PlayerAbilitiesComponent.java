package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Player spell cooldowns and passive trigger; active lifetimes belong to status effects. */
public class PlayerAbilitiesComponent extends Component {
  public static final long INVISIBILITY_DURATION_MS = 15_000;
  public static final long INVISIBILITY_COOLDOWN_MS = 45_000;
  public static final long LAST_STAND_DURATION_MS = 10_000;
  public static final long LAST_STAND_COOLDOWN_MS = 60_000;
  public static final float LAST_STAND_MULTIPLIER = 1.5f;

  /** Ability name carried by the abilityUsed, abilityEnded and abilityFailed events. */
  public static final String INVISIBILITY = "invisibility";

  /** Ability name carried by the abilityUsed and abilityEnded events. */
  public static final String LAST_STAND = "laststand";

  private static final int LAST_STAND_HEALTH_PERCENT = 20;

  private GameTime time;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private boolean lastStandEnabled;
  private boolean disposed;
  private long invisibilityReadyAt;
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
    effects = entity.getComponent(StatusEffectsControllerComponent.class);
    if (stats != null && effects == null) {
      throw new IllegalStateException("PlayerAbilities requires StatusEffectsControllerComponent");
    }
    if (effects != null) {
      effects.registerEffect(
          new Invisibility(time, INVISIBILITY_DURATION_MS, () -> onEnded(INVISIBILITY)));
      effects.registerEffect(
          new LastStand(time, LAST_STAND_DURATION_MS, () -> onEnded(LAST_STAND)));
    }
    entity.getEvents().addListener("damageTaken", this::onDamageTaken);
    entity.getEvents().addListener("entityDied", this::update);
  }

  /** Safe detection hook: null targets and targets without abilities are visible. */
  public static boolean isInvisible(Entity target) {
    PlayerAbilitiesComponent abilities =
        target == null ? null : target.getComponent(PlayerAbilitiesComponent.class);
    return abilities != null && abilities.isInvisible();
  }

  /**
   * Returns whether hostiles should ignore the target, either because there is no target or because
   * invisibility is active. Callers that pass this check may dereference the target.
   */
  public static boolean isUntargetable(Entity target) {
    return target == null || isInvisible(target);
  }

  /** Returns whether hostile damage immunity and undetectability are still active. */
  public boolean isInvisible() {
    update();
    Invisibility effect = effects == null ? null : effects.getEffect(Invisibility.class);
    return effect != null && effect.isActive();
  }

  /** Returns whether the temporary base-attack and attack-speed multiplier is active. */
  public boolean isLastStandActive() {
    update();
    LastStand effect = effects == null ? null : effects.getEffect(LastStand.class);
    return effect != null && effect.isActive();
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
      if (!disposed && entity != null && (effects == null || !effects.isDisposed())) {
        entity.getEvents().trigger("abilityFailed", INVISIBILITY, "Player is not alive");
      }
      return false;
    }
    long now = time.getTime();
    if (now < invisibilityReadyAt) {
      entity.getEvents().trigger("abilityFailed", INVISIBILITY, "Ability is on cooldown");
      return false;
    }
    effects.getEffect(Invisibility.class).activate();
    invisibilityReadyAt = now + INVISIBILITY_COOLDOWN_MS;
    entity.getEvents().trigger("abilityUsed", INVISIBILITY);
    return true;
  }

  /** Returns invisibility effect time remaining in milliseconds. */
  public long getInvisibilityRemainingMs() {
    update();
    Invisibility effect = effects == null ? null : effects.getEffect(Invisibility.class);
    return isAlive() && effect != null ? effect.getRemainingDuration() : 0;
  }

  /** Returns cooldown remaining from the cast, including the active period, in milliseconds. */
  public long getInvisibilityCooldownRemainingMs() {
    update();
    return isAlive() ? Math.max(0, invisibilityReadyAt - time.getTime()) : 0;
  }

  /** Returns Last Stand effect time remaining in milliseconds. */
  public long getLastStandRemainingMs() {
    update();
    LastStand effect = effects == null ? null : effects.getEffect(LastStand.class);
    return isAlive() && effect != null ? effect.getRemainingDuration() : 0;
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
    effects.getEffect(LastStand.class).activate();
    lastStandReadyAt = now + LAST_STAND_COOLDOWN_MS;
    entity.getEvents().trigger("abilityUsed", LAST_STAND);
  }

  private boolean isAlive() {
    return !disposed
        && stats != null
        && !stats.isDead()
        && time != null
        && effects != null
        && !effects.isDisposed();
  }

  @Override
  public void update() {
    if (!isAlive()) {
      invisibilityReadyAt = 0;
      lastStandReadyAt = 0;
      lastStandEnabled = false;
      if (effects != null) {
        effects.clearTimedEffects();
      }
    } else {
      effects.refreshTimedEffects();
    }
  }

  private void onEnded(String ability) {
    update();
    entity.getEvents().trigger("abilityEnded", ability);
  }

  @Override
  public void dispose() {
    // EventHandler has no unsubscribe API; retained callbacks become inert after disposal.
    disposed = true;
    update();
  }
}
