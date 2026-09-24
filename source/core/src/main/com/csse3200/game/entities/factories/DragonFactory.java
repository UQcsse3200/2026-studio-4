package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.miniboss.dragon.DragonCloudDashComponent;
import com.csse3200.game.components.miniboss.dragon.DragonCloudDashDamageComponent;
import com.csse3200.game.components.miniboss.dragon.DragonCloudDashMovementComponent;
import com.csse3200.game.components.miniboss.dragon.DragonCloudDashVisualComponent;
import com.csse3200.game.components.miniboss.dragon.DragonPhaseComponent;
import com.csse3200.game.components.miniboss.dragon.DragonThunderOrbComponent;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.DragonConfig;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Creates the Chinese Dragon mini-boss with health, phase logic, and animations. */
public final class DragonFactory {
  public static final String ATLAS_PATH = "images/dragon/dragon.atlas";

  private static final String IDLE = "idle";

  private DragonFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /**
   * Creates an unregistered dragon with default settings.
   *
   * @return dragon entity
   */
  public static Entity createDragon() {
    return createDragon(new DragonConfig());
  }

  /**
   * Creates an unregistered dragon with the supplied settings.
   *
   * <p>The dragon atlas must be loaded before calling this method.
   *
   * @param config combat and display settings
   * @return dragon entity
   */
  public static Entity createDragon(DragonConfig config) {
    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(ATLAS_PATH, TextureAtlas.class));

    animator.addAnimation(IDLE, 0.15f, Animation.PlayMode.LOOP);
    animator.addAnimation("moveRight", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("moveLeft", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("wave", 0.15f, Animation.PlayMode.NORMAL);
    animator.addAnimation("jump", 0.12f, Animation.PlayMode.NORMAL);
    animator.addAnimation("collapse", 0.15f, Animation.PlayMode.NORMAL);

    Entity dragon =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
            .addComponent(new DragonPhaseComponent())
            .addComponent(animator)
            .addComponent(new EnemyStatDisplay(config.healthBarScale))
            .addComponent(new EnemyDeathComponent(false));

    dragon.setScale(config.width, config.height);
    PhysicsUtils.setScaledCollider(dragon, 0.9f, 0.4f);
    animator.startAnimation(IDLE);

    return dragon;
  }

  /**
   * Creates a dragon with its thunder orb skill.
   *
   * @param target player targeted by the dragon
   * @param projectileSpawner callback that registers projectiles with the room
   * @return an unregistered dragon with its thunder orb component attached
   */
  public static Entity createDragon(Entity target, Consumer<Entity> projectileSpawner) {
    if (target == null || projectileSpawner == null) {
      throw new IllegalArgumentException("Target and projectile spawner are required");
    }

    Entity dragon = createDragon();
    dragon
        .addComponent(new DragonThunderOrbComponent(target, projectileSpawner))
        .addComponent(new DragonCloudDashComponent(target))
        .addComponent(new DragonCloudDashMovementComponent())
        .addComponent(new DragonCloudDashDamageComponent(target))
        .addComponent(new DragonCloudDashVisualComponent());
    return dragon;
  }
}
