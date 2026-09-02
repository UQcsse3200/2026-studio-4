package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.ExplodeComponent;
import com.csse3200.game.components.SplitComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.FloatingDemonAnimationController;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.LungeAttackTask;
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
import java.util.function.Consumer;

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

  private static final float CHASE_SPEED = 2.5f;
  private static final String DIE_ANIMATION = "dieAnimation";

  /**
   * Creates a bomb Enemy entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createBombEnemy(Entity target) {
    Entity bombEnemy = createBaseNPC();
    BombEnemyConfig config = configs.bombEnemy;

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
    animator.addAnimation(DIE_ANIMATION, 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("default", 0.1f, Animation.PlayMode.LOOP);

    bombEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack + 4))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new ExplodeComponent(target))
        .addComponent(new EnemyAnimationController());

    bombEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();

    return bombEnemy;
  }

  /**
   * Creates a chase enemy entity. Moves quickly toward the player, performs a telegraphed
   * lunge/dash attack when close enough, and splits into two weaker copies the first time it is hit
   * and survives.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createChaseEnemy(Entity target, boolean shouldSplit) {
    Entity chaseEnemy = createBaseNPC();
    ChaseEnemyConfig config = configs.chaseEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f))
            .addTask(new LungeAttackTask(target, CHASE_SPEED));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/chaseEnemy.atlas", TextureAtlas.class));
    animator.addAnimation("default", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.1f, Animation.PlayMode.NORMAL);

    chaseEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyAnimationController());

    if (shouldSplit) {
      chaseEnemy.addComponent(new SplitComponent(target));
    }

    chaseEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();
    chaseEnemy
        .getComponent(PhysicsMovementComponent.class)
        .setMaxSpeed(new Vector2(CHASE_SPEED, CHASE_SPEED));

    return chaseEnemy;
  }

  /**
   * Creates a floating demon which patrols in a straight horizontal line.
   *
   * @param leftPoint left point of its patrol path
   * @param topPoint top point of its patrol path
   * @param rightPoint right point of its patrol path
   * @return floating demon entity
   */
  public static Entity createFloatingDemon(
      Entity target, Vector2 leftPoint, Vector2 topPoint, Vector2 rightPoint) {
    return createFloatingDemon(
        target,
        leftPoint,
        topPoint,
        rightPoint,
        projectile -> ServiceLocator.getEntityService().register(projectile));
  }

  /** Creates a floating demon and delegates ownership of its projectiles to the given spawner. */
  public static Entity createFloatingDemon(
      Entity target,
      Vector2 leftPoint,
      Vector2 topPoint,
      Vector2 rightPoint,
      Consumer<Entity> projectileSpawner) {
    FloatingDemonConfig config = configs.floatingDemon;
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PatrolTask(leftPoint, topPoint, rightPoint))
            .addTask(new RangedAttackTask(target, config.baseAttack, projectileSpawner));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/floatingDemon.atlas", TextureAtlas.class));
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("attack", 0.08f);
    animator.addAnimation(DIE_ANIMATION, 0.1f);

    Entity demon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
            .addComponent(new EnemyDeathComponent(true))
            .addComponent(aiComponent)
            .addComponent(animator)
            .addComponent(new FloatingDemonAnimationController());

    animator.scaleEntity();
    demon.getComponent(PhysicsMovementComponent.class).setMaxSpeed(config.movement);
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
            .addComponent(new EnemyDeathComponent(true));

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
