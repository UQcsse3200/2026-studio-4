package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.npc.ProjectileMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Creates the projectiles fired by a floating demon. */
public class FloatingDemonProjectileFactory {
  private static final float PROJECTILE_SPEED = 5f;
  private static final float PROJECTILE_RANGE = 7f;
  private static final float PROJECTILE_LIFETIME = 1.8f;
  private static final int PROJECTILE_DAMAGE = 5;

  public static Entity createProjectile(Vector2 position, Vector2 direction) {
    TextureAtlas atlas =
        ServiceLocator.getResourceService()
            .getAsset("images/floatingDemon.atlas", TextureAtlas.class);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("projectile", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("projectileHit", 0.1f);

    HitboxSpec spec =
        new HitboxSpec()
            .position(position)
            .size(new Vector2(0.8f, 0.8f))
            .lifetime(PROJECTILE_LIFETIME)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.PLAYER)
            .damage(PROJECTILE_DAMAGE);

    Entity projectile = HitboxFactory.createHitbox(spec);
    projectile
        .addComponent(
            new ProjectileMovementComponent(direction, PROJECTILE_SPEED, PROJECTILE_RANGE))
        .addComponent(animator);

    projectile
        .getEvents()
        .addListener("projectileRangeReached", () -> animator.startAnimation("projectileHit"));
    animator.startAnimation("projectile");
    return projectile;
  }

  private FloatingDemonProjectileFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
