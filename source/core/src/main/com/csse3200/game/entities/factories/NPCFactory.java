package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.*;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.CoilAttackTask;
import com.csse3200.game.components.tasks.LungeAttackTask;
import com.csse3200.game.components.tasks.PatrolTask;
import com.csse3200.game.components.tasks.RangedAttackTask;
import com.csse3200.game.components.tasks.VenomSpitAttackTask;
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
  private static final String DEFAULT_ANIMATION = "default";
  private static final String DIE_ANIMATION = "dieAnimation";
  private static final String MOVE = "move";
  private static final String CHASE_ANIMATION = "chase";

  public static Entity createGiantEnemy(Entity target, String skin) {
    Entity giantEnemy = createBaseNPC();
    GiantEnemyConfig config = configs.giantEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent(target)
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(MOVE, 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    // Longer frame duration so the (currently single-frame) death pose is actually
    // visible before the entity is removed, instead of disappearing in one-tenth of a
    // second.
    animator.addAnimation(DIE_ANIMATION, 1.2f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);

    giantEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(animator)
        .addComponent(new EnemyAnimationController());
    giantEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();
    giantEnemy.setScale(3f, 3f);
    giantEnemy.getComponent(PhysicsMovementComponent.class).setMaxSpeed(new Vector2(0.5f, 0.5f));

    return giantEnemy;
  }

  /**
   * Creates a bomb Enemy entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createBombEnemy(Entity target, String skin, float fuseTime) {
    Entity bombEnemy = createBaseNPC();
    BombEnemyConfig config = configs.bombEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent(target)
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(MOVE, 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    // Longer frame duration so the (currently single-frame) death pose is actually
    // visible before the entity is removed, instead of disappearing in one-tenth of a
    // second.
    animator.addAnimation(DIE_ANIMATION, 1.2f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("fuse", 0.1f, Animation.PlayMode.LOOP);

    bombEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack + 4))
        .addComponent(aiComponent)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(animator)
        .addComponent(new ExplodeComponent(target, fuseTime))
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
   * @param shouldSplit whether the enemy should receive a {@link SplitComponent}
   * @return entity
   */
  public static Entity createChaseEnemy(Entity target, boolean shouldSplit, String skin) {
    Entity chaseEnemy = createBaseNPC();
    ChaseEnemyConfig config = configs.chaseEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent(target)
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f))
            .addTask(new LungeAttackTask(target, 20, CHASE_SPEED, chaseEnemy));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    // Longer frame duration so the (currently single-frame) death pose is actually
    // visible before the entity is removed, instead of disappearing in one-tenth of a
    // second.
    animator.addAnimation(DIE_ANIMATION, 1.2f, Animation.PlayMode.NORMAL);

    chaseEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(new EnemyAnimationController());
    if (shouldSplit) {
      chaseEnemy.addComponent(new SplitComponent(target, skin));
    }

    animator.scaleEntity();
    animator.startAnimation(DEFAULT_ANIMATION);

    chaseEnemy
        .getComponent(PhysicsComponent.class)
        .getBody()
        .setLinearVelocity(CHASE_SPEED, CHASE_SPEED);

    return chaseEnemy;
  }

  /**
   * Creates the snake mini-boss entity. Moves toward the player and, once close enough, performs a
   * telegraphed coil attack that poisons the player over time. Once its own health drops below 50%,
   * it also gains a ranged venom-spit attack that creates a damaging pool on the ground.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createSnakeMiniBoss(Entity target) {
    Entity snakeBoss = createBaseNPC();
    SnakeMiniBossConfig config = configs.snakeMiniBoss;

    AITaskComponent aiComponent =
        new AITaskComponent(target)
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f))
            .addTask(new CoilAttackTask(target, CHASE_SPEED))
            .addTask(new VenomSpitAttackTask(target));

    // Shravika's own hand-drawn snake sprite sheet.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset("images/snake.atlas", TextureAtlas.class));
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    // Longer frame duration so the (currently single-frame) death pose is actually
    // visible before the entity is removed, instead of disappearing in one-tenth of a
    // second.
    animator.addAnimation(DIE_ANIMATION, 1.2f, Animation.PlayMode.NORMAL);

    snakeBoss
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true))
        .addComponent(new EnemyAnimationController());

    animator.scaleEntity();
    animator.startAnimation(DEFAULT_ANIMATION);

    snakeBoss
        .getComponent(PhysicsMovementComponent.class)
        .setMaxSpeed(new Vector2(CHASE_SPEED, CHASE_SPEED));

    return snakeBoss;
  }

  /**
   * Creates a floating demon which patrols in a straight horizontal line.
   *
   * @param target target entity to attack
   * @param leftPoint left point of its patrol path
   * @param topPoint top point of its patrol path
   * @param rightPoint right point of its patrol path
   * @param skin the atlas file path for the entity
   * @return floating demon entity
   */
  public static Entity createFloatingDemon(
      Entity target, Vector2 leftPoint, Vector2 topPoint, Vector2 rightPoint, String skin) {
    return createFloatingDemon(
        target,
        leftPoint,
        topPoint,
        rightPoint,
        projectile -> ServiceLocator.getEntityService().register(projectile),
        skin);
  }

  /**
   * Creates a floating demon and delegates ownership of its projectiles to the given spawner.
   *
   * @param leftPoint left point of its patrol path
   * @param topPoint top point of its patrol path
   * @param rightPoint right point of its patrol path
   * @return floating demon entity
   */
  public static Entity createFloatingDemon(
      Entity target,
      Vector2 leftPoint,
      Vector2 topPoint,
      Vector2 rightPoint,
      Consumer<Entity> projectileSpawner,
      String skin) {

    FloatingDemonConfig config = configs.floatingDemon;

    AITaskComponent aiComponent =
        new AITaskComponent(target)
            .addTask(new PatrolTask(leftPoint, topPoint, rightPoint, 1))
            .addTask(new RangedAttackTask(target, 5, config.baseAttack, projectileSpawner));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("attack", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation(CHASE_ANIMATION, 0.08f, Animation.PlayMode.LOOP);
    // Longer frame duration so the (currently single-frame) death pose is actually
    // visible before the entity is removed, instead of disappearing in one-tenth of a
    // second.
    animator.addAnimation(DIE_ANIMATION, 1.2f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);

    Entity demon = createBaseNPC();

    demon
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(new EnemyAnimationController());

    animator.scaleEntity();
    animator.startAnimation("move");

    demon.getComponent(PhysicsMovementComponent.class).setMaxSpeed(config.movement);

    return demon;
  }

  /**
   * Creates a generic NPC to be used as a base entity by more specific NPC creation methods.
   *
   * @return entity
   */
  protected static Entity createBaseNPC() {
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new StatusEffectsControllerComponent());

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
