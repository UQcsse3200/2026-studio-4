package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.csse3200.game.extensions.GameExtension;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeGemSpawnAreaTest {
  @Test
  void followsCurrentCameraPositionZoomAndViewportSize() {
    OrthographicCamera camera = new OrthographicCamera(10f, 8f);
    camera.position.set(20f, 30f, 0f);
    Rectangle initial = SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).orElseThrow();
    assertArea(initial, 15.8f, 26.8f, 8.4f, 6.4f);

    camera.zoom = 0.5f;
    Rectangle zoomed = SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).orElseThrow();
    assertArea(zoomed, 18.3f, 28.8f, 3.4f, 2.4f);

    camera.position.set(5f, -2f, 0f);
    camera.viewportWidth = 12f;
    camera.viewportHeight = 6f;
    Rectangle moved = SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).orElseThrow();
    assertArea(moved, 2.8f, -2.7f, 4.4f, 1.4f);
    assertFalse(moved.contains(initial.getCenter(new Vector2())));
  }

  @Test
  void intersectsBothInsetRectanglesWithoutChangingInputs() {
    OrthographicCamera camera = new OrthographicCamera(20f, 10f);
    camera.position.set(0f, 0f, 2f);
    Rectangle room = new Rectangle(-2f, -8f, 8f, 12f);
    Rectangle originalRoom = new Rectangle(room);
    Vector3 originalPosition = camera.position.cpy();

    Rectangle result = SnakeGemSpawnArea.visibleArea(camera, room, 1f).orElseThrow();

    assertArea(result, -1f, -4f, 6f, 7f);
    assertNotSame(room, result);
    assertEquals(originalRoom, room);
    assertEquals(originalPosition, camera.position);
    assertEquals(20f, camera.viewportWidth);
    assertEquals(10f, camera.viewportHeight);
    assertEquals(1f, camera.zoom);
    result.set(100f, 100f, 1f, 1f);
    assertEquals(originalRoom, room);
  }

  @Test
  void refusesUnavailableUninitialisedAndNonOrthographicViews() {
    Rectangle room = new Rectangle(-50f, -50f, 100f, 100f);
    assertTrue(SnakeGemSpawnArea.visibleArea(null, room, 0.8f).isEmpty());
    assertTrue(SnakeGemSpawnArea.visibleArea(new OrthographicCamera(), room, 0.8f).isEmpty());
    assertTrue(
        SnakeGemSpawnArea.visibleArea(new PerspectiveCamera(67f, 10f, 8f), room, 0.8f).isEmpty());
  }

  @Test
  void rejectsNonFiniteCameraAndRoomGeometryRatherThanReturningWholeRoom() {
    OrthographicCamera camera = new OrthographicCamera(10f, 8f);
    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      camera.zoom = invalid;
      assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).isEmpty());
    }
    camera.zoom = 1f;
    camera.position.x = Float.NaN;
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).isEmpty());
    camera.position.setZero();
    camera.viewportHeight = Float.POSITIVE_INFINITY;
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).isEmpty());
    camera.viewportHeight = 8f;
    assertTrue(
        SnakeGemSpawnArea.visibleArea(camera, new Rectangle(0f, 0f, Float.NaN, 8f), 0.8f)
            .isEmpty());
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, Float.NaN).isEmpty());
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, -1f).isEmpty());
  }

  @Test
  void rejectsNarrowViewsRoomsAndDisjointIntersections() {
    OrthographicCamera camera = new OrthographicCamera(1.5f, 8f);
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).isEmpty());
    camera.viewportWidth = 10f;
    camera.position.setZero();
    assertTrue(
        SnakeGemSpawnArea.visibleArea(camera, new Rectangle(-1f, -1f, 1f, 6f), 0.8f).isEmpty());
    assertTrue(
        SnakeGemSpawnArea.visibleArea(camera, new Rectangle(20f, 20f, 10f, 10f), 0.8f).isEmpty());
    camera.zoom = Float.MAX_VALUE;
    assertTrue(SnakeGemSpawnArea.visibleArea(camera, null, 0.8f).isEmpty());
  }

  @Test
  void nearbySamplesUseThePreferredAnnulusAndAreDeterministic() {
    Rectangle area = new Rectangle(-10f, -10f, 20f, 20f);
    Vector2 player = new Vector2(2f, 1f);
    Random first = new Random(42L);
    Random replay = new Random(42L);
    for (int attempt = 0; attempt < 16; attempt++) {
      Vector2 point = SnakeGemSpawnArea.sample(first, area, player, true, attempt);
      assertTrue(SnakeGemSpawnArea.inPreferredBand(point, player, true));
      assertEquals(point, SnakeGemSpawnArea.sample(replay, area, player, true, attempt));
    }
    assertEquals(new Vector2(2f, 1f), player);
    assertEquals(new Rectangle(-10f, -10f, 20f, 20f), area);
    assertFalse(SnakeGemSpawnArea.inPreferredBand(player.cpy().add(1f, 0f), player, true));
    assertFalse(SnakeGemSpawnArea.inPreferredBand(player.cpy().add(4f, 0f), player, true));
  }

  @Test
  void distantSamplingAndLateFallbackStayWithinTheVisibleArea() {
    Rectangle area = new Rectangle(20f, 20f, 3f, 2f);
    Vector2 player = new Vector2();
    Random random = new Random(17L);
    for (int attempt = 0; attempt < 32; attempt++) {
      Vector2 far = SnakeGemSpawnArea.sample(random, area, player, false, attempt);
      assertTrue(area.contains(far));
      assertTrue(SnakeGemSpawnArea.inPreferredBand(far, player, false));
      if (attempt >= 16) {
        Vector2 fallback = SnakeGemSpawnArea.sample(random, area, player, true, attempt);
        assertTrue(area.contains(fallback));
        assertFalse(SnakeGemSpawnArea.inPreferredBand(fallback, player, true));
      }
    }
    assertFalse(SnakeGemSpawnArea.inPreferredBand(new Vector2(3f, 0f), player, false));
    assertTrue(SnakeGemSpawnArea.inPreferredBand(new Vector2(3.5f, 0f), player, false));
  }

  private static void assertArea(Rectangle area, float x, float y, float width, float height) {
    assertEquals(x, area.x, 0.00001f);
    assertEquals(y, area.y, 0.00001f);
    assertEquals(width, area.width, 0.00001f);
    assertEquals(height, area.height, 0.00001f);
  }
}
