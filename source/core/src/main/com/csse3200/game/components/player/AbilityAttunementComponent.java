package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import java.util.Objects;

/**
 * Keeps the player attuned to at most one ability at a time.
 *
 * <p>Abilities are handed out by attuning them, never by the player simply having them: this
 * component locks every registered ability to begin with, and {@link #attune} unlocks exactly one
 * and locks the rest. {@link PlayerAbilitiesComponent} still owns cooldowns, activation and
 * lifetimes; this only decides which ability the player is allowed to use.
 *
 * <p>Lives on the player so it outlives rooms and the NPC that does the attuning.
 */
public class AbilityAttunementComponent extends Component {
  /** Triggered with the ability name whenever the attuned ability changes. */
  public static final String ABILITY_ATTUNED = "abilityAttuned";

  /** Triggered with no arguments when the player is left with no ability at all. */
  public static final String ABILITY_UNATTUNED = "abilityUnattuned";

  /** Asks for the attuned ability to be cast. Listened for here, triggered by player input. */
  public static final String USE_ATTUNED_REQUEST = "useAttunedAbility";

  private static final String NOTHING_ATTUNED = "No ability attuned";
  private static final String TRIGGERS_ITSELF = "Ability triggers on its own";

  private Class<? extends PlayerAbility> attuned;

  /** Starts the player with nothing attuned; abilities are earned, never given. */
  public AbilityAttunementComponent() {}

  @Override
  public void create() {
    entity.getEvents().addListener("entityDied", this::clearAttunement);
    entity.getEvents().addListener(USE_ATTUNED_REQUEST, this::castAttuned);
    enforceAttunement();
  }

  /**
   * Re-asserts the invariant rather than trusting that nothing else touched the locks. Two things
   * make that necessary: entity creation runs components in an unspecified order, so the abilities
   * may not have been registered yet when {@link #create()} ran, and {@link
   * PlayerAbilitiesComponent} relocks everything when the player dies, which restores any ability
   * that starts unlocked. Checking is two booleans per ability; locking only happens on drift.
   */
  @Override
  public void update() {
    enforceAttunement();
  }

  /**
   * Makes this the only ability the player may use, unlocking it and locking every other. Attuning
   * the ability that is already attuned leaves it running and changes nothing.
   *
   * @param type an ability registered on {@link PlayerAbilitiesComponent}
   * @return false if the ability is unknown, or the player cannot be given abilities right now
   */
  public boolean attune(Class<? extends PlayerAbility> type) {
    PlayerAbilitiesComponent abilities = abilities();
    if (type == null || abilities == null || !isRegistered(abilities, type)) {
      return false;
    }
    if (type.equals(attuned)) {
      return true;
    }
    // Unlocking first means a refusal (a dead player) leaves the previous attunement untouched.
    if (!abilities.unlock(type)) {
      return false;
    }
    lockAllExcept(abilities, type);
    attuned = type;
    entity.getEvents().trigger(ABILITY_ATTUNED, nameOf(abilities, type));
    return true;
  }

  /**
   * Casts whatever the player is carrying. A passive is not cast, and nothing attuned is not a
   * silent no-op: both report through the same abilityFailed event the abilities component uses, so
   * one listener can explain every refusal.
   *
   * @return whether an ability actually started
   */
  public boolean castAttuned() {
    PlayerAbilitiesComponent abilities = abilities();
    if (abilities == null) {
      return false;
    }
    if (attuned == null) {
      entity.getEvents().trigger(PlayerAbilitiesComponent.ABILITY_FAILED, "", NOTHING_ATTUNED);
      return false;
    }
    PlayerAbility ability = find(abilities, attuned);
    if (ability != null && !ability.isCastable()) {
      entity
          .getEvents()
          .trigger(PlayerAbilitiesComponent.ABILITY_FAILED, ability.getName(), TRIGGERS_ITSELF);
      return false;
    }
    return abilities.tryActivate(attuned);
  }

  /** Takes back whatever was attuned, leaving the player with no ability. */
  public void clearAttunement() {
    PlayerAbilitiesComponent abilities = abilities();
    if (abilities == null) {
      return;
    }
    boolean hadAttunement = attuned != null;
    attuned = null;
    lockAllExcept(abilities, null);
    if (hadAttunement) {
      entity.getEvents().trigger(ABILITY_UNATTUNED);
    }
  }

  /**
   * Returns the one ability the player may currently use.
   *
   * @return the attuned ability, or null when the player has none
   */
  public Class<? extends PlayerAbility> getAttuned() {
    return attuned;
  }

  /**
   * @param type an ability class
   * @return whether that is the ability the player currently carries
   */
  public boolean isAttuned(Class<? extends PlayerAbility> type) {
    return attuned != null && attuned.equals(type);
  }

  /** Locks anything the player is holding that they are not attuned to. */
  private void enforceAttunement() {
    PlayerAbilitiesComponent abilities = abilities();
    if (abilities != null) {
      lockAllExcept(abilities, attuned);
    }
  }

  private static void lockAllExcept(
      PlayerAbilitiesComponent abilities, Class<? extends PlayerAbility> keep) {
    for (PlayerAbility ability : abilities.getRegisteredAbilities().toArray(new PlayerAbility[0])) {
      if (ability.isUnlocked() && !ability.getClass().equals(keep)) {
        abilities.lock(ability.getClass());
      }
    }
  }

  private static PlayerAbility find(
      PlayerAbilitiesComponent abilities, Class<? extends PlayerAbility> type) {
    for (PlayerAbility ability : abilities.getRegisteredAbilities()) {
      if (ability.getClass().equals(type)) {
        return ability;
      }
    }
    return null;
  }

  private static boolean isRegistered(
      PlayerAbilitiesComponent abilities, Class<? extends PlayerAbility> type) {
    return find(abilities, type) != null;
  }

  private static String nameOf(
      PlayerAbilitiesComponent abilities, Class<? extends PlayerAbility> type) {
    PlayerAbility ability = find(abilities, type);
    return ability == null ? Objects.toString(type) : ability.getName();
  }

  private PlayerAbilitiesComponent abilities() {
    return entity == null ? null : entity.getComponent(PlayerAbilitiesComponent.class);
  }
}
