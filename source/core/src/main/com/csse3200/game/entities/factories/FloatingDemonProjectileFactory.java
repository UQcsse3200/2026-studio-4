package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.npc.ProjectileMovementComponent;
import com.csse3200.game.components.weapons.LifetimeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Creates the visual projectiles fired by a floating demon. */
public class FloatingDemonProjectileFactory {
  private static final float PROJECTILE_SPEED = 5f;
  private static final float PROJECTILE_LIFETIME = 2f;

  public static Entity createProjectile(Vector2 position, Vector2 direction) {
    TextureAtlas atlas =
        ServiceLocator.getResourceService()
            .getAsset("images/floatingDemon.atlas", TextureAtlas.class);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("projectile", 0.1f, Animation.PlayMode.LOOP);

    Entity projectile =
        new Entity()
            .addComponent(new ProjectileMovementComponent(direction, PROJECTILE_SPEED))
            .addComponent(new LifetimeComponent(PROJECTILE_LIFETIME))
            .addComponent(animator);

    projectile.setPosition(position);
    projectile.setScale(0.5f, 0.5f);
    animator.startAnimation("projectile");
    return projectile;
  }

  private FloatingDemonProjectileFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
