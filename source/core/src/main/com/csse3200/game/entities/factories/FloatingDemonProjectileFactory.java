package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
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
  private static final String DEFAULT_ATLAS = "images/floatingDemon.atlas";

  public static Entity createProjectile(Vector2 position, Vector2 direction, int damage) {
    return createProjectile(position, direction, damage, DEFAULT_ATLAS);
  }

  /**
   * Creates a projectile whose look matches the firing enemy's own atlas, so a themed flying
   * enemy (e.g. harpy, wasp, crow, jingwei) fires a projectile in its own palette instead of
   * always using the generic floating demon's.
   *
   * @param atlasPath the atlas to pull the "projectile"/"projectileHit" animation regions from;
   *     falls back to the floating demon's own atlas if the enemy's atlas has no such regions
   */
  public static Entity createProjectile(
          Vector2 position, Vector2 direction, int damage, String atlasPath) {
    return createProjectileWithMovement(
            position,
            damage,
            new ProjectileMovementComponent(direction, PROJECTILE_SPEED, PROJECTILE_RANGE),
            atlasPath);
  }

  private static Entity createProjectileWithMovement(
          Vector2 position, int damage, Component movement, String atlasPath) {
    AnimationRenderComponent animator = new AnimationRenderComponent(resolveAtlas(atlasPath));
    animator.addAnimation("projectile", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("projectileHit", 0.1f);

    HitboxSpec spec =
            new HitboxSpec()
                    .position(position)
                    .size(new Vector2(0.8f, 0.8f))
                    .lifetime(PROJECTILE_LIFETIME)
                    .layer(PhysicsLayer.WEAPON)
                    .targetLayer(PhysicsLayer.PLAYER)
                    .damage(damage);

    Entity projectile = HitboxFactory.createHitbox(spec);
    projectile.addComponent(movement).addComponent(animator);

    projectile
            .getEvents()
            .addListener("projectileRangeReached", () -> animator.startAnimation("projectileHit"));
    animator.startAnimation("projectile");
    return projectile;
  }

  /**
   * Loads the given atlas and falls back to the floating demon's own atlas if it doesn't define
   * a "projectile" region, so enemies whose atlas has no themed projectile art still render one.
   */
  private static TextureAtlas resolveAtlas(String atlasPath) {
    TextureAtlas atlas =
            ServiceLocator.getResourceService().getAsset(atlasPath, TextureAtlas.class);
    if (atlas.findRegion("projectile") != null) {
      return atlas;
    }
    return ServiceLocator.getResourceService().getAsset(DEFAULT_ATLAS, TextureAtlas.class);
  }

  private FloatingDemonProjectileFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}

