package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks which of a wielder's weapons are upgraded and the stats each upgrade grants.
 *
 * <p>Upgrade bonuses are applied when a weapon resolves its hitbox damage and cooldown, never by
 * editing the shared {@link WeaponStatsComponent}, so upgrading one weapon cannot buff the others.
 * An upgraded weapon also unlocks its heavy attack (see {@link WeaponComponent#heavyAttack}).
 *
 * <p>Grant or revoke an upgrade with {@link #setUpgraded(Class, boolean)}; the {@code upgrade}
 * terminal command and any future pickup or shop should all go through that method.
 */
public class WeaponUpgradeComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(WeaponUpgradeComponent.class);

  /** Sword: +20% light damage, +35% heavy damage, heavy attack doubles the cooldown. */
  static final WeaponUpgradeStats SWORD_UPGRADE = new WeaponUpgradeStats(1.2f, 1.35f, 2f);

  /**
   * Knife: +20% light damage; each flurry slash deals 0.6x (the finisher doubles it), and the
   * flurry triples the cooldown.
   */
  static final WeaponUpgradeStats KNIFE_UPGRADE = new WeaponUpgradeStats(1.2f, 0.6f, 3f);

  /**
   * Bow: no damage or cooldown change. The splitting-arrow upgrade instead changes the light
   * attack itself &mdash; one attack fires three arrows instead of one, each at full damage &mdash;
   * so no heavy attack or multiplier is needed here. See {@link BowWeaponComponent}.
   */
  static final WeaponUpgradeStats BOW_UPGRADE = new WeaponUpgradeStats(1f, 1f, 1f);

  private final Map<Class<? extends WeaponComponent>, WeaponUpgradeStats> stats;
  private final Set<Class<? extends WeaponComponent>> upgraded = new HashSet<>();

  /** Create with the default upgrade stats for every weapon that has an upgrade. */
  public WeaponUpgradeComponent() {
    this(
        Map.of(
            SwordWeaponComponent.class, SWORD_UPGRADE,
            KnifeWeaponComponent.class, KNIFE_UPGRADE,
            BowWeaponComponent.class, BOW_UPGRADE));
  }

  /**
   * @param stats upgrade stats keyed by weapon class; weapons missing from the map cannot be
   *     upgraded
   * @throws IllegalArgumentException if stats is null
   */
  public WeaponUpgradeComponent(Map<Class<? extends WeaponComponent>, WeaponUpgradeStats> stats) {
    if (stats == null) {
      throw new IllegalArgumentException("stats must not be null");
    }
    this.stats = new HashMap<>(stats);
  }

  /**
   * Apply or revert a weapon's upgrade. Idempotent: setting the current state again changes
   * nothing. Triggers {@code "weaponUpgraded"} with the weapon class when the state changes.
   *
   * @param weapon weapon class to upgrade
   * @param on true to apply the upgrade, false to revert to the unupgraded weapon
   * @return false if the weapon has no upgrade stats, true otherwise
   */
  public boolean setUpgraded(Class<? extends WeaponComponent> weapon, boolean on) {
    if (!stats.containsKey(weapon)) {
      logger.debug("No upgrade defined for weapon {}", weapon);
      return false;
    }
    boolean changed = on ? upgraded.add(weapon) : upgraded.remove(weapon);
    if (changed && entity != null) {
      entity.getEvents().trigger("weaponUpgraded", weapon);
    }
    return true;
  }

  /**
   * @param weapon weapon class to check
   * @return true if the weapon's upgrade is currently applied
   */
  public boolean isUpgraded(Class<? extends WeaponComponent> weapon) {
    return upgraded.contains(weapon);
  }

  /**
   * @param weapon weapon class
   * @return light attack damage multiplier, or 1 if the weapon is not upgraded
   */
  public float getLightDamageMultiplier(Class<? extends WeaponComponent> weapon) {
    return isUpgraded(weapon) ? stats.get(weapon).lightDamageMultiplier() : 1f;
  }

  /**
   * @param weapon weapon class
   * @return heavy attack damage multiplier, or 1 if the weapon is not upgraded
   */
  public float getHeavyDamageMultiplier(Class<? extends WeaponComponent> weapon) {
    return isUpgraded(weapon) ? stats.get(weapon).heavyDamageMultiplier() : 1f;
  }

  /**
   * @param weapon weapon class
   * @return heavy attack cooldown multiplier, or 1 if the weapon is not upgraded
   */
  public float getHeavyCooldownMultiplier(Class<? extends WeaponComponent> weapon) {
    return isUpgraded(weapon) ? stats.get(weapon).heavyCooldownMultiplier() : 1f;
  }
}
