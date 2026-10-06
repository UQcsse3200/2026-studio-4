package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.Optional;
import java.util.Random;

/** Current visible room space and near/far candidates for the Snake's green gems. */
final class SnakeGemSpawnArea {
  private static final float NEAR_MIN = 1.5f;
  private static final float NEAR_MAX = 3.5f;
  private static final int PREFERRED_ATTEMPTS = 16;

  private SnakeGemSpawnArea() {}

  /**
   * Intersects the axis-aligned gameplay view with the room after insetting both by the effect's
   * margin. An unavailable or invalid view never falls back to spawning across the whole room.
   */
  static Optional<Rectangle> visibleArea(Camera camera, Rectangle roomBounds, float margin) {
    if (!(camera instanceof OrthographicCamera orthographic)
        || !validCamera(orthographic)
        || !Float.isFinite(margin)
        || margin < 0f) {
      return Optional.empty();
    }
    float width = orthographic.viewportWidth * orthographic.zoom;
    float height = orthographic.viewportHeight * orthographic.zoom;
    Rectangle view =
        inset(
            new Rectangle(
                orthographic.position.x - width / 2f,
                orthographic.position.y - height / 2f,
                width,
                height),
            margin);
    if (view == null || roomBounds == null) {
      return Optional.ofNullable(view);
    }
    Rectangle room = inset(roomBounds, margin);
    if (room == null) {
      return Optional.empty();
    }
    float left = Math.max(view.x, room.x);
    float bottom = Math.max(view.y, room.y);
    float right = Math.min(view.x + view.width, room.x + room.width);
    float top = Math.min(view.y + view.height, room.y + room.height);
    if (right <= left || top <= bottom) {
      return Optional.empty();
    }
    return Optional.of(new Rectangle(left, bottom, right - left, top - bottom));
  }

  private static boolean validCamera(OrthographicCamera camera) {
    return positiveFinite(camera.viewportWidth)
        && positiveFinite(camera.viewportHeight)
        && positiveFinite(camera.zoom)
        && Float.isFinite(camera.position.x)
        && Float.isFinite(camera.position.y)
        && Float.isFinite(camera.position.z);
  }

  private static Rectangle inset(Rectangle source, float margin) {
    if (!positiveFinite(source.width)
        || !positiveFinite(source.height)
        || !Float.isFinite(source.x)
        || !Float.isFinite(source.y)
        || !Float.isFinite(source.x + source.width)
        || !Float.isFinite(source.y + source.height)) {
      return null;
    }
    float width = source.width - margin * 2f;
    float height = source.height - margin * 2f;
    if (width <= 0f || height <= 0f) {
      return null;
    }
    return new Rectangle(source.x + margin, source.y + margin, width, height);
  }

  private static boolean positiveFinite(float value) {
    return Float.isFinite(value) && value > 0f;
  }

  /**
   * The first sixteen nearby attempts use an annulus; all other attempts sample the visible area.
   * The caller still checks the area, preferred distance, player clearance and obstacle collisions.
   */
  static Vector2 sample(
      Random random, Rectangle area, Vector2 player, boolean preferNearby, int attempt) {
    if (preferNearby && attempt < PREFERRED_ATTEMPTS) {
      float angle = random.nextFloat() * MathUtils.PI2;
      float radius = NEAR_MIN + random.nextFloat() * (NEAR_MAX - NEAR_MIN);
      return new Vector2(player).add(MathUtils.cos(angle) * radius, MathUtils.sin(angle) * radius);
    }
    return new Vector2(
        area.x + random.nextFloat() * area.width, area.y + random.nextFloat() * area.height);
  }

  static boolean inPreferredBand(Vector2 candidate, Vector2 player, boolean near) {
    float distanceSquared = candidate.dst2(player);
    return near
        ? distanceSquared >= NEAR_MIN * NEAR_MIN && distanceSquared <= NEAR_MAX * NEAR_MAX
        : distanceSquared >= NEAR_MAX * NEAR_MAX;
  }
}
