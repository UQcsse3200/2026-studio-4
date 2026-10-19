package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

public class DustCloudAnimation extends RenderComponent {
  private final Entity target;
  private static final int FRAME_COUNT = 7;
  private static final float SIZE_MULTIPLIER = 1.5f;
  private long startedAt;
  private GameTime time;
  private static final long FRAME_MILLIS = 100L;
  private final String atlas = "images/dust.png";
  private TextureRegion[] frames;
  private static final int CELL_SIZE = 64;
  private static final int ROW = 0;
  private boolean playing = false;

  public DustCloudAnimation(Entity target) {
    this.target = target;
    time = ServiceLocator.getTimeSource();
    target.getEvents().addListener("entityDied", this::dispose);
  }

  @Override
  public void create() {
    super.create();
    time = ServiceLocator.getTimeSource();
    startedAt = time.getTime();
    ensureFrames();
    playing = true;
  }

  @Override
  public void dispose() {
    playing = false;
    super.dispose();
  }

  @Override
  public void render(SpriteBatch batch) {
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!playing) {
      return;
    }
    if (elapsedMillis() / FRAME_MILLIS >= FRAME_COUNT) {
      startedAt = time.getTime(); // reset the timer
    }

    int frame = Math.min(FRAME_COUNT - 1, (int) (elapsedMillis() / FRAME_MILLIS));
    Vector2 centre = target.getCenterPosition();
    Vector2 scale = target.getScale();
    float size = Math.max(scale.x, scale.y) * SIZE_MULTIPLIER;
    float colour = batch.getPackedColor();
    try {
      batch.draw(frames[frame], centre.x - size / 2f, centre.y - size / 2f, size, size);
    } finally {
      batch.setPackedColor(colour);
    }
  }

  /** populate frames */
  private void ensureFrames() {
    if (frames != null) {
      return;
    }
    Texture sheet = ServiceLocator.getResourceService().getAsset(atlas, Texture.class);

    frames = new TextureRegion[FRAME_COUNT];
    for (int i = 0; i < FRAME_COUNT; i++) {
      frames[i] = new TextureRegion(sheet, i * CELL_SIZE, ROW * CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }
  }

  private long elapsedMillis() {
    return Math.max(0L, time.getTime() - startedAt);
  }

  @Override
  public float getZIndex() {
    // dust cloud exists behind entities
    return Float.NEGATIVE_INFINITY;
  }
}
