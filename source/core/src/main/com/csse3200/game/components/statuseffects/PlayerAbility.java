package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;

/**
 * A timed status effect the player owns as an ability, carrying the parts no other effect needs: an
 * event name, a cooldown, an unlock state and its own rule for when it may start.
 *
 * <p>PlayerAbilitiesComponent drives every ability through this class alone, so a new ability is a
 * subclass and one registration rather than a change to the component.
 */
public abstract class PlayerAbility extends TimedStatusEffect {
  private final String name;
  private final long cooldown;
  private final boolean unlockedByDefault;
  private boolean unlocked;

  protected PlayerAbility(
      String name, GameTime time, long duration, long cooldown, boolean unlockedByDefault) {
    super(time, duration);
    this.name = name;
    this.cooldown = cooldown;
    this.unlockedByDefault = unlockedByDefault;
    this.unlocked = unlockedByDefault;
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
