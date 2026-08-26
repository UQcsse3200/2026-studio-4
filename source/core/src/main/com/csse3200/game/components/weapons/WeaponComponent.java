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
 * round(wielder.baseAttack * weapon.multiplier)}; use {@link #resolveHitboxDamage()} when filling
 * {@link HitboxSpec#damage(int)}.
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

  /**
   * Caches {@link WeaponStatsComponent} from the same entity.
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
    stats.triggerCooldown();
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
   * Integer damage to copy onto a spawned hitbox: wielder {@code baseAttack} times this weapon's
   * multiplier, rounded. Missing combat stats are treated as 0 base attack.
   *
   * @return {@code round(baseAttack * multiplier)}
   */
  protected int resolveHitboxDamage() {
    CombatStatsComponent combat = entity.getComponent(CombatStatsComponent.class);
    int baseAttack = combat == null ? 0 : combat.getBaseAttack();
    return stats.resolveHitboxDamage(baseAttack);
  }
}
