package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.ExplodeComponent;
import com.csse3200.game.components.SpiltComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.FloatingDemonAnimationController;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.PatrolTask;
import com.csse3200.game.components.tasks.RangedAttackTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.*;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create non-playable character (NPC) entities with predefined components.
 *
 * <p>Each NPC entity type should have a creation method that returns a corresponding entity.
 * Predefined entity properties can be loaded from configs stored as json files which are defined in
 * "NPCConfigs".
 *
 * <p>If needed, this factory can be separated into more specific factories for entities with
 * similar characteristics.
 */
public class NPCFactory {
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
  private static final PlayerConfig targetConfig =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  /**
   * Creates a bomb Enemy entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createBombEnemy(Entity target) {
    Entity bombEnemy = createBaseNPC();
    BombEnemyConfig config = configs.bombEnemy;
    int targetHealth = targetConfig.health;

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/bombEnemy.atlas", TextureAtlas.class));
    animator.addAnimation("move", 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("dieAnimation", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("default", 0.1f, Animation.PlayMode.LOOP);

    bombEnemy
        .addComponent(new CombatStatsComponent(config.health, targetHealth / 100 * 90))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new ExplodeComponent(target))
        .addComponent(new EnemyAnimationController());

    bombEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();

    return bombEnemy;
  }

  /**
   * Creates a chase enemy entity. Moves quickly toward the player and splits into two weaker copies
   * the first time it is hit and survives.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createChaseEnemy(Entity target) {
    Entity chaseEnemy = createBaseNPC();
    ChaseEnemyConfig config = configs.chaseEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/chaseEnemy.atlas", TextureAtlas.class));
    animator.addAnimation("move", 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);

    chaseEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyAnimationController())
        .addComponent(new SpiltComponent(target));

    chaseEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();
    chaseEnemy.getComponent(PhysicsMovementComponent.class).setMaxSpeed(new Vector2(2.5f, 2.5f));

    return chaseEnemy;
  }

  /**
   * Creates a floating demon which patrols in a straight horizontal line.
   *
   * @param leftEdge left side of its patrol area
   * @param rightEdge right side of its patrol area
   * @return floating demon entity
   */
  public static Entity createFloatingDemon(Entity target, float leftEdge, float rightEdge) {
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PatrolTask(leftEdge, rightEdge))
            .addTask(new RangedAttackTask(target));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/floatingDemon.atlas", TextureAtlas.class));
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("attack", 0.08f);

    Entity demon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(aiComponent)
            .addComponent(animator)
            .addComponent(new FloatingDemonAnimationController());

    animator.scaleEntity();
    return demon;
  }

  /**
   * Creates a generic NPC to be used as a base entity by more specific NPC creation methods.
   *
   * @return entity
   */
  public static Entity createBaseNPC() {
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new EnemyDeathComponent());

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
