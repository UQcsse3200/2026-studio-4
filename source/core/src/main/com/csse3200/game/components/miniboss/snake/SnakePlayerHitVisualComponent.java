package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/** A brief green impact following the player after an unblocked Snake hit. */
public class SnakePlayerHitVisualComponent extends RenderComponent {
  public static final String HIT_SHEET = "images/snake-miniboss/hit/green-hit-sheet.png";
  private static final int CELL_SIZE = 64;
  private static final int FRAME_COUNT = 7;
  private static final long FRAME_MILLIS = 80L;
  private static final long DURATION_MILLIS = FRAME_COUNT * FRAME_MILLIS;
  private static final float SIZE_MULTIPLIER = 1.5f;
  private final Entity target;
  private TextureRegion[] frames;
  private GameTime time;
  private long startedAt;
  private boolean playing;
  private boolean stopped;

  public SnakePlayerHitVisualComponent(Entity target) {
    this.target = Objects.requireNonNull(target);
  }

  @Override
  public void create() {
    super.create();
    time = ServiceLocator.getTimeSource();
    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Restart one impact after damage is confirmed; repeated hits never stack renderers. */
  public void play() {
    if (stopped || time == null || targetIsDead()) {
      return;
    }
    startedAt = time.getTime();
    playing = true;
  }

  @Override
  public void update() {
    if (!isPlaying()) {
      playing = false;
    }
  }

  // Keep the green artwork independent of both the player's red flash and the Snake's tint/glow.
  @Override
  public void render(SpriteBatch batch) {
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isPlaying()) {
      return;
    }
    ensureFrames();
    int frame = Math.min(FRAME_COUNT - 1, (int) (elapsedMillis() / FRAME_MILLIS));
    Vector2 centre = target.getCenterPosition();
    Vector2 scale = target.getScale();
    float size = Math.max(scale.x, scale.y) * SIZE_MULTIPLIER;
    float colour = batch.getPackedColor();
    try {
      batch.setColor(1f, 1f, 1f, 0.85f);
      batch.draw(frames[frame], centre.x - size / 2f, centre.y - size / 2f, size, size);
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void ensureFrames() {
    if (frames != null) {
      return;
    }
    Texture sheet = ServiceLocator.getResourceService().getAsset(HIT_SHEET, Texture.class);
    frames = new TextureRegion[FRAME_COUNT];
    // Second row from the top, columns 0-6. Columns 7-10 are empty.
    for (int i = 0; i < FRAME_COUNT; i++) {
      frames[i] = new TextureRegion(sheet, i * CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }
  }

  boolean isPlaying() {
    return playing && !stopped && !targetIsDead() && elapsedMillis() < DURATION_MILLIS;
  }

  private long elapsedMillis() {
    return Math.max(0L, time.getTime() - startedAt);
  }

  private boolean targetIsDead() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  private void stop() {
    playing = false;
    stopped = true;
  }

  @Override
  public float getZIndex() {
    // The impact must stay in front of the player even when the Snake overlaps them.
    return Float.POSITIVE_INFINITY;
  }

  @Override
  public void dispose() {
    stop();
    frames = null; // The room owns and unloads the shared texture.
    super.dispose();
  }
}
