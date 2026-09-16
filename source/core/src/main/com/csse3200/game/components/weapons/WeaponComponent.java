package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;

/**
 * Template-method weapon attached to a wielder. {@link #attack(Vector2, Vector2)} is final so
 * subclasses cannot skip cooldown. Override {@link #createAttack(Vector2, Vector2)} to spawn the
 * weapon-specific hitbox.
 *
 * <p>The same entity must also have a {@link WeaponStatsComponent}. Hitbox damage is {@code
 * round(wielder.effectiveBaseAttack * weapon.multiplier)}; use {@link #resolveHitboxDamage()} when
 * filling {@link HitboxSpec#damage(int)}.
 *
 * <p>Listens for a {@code "weaponAttack"} event carrying a {@link Vector2} direction (triggered by,
 * e.g., a player action or AI controller), and calls {@link #attack(Vector2, Vector2)} using the
 * wielder's own centre position as the attack origin. Callers should trigger {@code "weaponAttack"}
 * rather than looking this component up by its concrete subclass and calling {@code attack}
 * directly &mdash; {@code entity.getComponent} in this engine matches by exact class, so a lookup
 * by the abstract {@code WeaponComponent} type will not find a subclass instance.
 *
 * <p>Upgrades: if the wielder has a {@link WeaponUpgradeComponent} and this weapon is upgraded,
 * light hitbox damage gains the upgrade's light multiplier, and a {@code "weaponHeavyAttack"} event
 * (also carrying a {@link Vector2} direction) calls {@link #heavyAttack(Vector2, Vector2)}. Weapons
 * with a heavy attack override {@link #hasHeavyAttack()} and {@link #createHeavyAttack(Vector2,
 * Vector2)}, using {@link #resolveHeavyHitboxDamage()} for its damage. Without the upgrade the
 * weapon behaves exactly as if no upgrade component existed.
 *
 * <p>Example melee subclass:
 *
 * <pre>
 * public class KnifeWeaponComponent extends WeaponComponent {
 *   {@literal @}Override
 *   protected void createAttack(Vector2 origin, Vector2 direction) {
 *     WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
 *     Vector2 offset = direction.cpy().nor().scl(0.5f);
 *     HitboxSpec spec =
 *         new HitboxSpec()
 *             .position(origin)
 *             .size(new Vector2(0.4f, 0.8f))
 *             .lifetime(0.15f)
 *             .layer(com.csse3200.game.physics.PhysicsLayer.WEAPON)
 *             .targetLayer(com.csse3200.game.physics.PhysicsLayer.NPC)
 *             .damage(resolveHitboxDamage())
 *             .knockback(stats.getKnockback())
 *             .owner(entity)
 *             .localOffset(offset);
 *     com.csse3200.game.services.ServiceLocator.getEntityService()
 *         .register(HitboxFactory.createHitbox(spec));
 *   }
 * }
 * </pre>
 *
 * Bow splash should omit {@link HitboxSpec#owner(com.csse3200.game.entities.Entity)} so the hitbox
 * stays in world space.
 *
 * @see HitboxFactory
 * @see WeaponStatsComponent
 */
public abstract class WeaponComponent extends Component {
  private WeaponStatsComponent stats;
  // Optional: wielders without one (e.g. enemies) are simply never upgraded.
  private WeaponUpgradeComponent upgrades;

  /**
   * Caches {@link WeaponStatsComponent} (and {@link WeaponUpgradeComponent}, if present) from the
   * same entity and subscribes to the {@code "weaponAttack"} and {@code "weaponHeavyAttack"}
   * events.
   *
   * @throws IllegalStateException if the entity has no {@link WeaponStatsComponent}
   */
  @Override
  public void create() {
    stats = entity.getComponent(WeaponStatsComponent.class);
    if (stats == null) {
      throw new IllegalStateException(
          "WeaponComponent requires a WeaponStatsComponent on the same entity");
    }
    upgrades = entity.getComponent(WeaponUpgradeComponent.class);
    // listens for weaponAttack event triggered when the player presses the attack key
    entity.getEvents().addListener("weaponAttack", this::onWeaponAttack);
    // listens for weaponHeavyAttack event triggered when the player presses the heavy attack key
    entity.getEvents().addListener("weaponHeavyAttack", this::onWeaponHeavyAttack);
  }

  private void onWeaponAttack(Vector2 direction) {
    // An entity can carry several weapons; only the enabled one responds to attack input.
    if (!enabled) {
      return;
    }
    attack(entity.getCenterPosition(), direction);
  }

  private void onWeaponHeavyAttack(Vector2 direction) {
    if (!enabled) {
      return;
    }
    heavyAttack(entity.getCenterPosition(), direction);
  }

  /**
   * Attempt an attack. When ready, delegates to {@link #createAttack(Vector2, Vector2)} then starts
   * cooldown.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @return true if {@code createAttack} ran; false if still cooling down
   * @require origin != null &amp;&amp; direction != null
   * @throws IllegalArgumentException if origin or direction is null
   */
  public final boolean attack(Vector2 origin, Vector2 direction) {
    if (origin == null || direction == null) {
      throw new IllegalArgumentException("origin and direction must not be null");
    }
    if (!stats.canAttack()) {
      return false;
    }
    createAttack(origin, direction);
    stats.triggerCooldown(resolveCooldown());
    return true;
  }

  /**
   * Spawn this weapon's hitbox. Melee implementations should pass the wielder as hitbox owner;
   * projectile splash should omit owner.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @require origin != null &amp;&amp; direction != null
   */
  protected abstract void createAttack(Vector2 origin, Vector2 direction);

  /**
   * Attempt a heavy attack. Only upgraded weapons that have one can heavy attack. When ready,
   * delegates to {@link #createHeavyAttack(Vector2, Vector2)} then starts the shared cooldown,
   * lengthened by the upgrade's heavy cooldown multiplier and never shorter than {@link
   * #getHeavyAttackDuration()}.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @return true if {@code createHeavyAttack} ran; false if the weapon has no heavy attack, is not
   *     upgraded, or is still cooling down
   * @require origin != null &amp;&amp; direction != null
   * @throws IllegalArgumentException if origin or direction is null
   */
  public final boolean heavyAttack(Vector2 origin, Vector2 direction) {
    if (origin == null || direction == null) {
      throw new IllegalArgumentException("origin and direction must not be null");
    }
    if (!hasHeavyAttack() || !isUpgraded() || !stats.canAttack()) {
      return false;
    }
    createHeavyAttack(origin, direction);
    float cooldown = resolveCooldown() * upgrades.getHeavyCooldownMultiplier(getClass());
    // Never let attack-speed buffs start a new heavy attack while the last one is still active.
    stats.triggerCooldown(Math.max(cooldown, getHeavyAttackDuration()));
    return true;
  }

  /**
   * @return seconds a heavy attack stays active; its cooldown is never shorter than this. 0 by
   *     default
   */
  protected float getHeavyAttackDuration() {
    return 0f;
  }

  /**
   * @return true if this weapon has a heavy attack; false by default
   */
  protected boolean hasHeavyAttack() {
    return false;
  }

  /**
   * Spawn this weapon's heavy attack hitbox. Only called for upgraded weapons whose {@link
   * #hasHeavyAttack()} is true. Does nothing by default.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @require origin != null &amp;&amp; direction != null
   */
  protected void createHeavyAttack(Vector2 origin, Vector2 direction) {
    // No heavy attack unless a subclass provides one.
  }

  /**
   * @return true if the wielder has a {@link WeaponUpgradeComponent} with this weapon upgraded
   */
  protected boolean isUpgraded() {
    return upgrades != null && upgrades.isUpgraded(getClass());
  }

  /**
   * Damage for this weapon's spawned hitboxes: the wielder's effective base attack scaled by the
   * weapon multiplier and, once upgraded, the upgrade's light damage multiplier, rounded. A wielder
   * without combat stats is treated as 0 base attack.
   *
   * @return {@code round(wielder.effectiveBaseAttack * multiplier * lightUpgradeMultiplier)}
   */
  protected int resolveHitboxDamage() {
    float scale = upgrades == null ? 1f : upgrades.getLightDamageMultiplier(getClass());
    return stats.resolveHitboxDamage(resolveBaseAttack(), scale);
  }

  /**
   * Damage for this weapon's heavy attack hitboxes: like {@link #resolveHitboxDamage()} but scaled
   * by the upgrade's heavy damage multiplier instead.
   *
   * @return {@code round(wielder.effectiveBaseAttack * multiplier * heavyUpgradeMultiplier)}
   */
  protected int resolveHeavyHitboxDamage() {
    return resolveHeavyHitboxDamage(1f);
  }

  /**
   * Heavy attack damage with an extra scale, for heavy attacks made of several hits that deal
   * different damage (e.g. a combo finisher).
   *
   * @param scale extra damage scale on top of the heavy upgrade multiplier; 1 for none
   * @return {@code round(wielder.baseAttack * multiplier * heavyUpgradeMultiplier * scale)}
   */
  protected int resolveHeavyHitboxDamage(float scale) {
    float heavy = upgrades == null ? 1f : upgrades.getHeavyDamageMultiplier(getClass());
    return stats.resolveHitboxDamage(resolveBaseAttack(), heavy * scale);
  }

  private int resolveBaseAttack() {
    CombatStatsComponent combat = entity.getComponent(CombatStatsComponent.class);
    return combat == null ? 0 : combat.getEffectiveBaseAttack();
  }

  /**
   * Cooldown for this weapon scaled by the wielder's effective attack speed, so buffs make every
   * weapon fire faster. A wielder without combat stats uses the weapon's base cooldown.
   *
   * @return {@code weapon.cooldown / wielder.effectiveAttackSpeed}
   */
  protected float resolveCooldown() {
    CombatStatsComponent combat = entity.getComponent(CombatStatsComponent.class);
    float attackSpeed = combat == null ? 1f : combat.getEffectiveAttackSpeed();
    return stats.resolveCooldown(attackSpeed);
  }
}
