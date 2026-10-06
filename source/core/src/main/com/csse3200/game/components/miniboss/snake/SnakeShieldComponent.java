package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/** One refillable player shield shared by this Snake's poison and emergence attacks. */
public class SnakeShieldComponent extends RenderComponent {
  public static final String SHIELD_TEXTURE = "images/snake-miniboss/shield/shieldGreen_Edit.png";
  private static final float RADIUS_SCALE = 0.8f;
  private static final float SHIELD_ALPHA = 0.42f;
  private static final float BLOCK_FLASH_DURATION = 0.16f;
  private static final float BLOCK_FLASH_ALPHA = 0.36f;
  private static final float BAR_WIDTH = 0.11f;
  private static final float BAR_HEIGHT = 0.72f;
  private static final float BAR_BORDER = 0.016f;
  private static final float BAR_GAP = 0.08f;
  private final Entity target;
  private final int capacity;
  private GameTime time;
  private Texture shieldTexture;
  private Texture pixel;
  private int durability;
  private float flashRemaining;
  private boolean stopped;
  private boolean disposed;

  public SnakeShieldComponent(Entity target, SnakeMiniBossConfig config) {
    this.target = Objects.requireNonNull(target);
    capacity = Math.max(0, Objects.requireNonNull(config).shieldCapacity);
  }

  @Override
  public void create() {
    super.create();
    time = ServiceLocator.getTimeSource();
    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Restores the same durability bar; pickups never create a second shield or reserve. */
  public void refill() {
    if (encounterAvailable()) {
      durability = capacity;
      flashRemaining = 0f;
    }
  }

  /** Consumes one point for a confirmed Snake attack, without damaging the player. */
  public boolean tryBlock() {
    if (!isActive()) {
      return false;
    }
    durability--;
    flashRemaining = BLOCK_FLASH_DURATION;
    return true;
  }

  public boolean isActive() {
    return encounterAvailable() && durability > 0;
  }

  public int getDurability() {
    return encounterAvailable() ? durability : 0;
  }

  public int getCapacity() {
    return capacity;
  }

  /** The visible shield and poison interception use this same world-space radius. */
  public float getRadius() {
    Vector2 scale = target.getScale();
    return Math.max(scale.x, scale.y) * RADIUS_SCALE;
  }

  /** Empties the shield while allowing another pickup during the same encounter. */
  public void clear() {
    durability = 0;
    flashRemaining = 0f;
  }

  @Override
  public void update() {
    if (!encounterAvailable() || StatusEffectsControllerComponent.isImmobilised(entity)) {
      return;
    }
    float delta = time == null ? 0f : time.getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      flashRemaining = Math.max(0f, flashRemaining - delta);
    }
  }

  private boolean encounterAvailable() {
    if (stopped || entity == null) {
      return false;
    }
    if (isDead(entity) || isDead(target)) {
      stop();
      return false;
    }
    return true;
  }

  private static boolean isDead(Entity entity) {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  private void stop() {
    stopped = true;
    clear();
  }

  // The green shield must not inherit either entity's damage tint or additional glow pass.
  @Override
  public void render(SpriteBatch batch) {
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isActive()) {
      return;
    }
    ensureTextures();
    float colour = batch.getPackedColor();
    try {
      Vector2 centre = target.getCenterPosition();
      float radius = getRadius();
      float alpha = SHIELD_ALPHA + BLOCK_FLASH_ALPHA * flashRemaining / BLOCK_FLASH_DURATION;
      batch.setColor(1f, 1f, 1f, alpha);
      batch.draw(shieldTexture, centre.x - radius, centre.y - radius, radius * 2f, radius * 2f);
      drawBar(batch);
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void drawBar(SpriteBatch batch) {
    Vector2 position = target.getPosition();
    Vector2 scale = target.getScale();
    float x = position.x + scale.x + BAR_GAP;
    float y = position.y + (scale.y - BAR_HEIGHT) / 2f;
    batch.setColor(0.15f, 0.3f, 0.15f, 1f);
    batch.draw(pixel, x, y, BAR_WIDTH, BAR_HEIGHT);
    float width = BAR_WIDTH - BAR_BORDER * 2f;
    float height = BAR_HEIGHT - BAR_BORDER * 2f;
    batch.setColor(0.04f, 0.1f, 0.04f, 0.95f);
    batch.draw(pixel, x + BAR_BORDER, y + BAR_BORDER, width, height);
    batch.setColor(0.25f, 0.9f, 0.25f, 1f);
    batch.draw(pixel, x + BAR_BORDER, y + BAR_BORDER, width, height * durability / capacity);
  }

  private void ensureTextures() {
    if (shieldTexture == null) {
      shieldTexture = ServiceLocator.getResourceService().getAsset(SHIELD_TEXTURE, Texture.class);
    }
    if (pixel == null) {
      Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      try {
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
      } finally {
        pixmap.dispose();
      }
    }
  }

  @Override
  public float getZIndex() {
    return Float.POSITIVE_INFINITY;
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    stop();
    if (pixel != null) {
      pixel.dispose();
      pixel = null;
    }
    shieldTexture = null; // Only the room unloads this shared asset.
    super.dispose();
  }
}
