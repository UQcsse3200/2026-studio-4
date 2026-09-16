package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link StrategyOnscreen}.
 *
 * <p>Uses a real {@link CameraComponent} rather than mocking it &mdash; it has no physics/rendering
 * dependencies to construct, and its viewport is easy to fix deterministically via {@link
 * CameraComponent#resize} with its position set directly on the underlying LibGDX {@code Camera}.
 * See {@link AllEnemiesTargetingStrategyTest} for the other scaffolding assumptions shared by this
 * test class.
 */
@ExtendWith(GameExtension.class)
class OnScreenEnemiesTargetingStrategyTest {
  private EntityService entityService;
  private CameraComponent cameraComponent;
  private Entity caster;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    cameraComponent = new CameraComponent();
    // screenWidth == screenHeight -> ratio 1 -> a 10x10 square viewport (half-extent 5 on each
    // axis), fixed and easy to reason about.
    cameraComponent.resize(20, 20, 10);
    cameraComponent.getCamera().position.set(0f, 0f, 0f);

    caster = mock(Entity.class);
    when(caster.getCenterPosition()).thenReturn(new Vector2(0f, 0f));
  }

  private void givenWorldEntities(Entity... entities) {
    Array<Entity> array = new Array<>();
    for (Entity entity : entities) {
      array.add(entity);
    }
    when(entityService.getEntities()).thenReturn(array);
  }

  @Test
  void constructorRejectsNullCamera() {
    assertThrows(IllegalArgumentException.class, () -> new StrategyOnscreen(null));
  }

  @Test
  void includesEnemyWellWithinTheViewport() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    Entity onScreen = enemyAt(2f, 2f); // within the 5-unit half-viewport
    givenWorldEntities(onScreen);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(onScreen, targets.first());
  }

  @Test
  void excludesEnemyOutsideTheViewport() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    Entity offScreen = enemyAt(50f, 0f); // far beyond the 5-unit half-viewport
    givenWorldEntities(offScreen);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }

  @Test
  void includesEnemyExactlyOnTheViewportBoundary() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    // Half-viewport is (5,5); the bounds check uses <=, not <, so a centre exactly 5 units out is
    // still included.
    Entity onBoundary = enemyAt(5f, 0f);
    givenWorldEntities(onBoundary);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(onBoundary, targets.first());
  }

  @Test
  void excludesNonEnemyEntitiesEvenOnScreen() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    Entity player = nonEnemyAt(1f, 1f);
    givenWorldEntities(player);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }

  @Test
  void excludesTheCasterEvenWhenOnScreenAndOnTheEnemyLayer() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    Entity npcCaster = enemyAt(0f, 0f);
    givenWorldEntities(npcCaster);

    Array<Entity> targets = strategy.selectTargets(npcCaster);

    assertEquals(0, targets.size);
  }

  @Test
  void viewportIsRelativeToCameraPositionNotTheWorldOrigin() {
    StrategyOnscreen strategy = new StrategyOnscreen(cameraComponent);
    // Move the camera away from the origin: an enemy near the old origin should now be off-screen,
    // and one near the camera's new position should be on-screen.
    cameraComponent.getCamera().position.set(100f, 100f, 0f);
    Entity nearOldOrigin = enemyAt(0f, 0f);
    Entity nearCamera = enemyAt(101f, 99f);
    givenWorldEntities(nearOldOrigin, nearCamera);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(nearCamera, targets.first());
  }
}
