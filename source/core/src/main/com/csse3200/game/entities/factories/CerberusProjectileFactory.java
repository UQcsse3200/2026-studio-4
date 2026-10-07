package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.miniboss.cerberus.HomingProjectileMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

public class CerberusProjectileFactory {
  public static final String ATLAS_PATH = "images/cerberus/cerberus-fireball.atlas";

  private static final float SPEED = 5f;
  private static final float RANGE = 7f;
  private static final float LIFETIME = 1.8f;
  private static final float SIZE = 0.8f;

  private CerberusProjectileFactory() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Creates an unregistered homing fireball.
   *
   * @param position bottom-left spawn position
   * @param target entity to follow
   * @param damage contact damage
   * @return fireball for the caller to spawn
   */
  public static Entity createHomingProjectile(Vector2 position, Entity target, int damage) {
    HomingProjectileMovementComponent movement =
        new HomingProjectileMovementComponent(target, SPEED, RANGE);

    TextureAtlas atlas =
        ServiceLocator.getResourceService().getAsset(ATLAS_PATH, TextureAtlas.class);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    boolean hasFlight = animator.addAnimation("projectile", 0.1f, Animation.PlayMode.LOOP);
    boolean hasImpact = animator.addAnimation("projectileHit", 0.1f, Animation.PlayMode.NORMAL);

    if (!hasFlight || !hasImpact) {
      throw new IllegalArgumentException(
          "Cerberus fireball atlas requires projectile and projectileHit animations");
    }

    Entity projectile =
        HitboxFactory.createHitbox(
            new HitboxSpec()
                .position(position)
                .size(new Vector2(SIZE, SIZE))
                .lifetime(LIFETIME)
                .layer(PhysicsLayer.WEAPON)
                .targetLayer(PhysicsLayer.PLAYER)
                .damage(damage));

    projectile.addComponent(movement).addComponent(animator);

    // Preserve the old range-end effect. This is not a collision callback.
    projectile
        .getEvents()
        .addListener("projectileRangeReached", () -> animator.startAnimation("projectileHit"));

    animator.startAnimation("projectile");
    return projectile;
  }
}
