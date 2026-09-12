package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hosts the player's abilities and owns only what every ability shares: cooldowns, the alive and
 * unlock gating, and the abilityUsed, abilityEnded and abilityFailed events. Active lifetimes
 * belong to the status effects controller, and everything specific to one ability belongs to its
 * PlayerAbility subclass, so adding an ability is a registration rather than a change here.
 */
public class PlayerAbilitiesComponent extends Component {
  /** Triggered with the ability name when one starts. */
  public static final String ABILITY_USED = "abilityUsed";

  /** Triggered with the ability name once one expires or is removed. */
  public static final String ABILITY_ENDED = "abilityEnded";

  /** Triggered with the ability name and a reason when a cast is refused. */
  public static final String ABILITY_FAILED = "abilityFailed";

  private final Map<Class<? extends PlayerAbility>, PlayerAbility> abilities =
      new LinkedHashMap<>();
  private final Map<Class<? extends PlayerAbility>, Long> readyAt = new LinkedHashMap<>();

  private GameTime time;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private boolean disposed;

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
      abilities.values().forEach(ability -> ability.attach(effects, () -> onEnded(ability)));
    }
    register(new Invisibility(time));
    register(new LastStand(time));
    entity.getEvents().addListener("damageTaken", this::onDamageTaken);
    entity.getEvents().addListener("entityDied", this::update);
  }

  /**
   * Adds an ability to this player, before or after create. This component owns its cooldown and
   * end event, and the status effects controller owns its active lifetime once there is one.
   */
  public void register(PlayerAbility ability) {
    abilities.put(ability.getClass(), ability);
    if (effects != null) {
      ability.attach(effects, () -> onEnded(ability));
    }
  }

  /** Returns whether the ability is running, refreshing expiry first. */
  public boolean isActive(Class<? extends PlayerAbility> type) {
    update();
    PlayerAbility ability = abilities.get(type);
    return ability != null && ability.isRunning();
  }

  /** Returns how much longer the ability runs, in milliseconds. */
  public long getRemainingMs(Class<? extends PlayerAbility> type) {
    update();
    PlayerAbility ability = abilities.get(type);
    return isAlive() && ability != null ? ability.getRemainingMs() : 0;
  }

  /** Returns the wait until the ability may start again, including its active period, in ms. */
  public long getCooldownRemainingMs(Class<? extends PlayerAbility> type) {
    update();
    return isAlive() ? Math.max(0, readyAt.getOrDefault(type, 0L) - time.getTime()) : 0;
  }

  /** Ends an ability early without touching its cooldown. */
  public void stop(Class<? extends PlayerAbility> type) {
    update();
    PlayerAbility ability = abilities.get(type);
    if (ability != null) {
      ability.stop();
    }
  }

  /** Unlocks an ability once; repeated calls never start it or reset its cooldown. */
  public void unlock(Class<? extends PlayerAbility> type) {
    PlayerAbility ability = abilities.get(type);
    if (isAlive() && ability != null) {
      ability.unlock();
    }
  }

  /** Starts a cast ability only when alive, unlocked and ready; a rejected cast changes nothing. */
  public boolean tryActivate(Class<? extends PlayerAbility> type) {
    update();
    PlayerAbility ability = abilities.get(type);
    if (ability == null || !ability.isCastable()) {
      return false;
    }
    if (!isAlive()) {
      if (!disposed && entity != null && (effects == null || !effects.isDisposed())) {
        entity.getEvents().trigger(ABILITY_FAILED, ability.getName(), "Player is not alive");
      }
      return false;
    }
    if (!ability.isUnlocked()) {
      entity.getEvents().trigger(ABILITY_FAILED, ability.getName(), "Ability is locked");
      return false;
    }
    if (time.getTime() < readyAt.getOrDefault(type, 0L)) {
      entity.getEvents().trigger(ABILITY_FAILED, ability.getName(), "Ability is on cooldown");
      return false;
    }
    start(ability);
    return true;
  }

  /** Offers the hit to every unlocked, ready ability and starts the ones that want it. */
  private void onDamageTaken(Entity attacker, int healthLost, int remainingHealth) {
    update();
    if (!isAlive()) {
      return;
    }
    long now = time.getTime();
    for (PlayerAbility ability : abilities.values()) {
      if (ability.isUnlocked()
          && now >= readyAt.getOrDefault(ability.getClass(), 0L)
          && ability.triggersOnDamage(stats, attacker, healthLost, remainingHealth)) {
        start(ability);
      }
    }
  }

  private void start(PlayerAbility ability) {
    ability.start();
    readyAt.put(ability.getClass(), time.getTime() + ability.getCooldown());
    entity.getEvents().trigger(ABILITY_USED, ability.getName());
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
    if (isAlive()) {
      effects.refreshTimedEffects();
      return;
    }
    readyAt.clear();
    for (PlayerAbility ability : abilities.values()) {
      ability.relock();
    }
    if (effects != null) {
      effects.clearTimedEffects();
    }
  }

  private void onEnded(PlayerAbility ability) {
    update();
    entity.getEvents().trigger(ABILITY_ENDED, ability.getName());
  }

  @Override
  public void dispose() {
    // EventHandler has no unsubscribe API; retained callbacks become inert after disposal.
    disposed = true;
    update();
  }
}
