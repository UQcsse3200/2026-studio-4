package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.*;
import com.csse3200.game.components.miniboss.snake.SnakeAchievementComponent;
import com.csse3200.game.components.miniboss.snake.SnakeBurrowComponent;
import com.csse3200.game.components.miniboss.snake.SnakeBurrowVisualComponent;
import com.csse3200.game.components.miniboss.snake.SnakePlayerHitVisualComponent;
import com.csse3200.game.components.miniboss.snake.SnakePoisonVolleyComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldPickupComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.components.npc.FlyingEnemyPatrolComponent;
import com.csse3200.game.components.tasks.*;
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

  public static final String PROJECTILE_SPAWNED = "enemyProjectileSpawned";
  private static final float CHASE_SPEED = 2.5f;
  private static final String DEFAULT_ANIMATION = "default";
  private static final String DIE_ANIMATION = "dieAnimation";
  private static final String MOVE = "move";
  private static final String CHASE_ANIMATION = "chase";

  public static Entity createGiantEnemy(Entity target, String skin) {
    Entity giantEnemy = createBaseNPC();
    GiantEnemyConfig config = configs.giantEnemy;

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(MOVE, 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);

    giantEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
        .addComponent(aiComponent)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(animator)
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(2.0f));
    giantEnemy.getComponent(AnimationRenderComponent.class).scaleEntity();
    giantEnemy.setScale(3f, 3f);
    giantEnemy.getComponent(PhysicsMovementComponent.class).setMaxSpeed(new Vector2(0.5f, 0.5f));

    return giantEnemy;
  }

  /** Applies shared scale and collision settings to a giant enemy. */
  private static Entity createScaledGiant(Entity target, String skin, float size) {
    Entity enemy = createGiantEnemy(target, skin);
    enemy.setScale(size, size);
    enemy
        .getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(1f, 1f),
            new Vector2(enemy.getCenterPosition().x, enemy.getCenterPosition().y / 2f));
    PhysicsUtils.setScaledCollider(enemy, 0.3f, 0.3f);
    return enemy;
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
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(MOVE, 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("fuse", 0.1f, Animation.PlayMode.LOOP);

    bombEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack + 4))
        .addComponent(aiComponent)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(animator)
        .addComponent(new ExplodeComponent(target, fuseTime))
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(1.5f));
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
        new AITaskComponent()
            .addTask(new WanderTask(config.movement, 1f))
            .addTask(new ChaseTask(target, 10, 3f, 10f))
            .addTask(new LungeAttackTask(target, 20, CHASE_SPEED, chaseEnemy));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);

    chaseEnemy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f, 1f))
        .addComponent(aiComponent)
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(1.5f));
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
   * Creates a floating demon which patrols in a straight horizontal line.
   *
   * @param target target entity to attack
   * @param skin the atlas file path for the entity
   * @return floating demon entity
   */
  public static Entity createFloatingDemon(Entity target, Vector2 spawnPoint, String skin) {
    return createFloatingDemon(
        target,
        spawnPoint,
        skin,
        projectile -> ServiceLocator.getEntityService().register(projectile));
  }

  /**
   * Creates a floating enemy with an explicitly supplied patrol origin. Retained for existing
   * callers.
   */
  public static Entity createFloatingDemon(
      Entity target, Vector2 spawn, String skin, Consumer<Entity> projectileSpawner) {
    Entity demon = createFloatingDemonBase(skin);

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PatrolTask(spawn, true))
            .addTask(
                new RangedAttackTask(
                    target, 10, configs.floatingDemon.baseAttack, projectileSpawner));

    demon.addComponent(aiComponent);
    return demon;
  }

  /** Builds the shared appearance and components of a floating enemy. */
  private static Entity createFloatingDemonBase(String skin) {
    FloatingDemonConfig config = configs.floatingDemon;
    Entity demon = createBaseNPC();

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("attack", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation(CHASE_ANIMATION, 0.08f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);

    demon
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(1.5f));

    animator.scaleEntity();
    animator.startAnimation(MOVE);
    demon.getComponent(PhysicsMovementComponent.class).setMaxSpeed(config.movement);

    return demon;
  }

  /**
   * Creates a floating enemy with spawn-relative patrol and projectile events. The owning room must
   * listen for PROJECTILE_SPAWNED before registration.
   */
  private static Entity createFloatingEnemy(Entity target, String skin) {
    Entity enemy = createFloatingDemonBase(skin);

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(
                new RangedAttackTask(
                    target,
                    10,
                    configs.floatingDemon.baseAttack,
                    projectile -> enemy.getEvents().trigger(PROJECTILE_SPAWNED, projectile)));

    enemy.addComponent(aiComponent).addComponent(new FlyingEnemyPatrolComponent());

    return enemy;
  }

  /**
   * Creates the snake mini-boss with an invulnerable underground approach, a fixed ground warning,
   * one emergence strike and a brief vulnerable recovery. Its old contact and poison-pool attacks
   * are replaced by the burrow controller.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createSnakeMiniBoss(Entity target) {
    Entity snakeBoss = createBaseNPC();
    SnakeMiniBossConfig config = configs.snakeMiniBoss;

    // Shravika's own hand-drawn snake sprite sheet.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset("images/snake.atlas", TextureAtlas.class));
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(MOVE, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);

    snakeBoss
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new SnakeBurrowComponent(target, config))
        .addComponent(new SnakeBurrowVisualComponent())
        .addComponent(new SnakePlayerHitVisualComponent(target))
        .addComponent(new SnakeShieldComponent(target, config))
        .addComponent(new SnakeShieldPickupComponent(target, config))
        .addComponent(new SnakePoisonVolleyComponent(target, config))
        .addComponent(new SnakeAchievementComponent(target))
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent(true))
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(1.5f));

    animator.scaleEntity();
    snakeBoss.scaleWidth(2f);
    PhysicsUtils.setScaledCollider(snakeBoss, 0.9f, 0.4f);
    animator.startAnimation(DEFAULT_ANIMATION);

    snakeBoss
        .getComponent(PhysicsMovementComponent.class)
        .setMaxSpeed(new Vector2(CHASE_SPEED, CHASE_SPEED));

    return snakeBoss;
  }

  public static Entity createSleipnir(Entity target, Vector2 spawn) {
    Entity sleipnir = createBaseNPC();
    BaseEntityConfig config = configs.sleipnir;

    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PatrolTask(spawn, false))
            .addTask(new StampedeTask(target, sleipnir))
            .addTask(new EarthquakeAttackTask(target, 5f, sleipnir))
            .addTask(new OneAttackTask(target, 2f, 5, new Vector2(8f, 8f)));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset("images/horse.atlas", TextureAtlas.class));
    animator.addAnimation(MOVE, 0.7f, Animation.PlayMode.LOOP);
    animator.addAnimation("attack", 0.09f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_up", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_down", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_right", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_left", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_NE", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_NW", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_SE", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move_SW", 1f, Animation.PlayMode.LOOP);
    animator.addAnimation(CHASE_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation(DIE_ANIMATION, 0.5f, Animation.PlayMode.NORMAL);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);

    sleipnir
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(aiComponent)
        .addComponent(new EnemyDeathComponent(true, true))
        .addComponent(animator)
        .addComponent(new EnemyAnimationController())
        .addComponent(new EnemyStatDisplay(1.5f))
        .addComponent(new BossPhaseComponent());
    sleipnir.getComponent(AnimationRenderComponent.class).scaleEntity();
    animator.startAnimation(MOVE);
    sleipnir.setScale(3f, 3f);
    sleipnir
        .getComponent(HitboxComponent.class)
        .setAsBox(new Vector2(2f, 2f), sleipnir.getCenterPosition());
    PhysicsUtils.setScaledCollider(sleipnir, 0.3f, 0.3f);

    sleipnir.getComponent(PhysicsMovementComponent.class).setMaxSpeed(new Vector2(8f, 8f));

    return sleipnir;
  }

  /**
   * Creates a zombie with its standard appearance and explosion fuse.
   *
   * @param target entity to chase
   * @return zombie entity
   */
  public static Entity createZombie(Entity target) {
    return createBombEnemy(target, "images/zombie.atlas", 2f);
  }

  /** Creates a bug with its standard appearance and splitting behaviour. */
  public static Entity createBug(Entity target) {
    return createChaseEnemy(target, true, "images/bug.atlas");
  }

  /** Creates a Longwei with its standard appearance and no splitting. */
  public static Entity createLongwei(Entity target) {
    return createChaseEnemy(target, false, "images/longwei.atlas");
  }

  /** Creates Jingwei with its standard appearance. */
  public static Entity createJingwei(Entity target) {
    return createFloatingEnemy(target, "images/jingwei.atlas");
  }

  /** Creates Wukong with its standard scale and collision bounds. */
  public static Entity createWukong(Entity target) {
    return createScaledGiant(target, "images/wukong.atlas", 3f);
  }

  /**
   * Creates a wolf with its standard appearance and no splitting.
   *
   * @param target entity to chase
   * @return wolf entity
   */
  public static Entity createWolf(Entity target) {
    return createChaseEnemy(target, false, "images/wolf.atlas");
  }

  /**
   * Creates a dark elf with its standard appearance and splitting behaviour.
   *
   * @param target entity to chase
   * @return dark elf entity
   */
  public static Entity createDarkElves(Entity target) {
    return createChaseEnemy(target, true, "images/dark_elves.atlas");
  }

  /** Creates a knight with its standard appearance, fuse and scale. */
  public static Entity createKnight(Entity target) {
    Entity knight = createBombEnemy(target, "images/knight.atlas", 2f);
    knight.setScale(0.75f, 1f);
    return knight;
  }

  /** Creates Jotunn with its standard scale and collision bounds. */
  public static Entity createJotunn(Entity target) {
    return createScaledGiant(target, "images/jotunn.atlas", 3f);
  }

  /** Creates a crow with its standard appearance. */
  public static Entity createCrow(Entity target) {
    return createFloatingEnemy(target, "images/crow.atlas");
  }

  /** Creates a Sandeye with its standard appearance and no splitting. */
  public static Entity createSandeye(Entity target) {
    return createChaseEnemy(target, false, "images/sandeye.atlas");
  }

  /** Creates a beetle with its standard appearance, fuse and scale. */
  public static Entity createBeetle(Entity target) {
    Entity beetle = createBombEnemy(target, "images/beetle.atlas", 2f);
    beetle.setScale(0.75f, 0.75f);
    return beetle;
  }

  /** Creates a crab with its standard scale and hitbox. */
  public static Entity createCrab(Entity target) {
    Entity crab = createChaseEnemy(target, true, "images/crab.atlas");
    crab.setScale(1.5f, 1f);
    crab.getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(1f, 0.5f),
            new Vector2(crab.getCenterPosition().x, crab.getCenterPosition().y / 2f));
    return crab;
  }

  /** Creates a mummy with its standard hitbox and collider. */
  public static Entity createMummy(Entity target) {
    Entity mummy = createGiantEnemy(target, "images/mummy.atlas");
    mummy
        .getComponent(HitboxComponent.class)
        .setAsBox(new Vector2(1f, 1.5f), mummy.getCenterPosition());
    PhysicsUtils.setScaledCollider(mummy, 0.3f, 0.3f);
    return mummy;
  }

  /** Creates a wasp with its standard appearance. */
  public static Entity createWasp(Entity target) {
    return createFloatingEnemy(target, "images/wasp.atlas");
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
            .addComponent(new StatusEffectsControllerComponent());

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
