package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;

/**
 * Slower sweeping attack. Spawns a short range radiant hitbox just in front of the wielder that
 * follows them for its brief lifetime.
 */
public class SwordWeaponComponent extends WeaponComponent {
  private static final Vector2 SIZE = new Vector2(1.0f, 1.5f);
  private static final float LIFETIME = 1.0f;
  private static final float REACH = 0.5f;

  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
      WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
      Vector2 offset = direction.cpy().nor().scl(REACH);

      HitboxSpec spec =
          new HitboxSpec()
              .position(origin)
              .size(SIZE)
              .lifetime(LIFETIME)
              .layer(PhysicsLayer.WEAPON)
              .targetLayer(PhysicsLayer.NPC)
              .damage(stats.getDamage())
              .knockback(stats.getKnockback())
              .owner(entity)
              .localOffset(offset);

      Entity hitbox = HitboxFactory.createHitbox(spec);
      ServiceLocator.getEntityService().register(hitbox);
  }
}
