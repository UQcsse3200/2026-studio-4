package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Draws a locked dash warning and short-lived smoke at travelled positions. */
public class DragonCloudDashVisualComponent extends RenderComponent {
  public static final String SMOKE_PATH = "images/dragon/smoke_sheet.png";
  private static final float DASH_DISTANCE = 3f;
  private static final float PUFF_SPACING = 0.25f;
  private static final float FRAME_DURATION = 0.04f;
  private static final int FRAME_COUNT = 21;
  private static final float PUFF_LIFETIME = FRAME_COUNT * FRAME_DURATION;
  private static final float PUFF_SIZE = 1.2f;

  private final List<Puff> puffs = new ArrayList<>();
  private final Vector2 warningOrigin = new Vector2();
  private final Vector2 warningDirection = new Vector2();
  private DragonCloudDashComponent dash;
  private CombatStatsComponent stats;
  private TextureRegion pixel;
  private TextureRegion[] smokeFrames;
  private float distanceSincePuff;
  private boolean stopped;

  private static class Puff {
    private final Vector2 position;
    private float age;

    private Puff(Vector2 position) {
      this.position = position;
    }
  }

  @Override
  public void create() {
    dash = entity.getComponent(DragonCloudDashComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    if (dash == null || stats == null) {
      throw new IllegalStateException("Dash visuals require dash and combat stats");
    }
    entity.getEvents().addListener(DragonCloudDashComponent.ATTACK_STARTED, this::beginWarning);
    entity.getEvents().addListener(DragonCloudDashMovementComponent.DASH_STEP, this::onDashStep);
    entity.getEvents().addListener("entityDied", this::stop);
    super.create();
  }

  private void beginWarning() {
    if (stopped || stats.isDead()) {
      return;
    }
    warningOrigin.set(entity.getPosition()).add(footOffset());
    warningDirection.set(dash.getLockedDirection());
    distanceSincePuff = 0f;
  }

  private Vector2 footOffset() {
    return new Vector2(entity.getScale().x * 0.5f, entity.getScale().y * 0.2f);
  }

  private void onDashStep(Vector2 from, Vector2 to) {
    if (stopped || stats.isDead() || dash.getState() != DragonCloudDashComponent.State.DASHING) {
      return;
    }
    float length = from.dst(to);
    if (!Float.isFinite(length) || length <= 0f) {
      return;
    }
    float next = PUFF_SPACING - distanceSincePuff;
    while (next <= length && puffs.size() < 24) {
      Vector2 position = from.cpy().lerp(to, next / length).add(footOffset());
      puffs.add(new Puff(position));
      next += PUFF_SPACING;
    }
    distanceSincePuff = (distanceSincePuff + length) % PUFF_SPACING;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Ages existing smoke; rendering itself never advances time. */
  public void update(float delta) {
    if (stopped) {
      return;
    }
    if (stats.isDead() || dash.getState() == DragonCloudDashComponent.State.STOPPED) {
      stop();
      return;
    }
    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    for (Puff puff : puffs) {
      puff.age += delta;
    }
    puffs.removeIf(puff -> puff.age >= PUFF_LIFETIME);
  }

  public boolean isWarningVisible() {
    return !stopped
        && stats != null
        && !stats.isDead()
        && dash.getState() == DragonCloudDashComponent.State.WARNING;
  }

  public int getPuffCount() {
    return puffs.size();
  }

  Vector2 getWarningOrigin() {
    return warningOrigin.cpy();
  }

  Vector2 getWarningDirection() {
    return warningDirection.cpy();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (stopped || stats.isDead() || dash.getState() == DragonCloudDashComponent.State.STOPPED) {
      return;
    }
    float original = batch.getPackedColor();
    try {
      if (isWarningVisible()) {
        drawWarning(batch);
      }
      if (!puffs.isEmpty()) {
        drawSmoke(batch);
      }
    } finally {
      batch.setPackedColor(original);
    }
  }

  private void drawWarning(SpriteBatch batch) {
    if (pixel == null) {
      Pixmap map = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      try {
        map.setColor(Color.WHITE);
        map.fill();
        pixel = new TextureRegion(new Texture(map));
      } finally {
        map.dispose();
      }
    }
    float angle = warningDirection.angleDeg();
    float bodyWidth = entity.getScale().x * 0.9f;
    float bodyHeight = entity.getScale().y * 0.4f;
    float width =
        Math.abs(warningDirection.y) * bodyWidth + Math.abs(warningDirection.x) * bodyHeight;
    float front =
        (Math.abs(warningDirection.x) * bodyWidth + Math.abs(warningDirection.y) * bodyHeight) / 2f;
    float length = DASH_DISTANCE + front;

    batch.setColor(0.25f, 0.8f, 1f, 0.3f);
    drawStrip(batch, warningOrigin, length, width, angle);
    batch.setColor(0.75f, 0.95f, 1f, 0.9f);
    drawStrip(batch, warningOrigin, length, 0.06f, angle);
    Vector2 tip = warningOrigin.cpy().mulAdd(warningDirection, length);
    drawStrip(batch, tip, 0.4f, 0.07f, angle + 150f);
    drawStrip(batch, tip, 0.4f, 0.07f, angle - 150f);
  }

  private void drawStrip(SpriteBatch batch, Vector2 start, float length, float width, float angle) {
    batch.draw(pixel, start.x, start.y - width / 2f, 0f, width / 2f, length, width, 1f, 1f, angle);
  }

  private void drawSmoke(SpriteBatch batch) {
    if (smokeFrames == null) {
      Texture texture = ServiceLocator.getResourceService().getAsset(SMOKE_PATH, Texture.class);
      if (texture.getWidth() != 1280 || texture.getHeight() != 1280) {
        throw new IllegalStateException("Expected a 1280 x 1280 smoke sheet");
      }
      smokeFrames = new TextureRegion[FRAME_COUNT];
      for (int i = 0; i < FRAME_COUNT; i++) {
        smokeFrames[i] = new TextureRegion(texture, i % 5 * 256, i / 5 * 256, 256, 256);
      }
    }
    batch.setColor(0.8f, 0.95f, 1f, 1f);
    for (Puff puff : puffs) {
      int frame = Math.min(FRAME_COUNT - 1, (int) (puff.age / FRAME_DURATION));
      batch.draw(
          smokeFrames[frame],
          puff.position.x - PUFF_SIZE / 2f,
          puff.position.y - PUFF_SIZE / 2f,
          PUFF_SIZE,
          PUFF_SIZE);
    }
  }

  private void stop() {
    stopped = true;
    puffs.clear();
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.02f;
  }

  @Override
  public void dispose() {
    stop();
    if (pixel != null) {
      pixel.getTexture().dispose();
      pixel = null;
    }
    smokeFrames = null;
    super.dispose();
  }
}
