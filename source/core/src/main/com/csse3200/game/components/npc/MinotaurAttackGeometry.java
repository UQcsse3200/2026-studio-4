package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Pixel-aligned cutting masks for the minotaur's four active axe frames. */
public final class MinotaurAttackGeometry {
  // Reuse the CPU alpha copy across enemies. Weak keys do not retain unloaded room textures.
  private static final Map<Texture, AlphaMask> TEXTURES = new WeakHashMap<>();

  private static class Masks {
    private static final Map<String, Map<Integer, int[][]>> FRAMES = load();

    private static Map<String, Map<Integer, int[][]>> load() {
      JsonValue json =
          new JsonReader()
              .parse(Gdx.files.internal("images/minotaur-combat/attacks-hit-masks.json"));
      Map<String, Map<Integer, int[][]>> result = new HashMap<>();
      for (String name : new String[] {"normalAttack", "chargeAttack"}) {
        Map<Integer, int[][]> frames = new HashMap<>();
        for (JsonValue frame : json.get(name)) {
          JsonValue spans = frame.get("spans");
          int[][] rows = new int[spans.size][];
          int index = 0;
          for (JsonValue span : spans) rows[index++] = span.asIntArray();
          frames.put(frame.getInt("frame"), rows);
        }
        result.put(name, frames);
      }
      return result;
    }
  }

  /** The same full-canvas placement used by the attack renderer. */
  public static Rectangle canvas(Entity attacker) {
    Vector2 position = attacker.getPosition();
    Vector2 scale = attacker.getScale();
    return new Rectangle(
        position.x - scale.x * 70f / 148f,
        position.y,
        scale.x * 288f / 148f,
        scale.y * 160f / 130f);
  }

  /** Starts a wind-up when any actual cutting frame could reach the target from here. */
  public boolean inReach(String animation, Entity attacker, boolean flipX, Entity target) {
    HitArea area = hitArea(target);
    for (int[][] spans : Masks.FRAMES.get(animation).values()) {
      if (overlaps(spans, canvas(attacker), flipX, area)) return true;
    }
    return false;
  }

  /** Checks only the cutting pixels on the currently displayed frame, never torso or padding. */
  public boolean overlaps(
      String animation, int frame, Entity attacker, boolean flipX, Entity target) {
    int[][] spans = Masks.FRAMES.get(animation).get(frame);
    return spans != null && overlaps(spans, canvas(attacker), flipX, hitArea(target));
  }

  private boolean overlaps(int[][] spans, Rectangle canvas, boolean flipX, HitArea area) {
    if (area == null || !area.overlaps(canvas)) return false;
    float pixelWidth = canvas.width / 288f;
    float pixelHeight = canvas.height / 160f;
    Rectangle strip = new Rectangle();
    for (int[] span : spans) {
      int left = flipX ? 288 - span[2] : span[1];
      strip.set(
          canvas.x + left * pixelWidth,
          canvas.y + (159 - span[0]) * pixelHeight,
          (span[2] - span[1]) * pixelWidth,
          pixelHeight);
      if (area.overlaps(strip)) return true;
    }
    return false;
  }

  private HitArea hitArea(Entity target) {
    HitboxComponent hitbox = target.getComponent(HitboxComponent.class);
    Fixture fixture = hitbox == null ? null : hitbox.getFixture();
    if (fixture == null) return null;
    AnimationRenderComponent animator = target.getComponent(AnimationRenderComponent.class);
    if (animator != null) {
      TextureRegion frame = animator.getCurrentFrame();
      if (frame == null) return null;
      Texture texture = frame.getTexture();
      if (texture.getTextureData().getType() == TextureData.TextureDataType.Pixmap) {
        AlphaMask mask = TEXTURES.computeIfAbsent(texture, this::readAlpha);
        return new SpriteArea(target, animator, frame, mask);
      }
    }
    // Invisible test/logic actors have no sprite; respect their real fixture, not its AABB.
    if (fixture.getShape() instanceof PolygonShape polygon) {
      float[] vertices = new float[polygon.getVertexCount() * 2];
      Vector2 point = new Vector2();
      for (int i = 0; i < polygon.getVertexCount(); i++) {
        polygon.getVertex(i, point);
        point.set(fixture.getBody().getWorldPoint(point));
        vertices[i * 2] = point.x;
        vertices[i * 2 + 1] = point.y;
      }
      return rectangle -> Intersector.overlapConvexPolygons(vertices, vertices(rectangle), null);
    }
    if (fixture.getShape() instanceof CircleShape circle) {
      Vector2 centre = fixture.getBody().getWorldPoint(circle.getPosition());
      Circle worldCircle = new Circle(centre, circle.getRadius());
      return rectangle -> Intersector.overlaps(worldCircle, rectangle);
    }
    return null;
  }

  private AlphaMask readAlpha(Texture texture) {
    TextureData data = texture.getTextureData();
    if (!data.isPrepared()) data.prepare();
    Pixmap pixels = data.consumePixmap();
    try {
      BitSet opaque = new BitSet(pixels.getWidth() * pixels.getHeight());
      for (int y = 0; y < pixels.getHeight(); y++) {
        for (int x = 0; x < pixels.getWidth(); x++) {
          // Exclude translucent ground shadows as well as transparent atlas padding.
          if ((pixels.getPixel(x, y) & 0xff) >= 128) opaque.set(y * pixels.getWidth() + x);
        }
      }
      return new AlphaMask(pixels.getWidth(), opaque);
    } finally {
      if (data.disposePixmap()) pixels.dispose();
    }
  }

  private interface HitArea {
    boolean overlaps(Rectangle rectangle);
  }

  private record AlphaMask(int width, BitSet opaque) {}

  private static float[] vertices(Rectangle rectangle) {
    return new float[] {
      rectangle.x,
      rectangle.y,
      rectangle.x + rectangle.width,
      rectangle.y,
      rectangle.x + rectangle.width,
      rectangle.y + rectangle.height,
      rectangle.x,
      rectangle.y + rectangle.height
    };
  }

  /** Uses the player's visible body frame, including its current flip, offset and rotation. */
  private static class SpriteArea implements HitArea {
    private final TextureRegion frame;
    private final AlphaMask mask;
    private final Vector2 position;
    private final Vector2 scale;
    private final int sourceX;
    private final int sourceY;
    private final boolean flipX;
    private final boolean flipY;
    private final float cosine;
    private final float sine;

    SpriteArea(
        Entity target, AnimationRenderComponent animator, TextureRegion frame, AlphaMask mask) {
      this.frame = frame;
      this.mask = mask;
      position = target.getPosition().add(0f, animator.getVerticalOffset());
      scale = target.getScale();
      sourceX = frame.getRegionX() - (frame.isFlipX() ? frame.getRegionWidth() : 0);
      sourceY = frame.getRegionY() - (frame.isFlipY() ? frame.getRegionHeight() : 0);
      flipX = animator.isFlipX() ^ frame.isFlipX();
      flipY = animator.isFlipY() ^ frame.isFlipY();
      cosine = MathUtils.cosDeg(-animator.getRotation());
      sine = MathUtils.sinDeg(-animator.getRotation());
    }

    @Override
    public boolean overlaps(Rectangle rectangle) {
      if (scale.x <= 0f || scale.y <= 0f) return false;
      int width = frame.getRegionWidth();
      int height = frame.getRegionHeight();
      float[] local = vertices(rectangle);
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      for (int i = 0; i < local.length; i += 2) {
        float dx = local[i] - position.x - scale.x / 2f;
        float dy = local[i + 1] - position.y - scale.y / 2f;
        local[i] = ((dx * cosine - dy * sine) / scale.x + 0.5f) * width;
        local[i + 1] = ((dx * sine + dy * cosine) / scale.y + 0.5f) * height;
        minX = Math.min(minX, local[i]);
        maxX = Math.max(maxX, local[i]);
        minY = Math.min(minY, local[i + 1]);
        maxY = Math.max(maxY, local[i + 1]);
      }
      // Ignore sub-pixel floating-point noise at two exactly adjacent pixel edges.
      float epsilon = 0.00001f;
      minX += epsilon;
      minY += epsilon;
      maxX -= epsilon;
      maxY -= epsilon;
      Rectangle pixel = new Rectangle(0f, 0f, 1f, 1f);
      Intersector.MinimumTranslationVector overlap = new Intersector.MinimumTranslationVector();
      for (int y = Math.max(0, MathUtils.floor(minY));
          y < Math.min(height, MathUtils.ceil(maxY));
          y++) {
        for (int x = Math.max(0, MathUtils.floor(minX));
            x < Math.min(width, MathUtils.ceil(maxX));
            x++) {
          var pixelX = (float) x;
          var pixelY = (float) y;
          if (isOpaque(x, y, width, height)
              && (Math.abs(sine) < epsilon
                  || (Intersector.overlapConvexPolygons(
                          local, vertices(pixel.setPosition(pixelX, pixelY)), overlap)
                      && overlap.depth > epsilon))) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean isOpaque(int x, int y, int width, int height) {
      int tx = sourceX + (flipX ? width - 1 - x : x);
      int ty = sourceY + (flipY ? y : height - 1 - y);
      return mask.opaque.get(ty * mask.width + tx);
    }
  }
}
