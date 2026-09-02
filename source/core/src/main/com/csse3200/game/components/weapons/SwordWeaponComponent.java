package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;

/**
 * Slower sweeping attack. Spawns a rectangular sword hitbox that moves along a predetermined arc of
 * attack using SweepComponent.
 */
public class SwordWeaponComponent extends WeaponComponent {
  /** Sprite drawn for the sword's sweeping attack. Loaded by {@link WeaponAssetsComponent}. */
  public static final String TEXTURE = "images/weapons/sword.png";

  // weapon attributes
  private static final float LIFETIME = 0.5f; // How long the sweep attack takes
  private static final float BLADE_LENGTH = 1.0f; // the length of the hitbox when sweeping
  private static final float BLADE_WIDTH = 0.4f; // the width of the hitbox when sweeping
  private static final float ARC_DEGREES = 90f; // the arc of the sweep attack
  private static final float GAP = 0.05f; // hitbox min distance from the player
  // Drawn square so the blade keeps its shape; the hitbox stays long and thin for collision.
  private static final float SPRITE_SIZE = 1.0f;
  // The hitbox rides the arc at full reach, so drawing there would put the blade's midpoint on the
  // arc and leave the handle floating. Pull it back along the swing so the handle sits at the
  // wielder and the blade points outward through the sweep.
  private static final float SPRITE_PULL_IN = -0.45f;
  // The pack draws every weapon as an icon pointing up and to the LEFT: hilt at the bottom-right,
  // tip at the top-left, a measured 135 degrees. Correcting it here keeps the pixel art crisp,
  // where rotating the PNG off-axis would resample and soften it.
  private static final float SPRITE_ANGLE_OFFSET = -135f;

  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);

    Vector2 dir = direction.cpy().nor();

    // Flip the axis of the hitbox based on the attack direction
    boolean horizontal = Math.abs(dir.x) >= Math.abs(dir.y);
    Vector2 size =
        horizontal
            ? new Vector2(BLADE_LENGTH, BLADE_WIDTH)
            : new Vector2(BLADE_WIDTH, BLADE_LENGTH);

    // Ensure the offset keeps the attack centred with the player
    Vector2 playerHalfSize = entity.getScale().cpy().scl(0.5f);
    float playerHalfExtent = horizontal ? playerHalfSize.x : playerHalfSize.y;
    float hitboxHalfExtent = horizontal ? size.x / 2f : size.y / 2f;
    float reach = playerHalfExtent + hitboxHalfExtent + GAP;

    // get start and end of attack radius for sweep component constructor
    float baseAngle = dir.angleDeg();
    float startAngle = baseAngle - ARC_DEGREES / 2f;
    float endAngle = baseAngle + ARC_DEGREES / 2f;

    Vector2 offset = new Vector2(reach, 0f).setAngleDeg(startAngle);

    // construct the hitbox spec
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
            .texture(TEXTURE)
            .visualScale(new Vector2(SPRITE_SIZE, SPRITE_SIZE))
            .visualOffset(new Vector2(SPRITE_PULL_IN, 0f))
            .rotation(startAngle)
            .rotationOffset(SPRITE_ANGLE_OFFSET);

    Entity hitbox = HitboxFactory.createHitbox(spec);
    hitbox.addComponent(new SweepComponent(LIFETIME, startAngle, endAngle, reach));
    ServiceLocator.getEntityService().register(hitbox);
  }
}
