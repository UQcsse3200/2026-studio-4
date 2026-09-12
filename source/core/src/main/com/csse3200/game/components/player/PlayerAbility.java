package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;

/**
 * Something the player can use: it has a name, a cooldown, an unlock state and its own rule for
 * when it may start. PlayerAbilitiesComponent drives every ability through this class alone, so a
 * new ability is a subclass and one registration rather than a change to the component.
 *
 * <p>An ability is not a status effect. An ability that puts a timed condition on the player
 * extends {@link TimedPlayerAbility}, which owns one; an instant or toggled ability extends this
 * directly and never touches the status effects system at all.
 */
public abstract class PlayerAbility {
  private final String name;
  private final long cooldown;
  private final boolean unlockedByDefault;
  private boolean unlocked;
  private Entity owner;

  protected PlayerAbility(String name, long cooldown, boolean unlockedByDefault) {
    this.name = name;
    this.cooldown = cooldown;
    this.unlockedByDefault = unlockedByDefault;
    this.unlocked = unlockedByDefault;
  }

  /**
   * Returns the player this ability belongs to, which is set when it is registered. An ability that
   * acts on the world, such as one that puts an effect on nearby enemies, starts from here.
   */
  public Entity getOwner() {
    return owner;
  }

  /** Set by PlayerAbilitiesComponent when the ability is registered. */
  final void setOwner(Entity owner) {
    this.owner = owner;
  }

  /** Returns the name carried by the abilityUsed, abilityEnded and abilityFailed events. */
  public String getName() {
    return name;
  }

  /** Returns the wait before the ability may start again, measured from the start, in ms. */
  public long getCooldown() {
    return cooldown;
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  /** Unlocks an ability that starts locked; an already unlocked ability is unaffected. */
  public void unlock() {
    unlocked = true;
  }

  /** Restores the starting unlock state, so death does not carry an unlock into the next life. */
  public void relock() {
    unlocked = unlockedByDefault;
  }

  /** Returns whether the player starts this ability directly. Passives return false. */
  public boolean isCastable() {
    return false;
  }

  /**
   * Returns whether incoming damage should start this ability. Passives override this; cast
   * abilities never start themselves.
   *
   * @param stats the combat stats of the player taking the damage
   * @param attacker the entity that dealt the damage, which may be null
   * @param healthLost health removed by this hit
   * @param remainingHealth health left after this hit
   */
  public boolean triggersOnDamage(
      CombatStatsComponent stats, Entity attacker, int healthLost, int remainingHealth) {
    return false;
  }

  /**
   * Hands over the player's status effects controller, along with the callback to run when this
   * ability ends. An ability that puts no timed condition on the player ignores both; it can still
   * reach any other entity's controller through getOwner.
   */
  protected void attach(StatusEffectsControllerComponent effects, Runnable onEnded) {
    // Nothing to hand over by default.
  }

  /** Starts the ability. The component has already checked alive, unlock and cooldown. */
  public abstract void start();

  /** Ends the ability early. Doing this to an ability that is not running changes nothing. */
  public abstract void stop();

  /** Returns whether the ability is still doing something. */
  public abstract boolean isRunning();

  /** Returns how much longer it runs in ms, which is zero for an ability that finishes at once. */
  public abstract long getRemainingMs();

  /**
   * Returns whether the ability is running on an entity, refreshing expiry first so the answer does
   * not depend on where in the frame it is asked. Missing entities and entities without abilities
   * are never running one.
   */
  protected static boolean isRunningOn(Entity entity, Class<? extends PlayerAbility> type) {
    PlayerAbilitiesComponent abilities =
        entity == null ? null : entity.getComponent(PlayerAbilitiesComponent.class);
    return abilities != null && abilities.isActive(type);
  }
}
