package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;

/**
 * Template-method weapon attached to a wielder. {@link #attack(Vector2, Vector2)} is final so
 * subclasses cannot skip cooldown. Override {@link #createAttack(Vector2, Vector2)} to spawn the
 * weapon-specific hitbox.
 *
 * <p>The same entity must also have a {@link WeaponStatsComponent}.
 *
 * <p>Listens for a {@code "weaponAttack"} event carrying a {@link Vector2} direction (triggered by,
 * e.g., a player action or AI controller), and calls {@link #attack(Vector2, Vector2)} using the
 * wielder's own centre position as the attack origin. Callers should trigger {@code "weaponAttack"}
 * rather than looking this component up by its concrete subclass and calling {@code attack}
 * directly &mdash; {@code entity.getComponent} in this engine matches by exact class, so a lookup
 * by the abstract {@code WeaponComponent} type will not find a subclass instance.
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
 *             .damage(stats.getDamage())
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
   * Caches {@link WeaponStatsComponent} from the same entity and subscribes to the {@code
   * "weaponAttack"} event.
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
    // listens for weaponAttack event triggered when the player presses the attack key
    entity.getEvents().addListener("weaponAttack", this::onWeaponAttack);
  }

  private void onWeaponAttack(Vector2 direction) {
    attack(entity.getCenterPosition(), direction);
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
}
