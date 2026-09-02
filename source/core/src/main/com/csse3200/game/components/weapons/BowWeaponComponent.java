package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;

/**
 * Ranged weapon that fires an arrow in the wielder's facing direction.
 *
 * <p>The arrow is a small world-space hitbox (no owner, so it does not follow the wielder) that
 * travels in a straight line via {@link ProjectileComponent}. It despawns on its first enemy or
 * obstacle hit, or when its lifetime runs out.
 */
public class BowWeaponComponent extends WeaponComponent {
  private static final float ARROW_SIZE = 0.25f;
  private static final float ARROW_SPEED = 5f; // metres per second
  private static final float LIFETIME = 1.5f; // maximum flight time, caps range
  private static final float GAP = 0.05f; // spawn gap between wielder and arrow
  // Drawn larger than the hitbox so the blade reads at speed without widening what it hits.
  private static final float SPRITE_SIZE = 0.5f;
  // The pack draws every weapon as an icon pointing up and to the LEFT: hilt at the bottom-right,
  // tip at the top-left, a measured 135 degrees. Correcting it here keeps the pixel art crisp,
  // where rotating the PNG off-axis would resample and soften it.
  private static final float SPRITE_ANGLE_OFFSET = -135f;

  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
    Vector2 dir = direction.cpy().nor();

    // Spawn just outside the wielder so the arrow visibly starts at their edge.
    float reach = entity.getScale().len() / 2f + ARROW_SIZE / 2f + GAP;
    Vector2 spawnCenter = origin.cpy().mulAdd(dir, reach);

    // The spawn point can sit beyond a wall the wielder is touching; never fire through it.
    if (ProjectileComponent.isPathBlocked(origin, spawnCenter)) {
      return;
    }

    // HitboxSpec positions by bottom-left corner; shift by half the arrow size to centre it.
    Vector2 spawnPosition = spawnCenter.sub(ARROW_SIZE / 2f, ARROW_SIZE / 2f);

    HitboxSpec spec =
        new HitboxSpec()
            .position(spawnPosition)
            .size(new Vector2(ARROW_SIZE, ARROW_SIZE))
            .lifetime(LIFETIME)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.NPC)
            .damage(resolveHitboxDamage())
            .knockback(stats.getKnockback())
            .texture("images/weapons/throwing_knife.png")
            .visualScale(new Vector2(SPRITE_SIZE, SPRITE_SIZE))
            .rotation(dir.angleDeg())
            .rotationOffset(SPRITE_ANGLE_OFFSET);

    Entity arrow = HitboxFactory.createHitbox(spec);
    arrow.addComponent(new ProjectileComponent(dir, ARROW_SPEED));
    ServiceLocator.getEntityService().register(arrow);
  }
}
