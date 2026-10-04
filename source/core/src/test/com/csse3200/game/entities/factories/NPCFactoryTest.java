package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.miniboss.snake.SnakeBurrowComponent;
import com.csse3200.game.components.miniboss.snake.SnakeBurrowVisualComponent;
import com.csse3200.game.components.miniboss.snake.SnakePlayerHitVisualComponent;
import com.csse3200.game.components.miniboss.snake.SnakePoisonAssets;
import com.csse3200.game.components.miniboss.snake.SnakePoisonVolleyComponent;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NPCFactoryTest {

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(
        new String[] {
          "images/beetle.atlas",
          "images/crab.atlas",
          "images/cyclops.atlas",
          "images/floatingDemon.atlas",
          "images/golem.atlas",
          "images/medusa.atlas",
          "images/mummy.atlas",
          "images/snake.atlas"
        });
    resourceService.loadTextures(new String[] {SnakePlayerHitVisualComponent.HIT_SHEET});
    resourceService.loadTextures(SnakePoisonAssets.paths());
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void giantEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createGiantEnemy(new Entity(), "images/mummy.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void bombEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createBombEnemy(new Entity(), "images/beetle.atlas", 2f);
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void chaseEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createChaseEnemy(new Entity(), true, "images/crab.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void snakeMiniBossHasHealthBar() {
    Entity enemy = NPCFactory.createSnakeMiniBoss(new Entity());
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void snakeMiniBossUsesBurrowControllerInsteadOfOldAttackTasks() {
    Entity enemy = NPCFactory.createSnakeMiniBoss(new Entity());

    assertNotNull(enemy.getComponent(SnakeBurrowComponent.class));
    assertNotNull(enemy.getComponent(SnakePlayerHitVisualComponent.class));
    assertNotNull(enemy.getComponent(SnakePoisonVolleyComponent.class));
    assertNull(enemy.getComponent(AITaskComponent.class));
  }

  @Test
  void snakeMiniBossHasNoAutomaticContactDamage() {
    Entity enemy = NPCFactory.createSnakeMiniBoss(new Entity());

    assertNull(enemy.getComponent(TouchAttackComponent.class));
  }

  @Test
  void snakeFactoryCompletesBurrowRenderAndDeathLifecycle() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    player.setPosition(5f, 5f);
    entities.register(player);
    Entity snake = NPCFactory.createSnakeMiniBoss(player);
    entities.register(snake);
    AnimationRenderComponent animator = snake.getComponent(AnimationRenderComponent.class);
    PhysicsComponent physics = snake.getComponent(PhysicsComponent.class);
    SnakeBurrowVisualComponent visual = snake.getComponent(SnakeBurrowVisualComponent.class);
    SnakeBurrowComponent burrow = snake.getComponent(SnakeBurrowComponent.class);
    SnakeMiniBossConfig config = new SnakeMiniBossConfig();
    SpriteBatch batch = mock(SpriteBatch.class);

    // Exercise real component registration order, then lazily allocate the first dust texture.
    advanceEntities(entities, time, 0f);
    ServiceLocator.getRenderService().render(batch);
    assertNull(animator.getCurrentAnimation());
    assertFalse(physics.getBody().isActive());

    advanceEntities(entities, time, config.burrowDuration);
    advanceEntities(entities, time, config.undergroundDuration);
    ServiceLocator.getRenderService().render(batch);
    assertTrue(visual.isWarningVisible());
    assertEquals(burrow.getWarningCentre(), visual.getWarningCentre());

    advanceEntities(entities, time, config.warningDuration);
    ServiceLocator.getRenderService().render(batch);
    assertEquals("default", animator.getCurrentAnimation());
    assertTrue(physics.getBody().isActive());
    assertFalse(visual.isWarningVisible());

    int healthAfterStrike = player.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(95, healthAfterStrike);
    snake.getComponent(CombatStatsComponent.class).setHealth(0);
    assertEquals("dieAnimation", animator.getCurrentAnimation());
    advanceEntities(entities, time, 0.6f);
    ServiceLocator.getRenderService().render(batch);
    advanceEntities(entities, time, 0.1f);

    assertEquals(healthAfterStrike, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    assertFalse(entities.getEntities().contains(snake, true));
    // A later room cleanup may dispose an enemy whose death animation already removed it.
    snake.dispose();
  }

  private void advanceEntities(EntityService entities, GameTime time, float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    entities.update();
  }

  @Test
  void snakeFactoryRendersSixteenPoisonShotsAndClearsThemOnDeath() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    player.setPosition(5f, 5f);
    entities.register(player);
    Entity snake = NPCFactory.createSnakeMiniBoss(player);
    entities.register(snake);
    CombatStatsComponent stats = snake.getComponent(CombatStatsComponent.class);
    SnakeBurrowComponent burrow = snake.getComponent(SnakeBurrowComponent.class);
    SnakePoisonVolleyComponent poison = snake.getComponent(SnakePoisonVolleyComponent.class);
    PhysicsComponent physics = snake.getComponent(PhysicsComponent.class);
    AnimationRenderComponent animator = snake.getComponent(AnimationRenderComponent.class);
    SnakeMiniBossConfig config = new SnakeMiniBossConfig();
    SpriteBatch batch = mock(SpriteBatch.class);

    advanceEntities(entities, time, 0f);
    advanceEntities(entities, time, config.burrowDuration);
    advanceEntities(entities, time, config.undergroundDuration);
    player.setPosition(50f, 50f);
    advanceEntities(entities, time, config.warningDuration);
    assertEquals("default", animator.getCurrentAnimation());
    stats.setHealth(stats.getMaxHealth() / 2);
    advanceEntities(entities, time, config.exposedDuration);

    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    assertEquals("chase", animator.getCurrentAnimation());
    assertTrue(Math.abs(animator.getRotation()) > 1f);
    assertTrue(physics.getBody().isActive());
    assertFalse(stats.isInvulnerable());
    assertTrue(poison.isSpitting());
    assertEquals(0, poison.getProjectileCount());
    ServiceLocator.getRenderService().render(batch);

    for (int volley = 0; volley < 4; volley++) {
      float facing = volley * 90f;
      Vector2 scale = snake.getScale();
      Vector2 targetCentre =
          new Vector2(0.28125f * scale.x + 50f, 0.21875f * scale.y)
              .rotateDeg(facing)
              .add(snake.getCenterPosition());
      player.setPosition(targetCentre.mulAdd(player.getScale(), -0.5f));
      advanceEntities(
          entities, time, volley == 0 ? config.spitWindupDuration : config.spitVolleyInterval);
      assertEquals((volley + 1) * 4, poison.getProjectileCount());
      ServiceLocator.getRenderService().render(batch);
      assertEquals("chase", animator.getCurrentAnimation());
      assertEquals(0f, (animator.getRotation() - facing + 540f) % 360f - 180f, 0.002f);
    }
    advanceEntities(entities, time, config.spitRecoveryDuration);

    assertEquals(SnakeBurrowComponent.State.BURROWING, burrow.getState());
    assertNull(animator.getCurrentAnimation());
    assertEquals(0f, animator.getRotation());
    assertFalse(poison.isSpitting());
    assertFalse(physics.getBody().isActive());
    assertEquals(16, poison.getProjectileCount());

    advanceEntities(entities, time, config.burrowDuration);
    advanceEntities(entities, time, config.undergroundDuration);
    player.setPosition(70f, 70f);
    advanceEntities(entities, time, config.warningDuration);
    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertEquals("default", animator.getCurrentAnimation());
    assertEquals(0f, animator.getRotation());

    stats.setHealth(0);
    assertEquals("dieAnimation", animator.getCurrentAnimation());
    assertEquals(0f, animator.getRotation());
    assertEquals(0, poison.getProjectileCount());
    advanceEntities(entities, time, 0.6f);
    ServiceLocator.getRenderService().render(batch);
    advanceEntities(entities, time, 0.1f);

    assertFalse(entities.getEntities().contains(snake, true));
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    snake.dispose();
  }

  @Test
  void floatingDemonHasHealthBar() {
    Entity enemy =
        NPCFactory.createFloatingDemon(
            new Entity(),
            new Vector2(0f, 0f),
            new Vector2(1f, 1f),
            new Vector2(2f, 0f),
            "images/floatingDemon.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void baseNpcDoesNotGainHealthBar() {
    assertNull(NPCFactory.createBaseNPC().getComponent(EnemyStatDisplay.class));
  }
}
