package com.csse3200.game.components.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** Drop-only fountain arc, bottom-pivot rocking and glass reflection. */
public class ItemDropAnimationComponent extends RenderComponent {
  private static final float SETTLE_DURATION = 3.2f;
  private static final float SWAY_ANGLE = 0.18f;
  private static final float GLINT_PERIOD = 2.1f;
  private static final float GLINT_DURATION = 1f;
  private static final float GLINT_WIDTH = 0.22f;
  private static final float GLINT_CORE_WIDTH = 0.08f;
  private static final int CELLS = 8;
  private final Vector2 offset = new Vector2();
  private final Vector2 spawnOffset = new Vector2();
  private final float[] vertices = new float[20];
  private final float swayPeriod;
  private Vector2 burstOrigin;
  private TextureRegion[] strips;
  private TextureRegion[][] tiles;
  private float hopDuration = 0.48f;
  private float hopHeight = 0.35f;
  private float elapsed;
  private float swayAngle;
  private float swayCos = 1f;
  private float swaySin;
  private boolean landed;

  public ItemDropAnimationComponent() {
    this(ThreadLocalRandom.current());
  }

  ItemDropAnimationComponent(RandomGenerator random) {
    // Sample once per item so its rhythm stays smooth and independent of loot rolls.
    swayPeriod = (float) random.nextDouble(0.5, 0.7);
  }

  /** Makes different-sized drops start visually at the same point during their outward flight. */
  public void launchFrom(Vector2 origin) {
    burstOrigin = origin.cpy();
    hopDuration = 0.7f;
    hopHeight = 0.9f;
  }

  @Override
  public void create() {
    super.create();
    if (burstOrigin != null) spawnOffset.set(burstOrigin).sub(entity.getCenterPosition());
    updatePose();
    ItemComponent item = entity.getComponent(ItemComponent.class);
    if (item == null) return;
    Texture texture =
        ServiceLocator.getResourceService().getAsset(item.getItem().getTexture(), Texture.class);
    strips = new TextureRegion[CELLS];
    tiles = new TextureRegion[CELLS][CELLS];
    for (int y = 0; y < CELLS; y++) {
      int top = y * texture.getHeight() / CELLS;
      int height = (y + 1) * texture.getHeight() / CELLS - top;
      strips[y] = new TextureRegion(texture, 0, top, texture.getWidth(), height);
      for (int x = 0; x < CELLS; x++) {
        int left = x * texture.getWidth() / CELLS;
        tiles[y][x] =
            new TextureRegion(
                texture, left, top, (x + 1) * texture.getWidth() / CELLS - left, height);
      }
    }
  }

  @Override
  public void update() {
    var time = ServiceLocator.getTimeSource();
    if (time == null) return;
    float delta = time.getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;
    elapsed += delta;
    updatePose();
  }

  private void updatePose() {
    if (elapsed < hopDuration) {
      float progress = elapsed / hopDuration;
      offset.set(spawnOffset).scl(1f - progress);
      offset.y += 4f * hopHeight * progress * (1f - progress);
      swayAngle = 0f;
    } else {
      offset.setZero();
      if (burstOrigin != null && !landed) {
        PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
        if (physics != null) physics.getBody().setLinearVelocity(Vector2.Zero);
        landed = true;
      }
      float settling = elapsed - hopDuration;
      if (settling >= SETTLE_DURATION) {
        swayAngle = 0f;
      } else {
        float remaining = 1f - settling / SETTLE_DURATION;
        swayAngle =
            SWAY_ANGLE
                * remaining
                * remaining
                * (float) Math.sin(2d * Math.PI * settling / swayPeriod);
      }
    }
    swayCos = (float) Math.cos(swayAngle);
    swaySin = (float) Math.sin(swayAngle);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (tiles == null) return;
    Vector2 centre = entity.getCenterPosition().add(offset);
    Vector2 size = entity.getScale();
    float left = centre.x - size.x / 2f;
    float bottom = centre.y - size.y / 2f;
    // Base texture and reflection share a rigid rotation about the bottom centre.
    for (int y = 0; y < CELLS; y++) {
      drawQuad(batch, strips[y], left, bottom, size, 0f, 1f, y);
    }
    float phase = elapsed % GLINT_PERIOD;
    if (phase > GLINT_DURATION) return;
    Color color = batch.getColor();
    float r = color.r, g = color.g, b = color.b, a = color.a;
    int source = batch.getBlendSrcFunc(), destination = batch.getBlendDstFunc();
    int sourceAlpha = batch.getBlendSrcFuncAlpha(), destinationAlpha = batch.getBlendDstFuncAlpha();
    try {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      for (int y = 0; y < CELLS; y++) {
        for (int x = 0; x < CELLS; x++) {
          float distance = Math.abs((x + y) / (float) (CELLS * 2 - 2) - phase / GLINT_DURATION);
          if (distance > GLINT_WIDTH) continue;
          batch.setColor(0.85f * r, 0.95f * g, b, a * (1f - distance / GLINT_WIDTH) * 0.45f);
          drawQuad(batch, tiles[y][x], left, bottom, size, x / (float) CELLS, (x + 1f) / CELLS, y);
          if (distance < GLINT_CORE_WIDTH) {
            batch.setColor(r, g, b, a * (1f - distance / GLINT_CORE_WIDTH) * 0.9f);
            drawQuad(
                batch, tiles[y][x], left, bottom, size, x / (float) CELLS, (x + 1f) / CELLS, y);
          }
        }
      }
    } finally {
      batch.setColor(r, g, b, a);
      batch.setBlendFunctionSeparate(source, destination, sourceAlpha, destinationAlpha);
    }
  }

  private void drawQuad(
      SpriteBatch batch,
      TextureRegion region,
      float left,
      float bottom,
      Vector2 size,
      float x0,
      float x1,
      int row) {
    float y0 = (CELLS - 1f - row) / CELLS;
    float y1 = (CELLS - (float) row) / CELLS;
    float pivotX = left + size.x / 2f;
    float packed = batch.getPackedColor();
    vertex(
        0,
        pivotX,
        bottom,
        size.x * (x0 - 0.5f),
        size.y * y0,
        packed,
        region.getU(),
        region.getV2());
    vertex(
        5, pivotX, bottom, size.x * (x0 - 0.5f), size.y * y1, packed, region.getU(), region.getV());
    vertex(
        10,
        pivotX,
        bottom,
        size.x * (x1 - 0.5f),
        size.y * y1,
        packed,
        region.getU2(),
        region.getV());
    vertex(
        15,
        pivotX,
        bottom,
        size.x * (x1 - 0.5f),
        size.y * y0,
        packed,
        region.getU2(),
        region.getV2());
    batch.draw(region.getTexture(), vertices, 0, vertices.length);
  }

  private void vertex(
      int index, float pivotX, float pivotY, float x, float y, float color, float u, float v) {
    vertices[index] = pivotX + x * swayCos + y * swaySin;
    vertices[index + 1] = pivotY - x * swaySin + y * swayCos;
    vertices[index + 2] = color;
    vertices[index + 3] = u;
    vertices[index + 4] = v;
  }
}
