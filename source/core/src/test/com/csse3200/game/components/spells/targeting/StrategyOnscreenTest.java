package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.givenWorld;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link StrategyOnscreen}: enemies the player can actually see. */
@ExtendWith(GameExtension.class)
class StrategyOnscreenTest {

  /** A camera of the given size, looking at the given point. */
  private static CameraComponent cameraAt(float centreX, float centreY, float width, float height) {
    Camera camera = new OrthographicCamera();
    camera.position.set(centreX, centreY, 0f);
    CameraComponent component = mock(CameraComponent.class);
    when(component.getCamera()).thenReturn(camera);
    // A fresh vector per call: the strategy halves it in place.
    when(component.getCameraSize()).thenAnswer(invocation -> new Vector2(width, height));
    return component;
  }

  @Test
  void selectsAnEnemyWellInsideTheViewport() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity onScreen = enemyAt(1f, 1f);
    givenWorld(caster, onScreen);

    Array<Entity> targets = new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(onScreen, true));
  }

  @Test
  void excludesAnEnemyPastTheEdgeOfTheViewport() {
    Entity caster = nonEnemyAt(0f, 0f);
    givenWorld(caster, enemyAt(50f, 0f), enemyAt(0f, 50f));

    assertEquals(0, new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).selectTargets(caster).size);
  }

  @Test
  void includesAnEnemyExactlyOnTheViewportBoundary() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity onTheEdge = enemyAt(10f, 5f);
    givenWorld(caster, onTheEdge);

    Array<Entity> targets = new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).selectTargets(caster);

    assertEquals(1, targets.size);
  }

  @Test
  void followsTheCameraRatherThanTheWorldOrigin() {
    Entity caster = nonEnemyAt(100f, 100f);
    Entity nearTheCamera = enemyAt(101f, 100f);
    Entity nearTheOrigin = enemyAt(0f, 0f);
    givenWorld(caster, nearTheCamera, nearTheOrigin);

    Array<Entity> targets =
        new StrategyOnscreen(cameraAt(100f, 100f, 20f, 10f)).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(nearTheCamera, true));
  }

  @Test
  void judgesEachEnemyIndependentlyRatherThanCorruptingLaterReads() {
    // The strategy subtracts the camera centre from each position in place, so a bug here would
    // show up only once several enemies are checked in one pass.
    Entity caster = nonEnemyAt(0f, 0f);
    Entity first = enemyAt(1f, 1f);
    Entity second = enemyAt(2f, 2f);
    Entity third = enemyAt(3f, 3f);
    givenWorld(caster, first, second, third);

    Array<Entity> targets = new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).selectTargets(caster);

    assertEquals(3, targets.size);
  }

  @Test
  void excludesTheCasterAndOffLayerEntitiesEvenWhenTheyAreOnScreen() {
    Entity caster = enemyAt(0f, 0f);
    Entity enemy = enemyAt(1f, 1f);
    givenWorld(caster, enemy, nonEnemyAt(2f, 2f));

    Array<Entity> targets = new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(enemy, true));
  }

  @Test
  void rejectsBeingBuiltWithoutACamera() {
    assertThrows(IllegalArgumentException.class, () -> new StrategyOnscreen(null));
  }

  @Test
  void reportsNoRadiusBecauseAViewportIsNotACircle() {
    assertEquals(0f, new StrategyOnscreen(cameraAt(0f, 0f, 20f, 10f)).getRadius());
  }
}
