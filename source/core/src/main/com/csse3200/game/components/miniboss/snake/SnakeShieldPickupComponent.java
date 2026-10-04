package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Stage-two green gems, owned by the Snake encounter rather than separate physics entities. */
public class SnakeShieldPickupComponent extends RenderComponent {
  public static final String GEM_TEXTURE =
      "images/snake-miniboss/pickup/GEM 1 - LIGHT GREEN - Spritesheet.png";
  public static final String SPAWN_TEXTURE =
      "images/snake-miniboss/pickup/green-gem-spawn-sheet.png";
  private static final int SPAWN_FRAME_COUNT = 7;
  private static final float SPAWN_FRAME_DURATION = 0.05f;
  private static final float SPAWN_DURATION = SPAWN_FRAME_COUNT * SPAWN_FRAME_DURATION;
  private static final float SPAWN_SIZE = 1.1f;
  private static final float VISUAL_HEIGHT = 0.65f;
  private static final float VISUAL_WIDTH = VISUAL_HEIGHT * 18f / 30f;
  private static final float PICKUP_RADIUS = 0.65f;
  private static final float FRAME_DURATION = 0.09f;
  private static final int PLACEMENT_ATTEMPTS = 32;
  private final Entity target;
  private final SnakeMiniBossConfig config;
  private final Random random;
  private final List<Gem> gems = new ArrayList<>();
  private Rectangle bounds;
  private Camera camera;
  private GameTime time;
  private TextureRegion[] frames;
  private TextureRegion[] spawnFrames;
  private float spawnAge;
  private boolean active;
  private boolean stopped;
  private boolean fresh;
  private boolean preferNearby = true;

  public SnakeShieldPickupComponent(Entity target, SnakeMiniBossConfig config) {
    this(target, config, new Random());
  }

  SnakeShieldPickupComponent(Entity target, SnakeMiniBossConfig config, Random random) {
    this.target = Objects.requireNonNull(target);
    this.config = Objects.requireNonNull(config);
    this.random = Objects.requireNonNull(random);
  }

  /** Copies the room bounds so later changes to the caller's rectangle cannot move the drops. */
  public void setArenaBounds(Rectangle bounds) {
    this.bounds = bounds == null ? null : new Rectangle(bounds);
  }

  /** Uses the live gameplay camera; null resolves the currently registered world camera. */
  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  @Override
  public void create() {
    super.create();
    time = ServiceLocator.getTimeSource();
    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Activates once when stage two begins and seeds the first usable drops. */
  public void start() {
    if (active || encounterEnded()) {
      return;
    }
    active = true;
    fresh = true;
    spawnAge = 0f;
    preferNearby = true;
    int initial = Math.min(config.shieldGemInitialCount, config.shieldGemMaxActive);
    for (int i = 0; i < initial; i++) {
      spawnGem();
    }
  }

  /** Clears the current drops and disables spawning until explicitly started again. */
  public void clear() {
    active = false;
    fresh = false;
    spawnAge = 0f;
    gems.clear();
  }

  /** Permanently ends this encounter's drops. */
  public void stop() {
    stopped = true;
    clear();
  }

  public int getPickupCount() {
    return gems.size();
  }

  /** Returns independent positions so callers cannot move live drops through the list. */
  public List<Vector2> getPickupPositions() {
    return gems.stream().map(gem -> gem.position.cpy()).toList();
  }

  @Override
  public void update() {
    if (encounterEnded()) {
      stop();
      return;
    }
    float delta = time == null ? 0f : time.getDeltaTime();
    if (!active || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    // Stage activation earlier in this frame must not consume the frame that just ended.
    if (fresh) {
      fresh = false;
      return;
    }
    for (int i = gems.size() - 1; i >= 0; i--) {
      Gem gem = gems.get(i);
      gem.age += delta;
      if (gem.age >= config.shieldGemLifetime) {
        gems.remove(i);
      } else if (gem.age >= SPAWN_DURATION && canCollect(gem)) {
        SnakeShieldComponent shield = entity.getComponent(SnakeShieldComponent.class);
        if (shield != null) {
          gems.remove(i);
          shield.refill();
          healIfLowHealth();
        }
      }
    }
    spawnAge += delta;
    if (spawnAge >= config.shieldGemSpawnInterval) {
      // At most one spawn after a long frame; missed intervals never pile up.
      spawnAge = 0f;
      spawnGem();
    }
  }

  private boolean canCollect(Gem gem) {
    Vector2 centre = target.getCenterPosition();
    return centre.dst2(gem.position) <= PICKUP_RADIUS * PICKUP_RADIUS
        && SnakePoisonCollision.wallFraction(centre, gem.position, 0f, null) > 1f;
  }

  private void healIfLowHealth() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats == null || stats.isDead() || stats.getMaxHealth() <= 0) {
      return;
    }
    int health = stats.getHealth();
    int maximum = stats.getMaxHealth();
    if (health >= maximum * config.shieldGemHealThreshold) {
      return;
    }
    int restored = Math.max(1, Math.round(maximum * config.shieldGemHealFraction));
    int cap = Math.min(maximum, (int) Math.floor(maximum * config.shieldGemHealCap));
    int result = (int) Math.min(cap, (long) health + restored);
    if (result > health) {
      stats.setHealth(result);
    }
  }

  private void spawnGem() {
    if (gems.size() >= config.shieldGemMaxActive) {
      return;
    }
    Vector2 playerCentre = target.getCenterPosition();
    Vector2 scale = target.getScale();
    float clearance = Math.max(scale.x, scale.y) * 0.5f;
    Camera currentCamera = camera == null ? ServiceLocator.getWorldCamera() : camera;
    Rectangle visibleArea =
        SnakeGemSpawnArea.visibleArea(
                currentCamera, bounds, Math.max(clearance, SPAWN_SIZE / 2f + 0.25f))
            .orElse(null);
    if (visibleArea == null) {
      return;
    }
    for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
      Vector2 candidate =
          SnakeGemSpawnArea.sample(random, visibleArea, playerCentre, preferNearby, attempt);
      if (visibleArea.contains(candidate)
          && (attempt >= PLACEMENT_ATTEMPTS / 2
              || SnakeGemSpawnArea.inPreferredBand(candidate, playerCentre, preferNearby))
          && candidate.dst2(playerCentre) >= 1.25f * 1.25f
          && candidate.dst2(entity.getCenterPosition()) >= 1f
          && gems.stream().noneMatch(gem -> gem.position.dst2(candidate) < 1f)
          && SnakePoisonCollision.wallFraction(candidate, candidate, clearance, bounds) > 1f
          && SnakePoisonCollision.wallFraction(playerCentre, candidate, 0f, null) > 1f) {
        gems.add(new Gem(candidate));
        preferNearby = !preferNearby;
        return;
      }
    }
  }

  private boolean encounterEnded() {
    return stopped || isDead(entity) || isDead(target);
  }

  private static boolean isDead(Entity entity) {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  // Gems keep their original green colour while the Snake is hidden, frozen or glowing.
  @Override
  public void render(SpriteBatch batch) {
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (encounterEnded()) {
      stop();
      return;
    }
    if (gems.isEmpty()) {
      return;
    }
    ensureFrames();
    float previousColour = batch.getPackedColor();
    try {
      for (Gem gem : gems) {
        float fade = Math.min(1f, Math.max(0f, config.shieldGemLifetime - gem.age));
        if (gem.age < SPAWN_DURATION) {
          batch.setColor(1f, 1f, 1f, fade);
          int frameIndex = Math.min(SPAWN_FRAME_COUNT - 1, (int) (gem.age / SPAWN_FRAME_DURATION));
          batch.draw(
              spawnFrames[frameIndex],
              gem.position.x - SPAWN_SIZE / 2f,
              gem.position.y - SPAWN_SIZE / 2f,
              SPAWN_SIZE,
              SPAWN_SIZE);
        }
        batch.setColor(1f, 1f, 1f, fade * Math.min(1f, gem.age / SPAWN_DURATION));
        TextureRegion frame = frames[(int) (gem.age / FRAME_DURATION) % frames.length];
        batch.draw(
            frame,
            gem.position.x - VISUAL_WIDTH / 2f,
            gem.position.y - VISUAL_HEIGHT / 2f,
            VISUAL_WIDTH,
            VISUAL_HEIGHT);
      }
    } finally {
      batch.setPackedColor(previousColour);
    }
  }

  private void ensureFrames() {
    if (frames != null) {
      return;
    }
    Texture gemTexture = ServiceLocator.getResourceService().getAsset(GEM_TEXTURE, Texture.class);
    Texture spawnTexture =
        ServiceLocator.getResourceService().getAsset(SPAWN_TEXTURE, Texture.class);
    frames = new TextureRegion[10];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new TextureRegion(gemTexture, i * 18, 0, 18, 30);
    }
    spawnFrames = new TextureRegion[SPAWN_FRAME_COUNT];
    // Second row from the top: seven 64px green frames, followed by four empty cells.
    for (int i = 0; i < spawnFrames.length; i++) {
      spawnFrames[i] = new TextureRegion(spawnTexture, i * 64, 64, 64, 64);
    }
  }

  @Override
  public float getZIndex() {
    return Float.POSITIVE_INFINITY;
  }

  @Override
  public void dispose() {
    stop();
    frames = null;
    spawnFrames = null;
    super.dispose();
  }

  private static final class Gem {
    private final Vector2 position;
    private float age;

    private Gem(Vector2 position) {
      this.position = position.cpy();
    }
  }
}
