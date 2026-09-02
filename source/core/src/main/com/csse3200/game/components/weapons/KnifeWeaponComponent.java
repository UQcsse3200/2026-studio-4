package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;

/**
 * Fast, short-range stab. Spawns a small hitbox just in front of the wielder that follows them for
 * its brief lifetime.
 */
public class KnifeWeaponComponent extends WeaponComponent {
  private static final float BLADE_LENGTH = 1.0f;
  private static final float BLADE_WIDTH = 0.5f;
  private static final float LIFETIME = 1.0f;
  private static final float GAP = 0.05f;

  /**
   * Spawn this weapon's hitbox. Melee implementations should pass the wielder as hitbox owner;
   * projectile splash should omit owner.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @require origin != null &amp;&amp; direction != null
   */
  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
    // Vector2 offset = direction.cpy().nor().scl(REACH);
    Vector2 dir = direction.cpy().nor();

    boolean horizontal = Math.abs(dir.x) >= Math.abs(dir.y);
    Vector2 size =
        horizontal
            ? new Vector2(BLADE_LENGTH, BLADE_WIDTH)
            : new Vector2(BLADE_WIDTH, BLADE_LENGTH);
    Vector2 cardinalDir =
        horizontal ? new Vector2(Math.signum(dir.x), 0f) : new Vector2(0f, Math.signum(dir.y));

    Vector2 wielderHalfSize = entity.getScale().cpy().scl(0.5f);
    float wielderHalfExtent = horizontal ? wielderHalfSize.x : wielderHalfSize.y;
    float hitboxHalfExtent = horizontal ? size.x / 2f : size.y / 2f;
    float reach = wielderHalfExtent + hitboxHalfExtent + GAP;

    Vector2 offset = cardinalDir.cpy().scl(reach);

    HitboxSpec spec =
        new HitboxSpec()
            .position(origin)
            .size(size)
            .lifetime(LIFETIME)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.NPC)
            .damage(resolveHitboxDamage())
            .knockback(stats.getKnockback())
            .owner(entity)
            .localOffset(offset)
                .texture("images/weapons/knife.png");

    Entity hitbox = HitboxFactory.createHitbox(spec);
    ServiceLocator.getEntityService().register(hitbox);
  }
}
