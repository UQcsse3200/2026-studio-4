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
  private static final float BLADE_LENGTH = 1.0f;
  private static final float BLADE_WIDTH = 0.4f;
  private static final float ARC_DEGREES = 130f;
  private static final float GAP = 0.05f;

  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);

    Vector2 dir = direction.cpy().nor();

    boolean horizontal = Math.abs(dir.x) >= Math.abs(dir.y);
    Vector2 size =
        horizontal
            ? new Vector2(BLADE_LENGTH, BLADE_WIDTH)
            : new Vector2(BLADE_WIDTH, BLADE_LENGTH);

    Vector2 playerHalfSize = entity.getScale().cpy().scl(0.5f);
    float playerHalfExtent = horizontal ? playerHalfSize.x : playerHalfSize.y;
    float hitboxHalfExtent = horizontal ? size.x / 2f : size.y / 2f;
    float reach = playerHalfExtent + hitboxHalfExtent + GAP;

    float baseAngle = dir.angleDeg();
    float startAngle = baseAngle - ARC_DEGREES / 2f;
    float endAngle = baseAngle + ARC_DEGREES / 2f;
    Vector2 offset = new Vector2(reach, 0f).setAngleDeg(startAngle);

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
    hitbox.addComponent(new SweepComponent(LIFETIME, startAngle, endAngle, reach));
    ServiceLocator.getEntityService().register(hitbox);
  }
}
