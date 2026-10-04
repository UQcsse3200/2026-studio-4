package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns the Snake's straight poison shots and their artwork without spawning physics entities. */
public class SnakePoisonVolleyComponent extends RenderComponent {
  private static final int MAX_PROJECTILES = 128;
  private static final float FORMING_DURATION = 0.2f;
  private static final float FLYING_FRAME_DURATION = 0.08f;
  private static final float FADING_DURATION = 0.28f;
  // The right-facing chase pose's mouth is at (25, 9) from the top-left of its 32px cell.
  private static final float MOUTH_X = 25f / 32f;
  private static final float MOUTH_Y = 23f / 32f;
  private static final float PLAYER_RADIUS_SCALE = 0.3f;
  private final Entity target;
  private final SnakeMiniBossConfig config;
  private final List<Shot> shots = new ArrayList<>();
  private final Vector2 aim = new Vector2(1f, 0f);
  private Rectangle bounds;
  private GameTime time;
  private AnimationRenderComponent animator;
  private SnakePoisonAssets.Frames frames;
  private float facingDegrees;
  private float chargeAge;
  private int nextWave;
  private boolean spitting;
  private boolean stopped;
  private boolean updating;
  private boolean clearRequested;
  private boolean freshCharge;

  public SnakePoisonVolleyComponent(Entity target, SnakeMiniBossConfig config) {
    this.target = Objects.requireNonNull(target);
    this.config = Objects.requireNonNull(config);
  }

  /** Copies room bounds; null leaves projectiles bounded by walls and their lifetime. */
  public void setArenaBounds(Rectangle bounds) {
    this.bounds = bounds == null ? null : new Rectangle(bounds);
  }

  @Override
  public void create() {
    super.create();
    time = ServiceLocator.getTimeSource();
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Faces the player and begins the visible mouth charge before the first volley. */
  public void beginSpit() {
    if (!canFire()) {
      return;
    }
    faceTarget();
    chargeAge = 0f;
    nextWave = 0;
    spitting = true;
    freshCharge = true;
  }

  /**
   * Faces the player and emits one fan. Only the next wave is accepted, preventing duplicate
   * updates from turning the Snake or stacking shots.
   */
  public void fireVolley(int waveIndex) {
    if (!spitting || !canFire() || waveIndex != nextWave || waveIndex >= config.poisonVolleyCount) {
      return;
    }
    nextWave++;
    faceTarget();
    Vector2 mouth = mouthPosition();
    if (SnakePoisonCollision.wallFraction(
            entity.getCenterPosition(), mouth, config.poisonRadius, bounds)
        <= 1f) {
      return;
    }
    int count = Math.min(config.poisonShotsPerVolley, MAX_PROJECTILES - shots.size());
    float offset =
        (waveIndex - (config.poisonVolleyCount - 1) / 2f) * config.poisonWaveOffsetDegrees;
    for (int i = 0; i < count; i++) {
      float fan =
          config.poisonShotsPerVolley <= 1
              ? 0f
              : -config.poisonFanDegrees / 2f
                  + i * config.poisonFanDegrees / (config.poisonShotsPerVolley - 1);
      shots.add(new Shot(mouth, aim.cpy().rotateDeg(offset + fan).scl(config.poisonSpeed)));
    }
  }

  /** Hides the charge while already-fired shots keep moving through the burrow cycle. */
  public void endSpit() {
    spitting = false;
    resetFacing();
  }

  /** Cancels all encounter poison without unloading the room's shared textures. */
  public void clear() {
    endSpit();
    chargeAge = 0f;
    if (updating) {
      clearRequested = true;
    } else {
      shots.clear();
    }
  }

  public int getProjectileCount() {
    return clearRequested ? 0 : shots.size();
  }

  public boolean isSpitting() {
    return spitting;
  }

  @Override
  public void update() {
    if (encounterEnded()) {
      stop();
      return;
    }
    float delta = time == null ? 0f : time.getDeltaTime();
    if (!Float.isFinite(delta)
        || delta <= 0f
        || StatusEffectsControllerComponent.isImmobilised(entity)) {
      return;
    }
    if (spitting && !freshCharge) {
      chargeAge += delta;
    }
    freshCharge = false;
    updating = true;
    try {
      for (int i = shots.size() - 1; i >= 0 && !clearRequested; i--) {
        if (advance(shots.get(i), delta)) {
          shots.remove(i);
        }
      }
    } finally {
      updating = false;
      if (clearRequested) {
        clearRequested = false;
        shots.clear();
      }
    }
  }

  private boolean advance(Shot shot, float delta) {
    // A controller earlier in this entity's update may have emitted this shot just now.
    if (shot.fresh) {
      shot.fresh = false;
      return false;
    }
    if (shot.fading) {
      shot.fadeAge += delta;
      return shot.fadeAge >= FADING_DURATION;
    }
    float formingTime = Math.min(delta, Math.max(0f, FORMING_DURATION - shot.formingAge));
    shot.formingAge += formingTime;
    delta -= formingTime;
    if (delta <= 0f) {
      return false;
    }
    float travelTime = Math.min(delta, Math.max(0f, config.poisonLifetime - shot.age));
    Vector2 end = shot.position.cpy().mulAdd(shot.velocity, travelTime);
    float wall = SnakePoisonCollision.wallFraction(shot.position, end, config.poisonRadius, bounds);
    float player = playerFraction(shot.position, end);
    float collision = Math.min(wall, player);
    shot.age += travelTime;
    if (collision <= 1f) {
      shot.position.lerp(end, collision);
      shot.fading = true;
      if (player < wall) {
        damagePlayer();
      }
    } else {
      shot.position.set(end);
      shot.fading = shot.age >= config.poisonLifetime;
    }
    return false;
  }

  private float playerFraction(Vector2 start, Vector2 end) {
    if (StatusEffectsControllerComponent.isConcealed(target)
        || target.getComponent(CombatStatsComponent.class) == null) {
      return Float.POSITIVE_INFINITY;
    }
    Vector2 scale = target.getScale();
    float radius = Math.min(scale.x, scale.y) * PLAYER_RADIUS_SCALE + config.poisonRadius;
    return SnakePoisonCollision.circleFraction(start, end, target.getCenterPosition(), radius);
  }

  private void damagePlayer() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    int before = stats.getHealth();
    stats.takeDamage(config.poisonDamage, entity);
    if (encounterEnded()) {
      stop();
      return;
    }
    SnakePlayerHitVisualComponent impact = entity.getComponent(SnakePlayerHitVisualComponent.class);
    if (stats.getHealth() < before && impact != null) {
      impact.play();
    }
  }

  private boolean canFire() {
    return time != null
        && !encounterEnded()
        && !StatusEffectsControllerComponent.isImmobilised(entity)
        && !StatusEffectsControllerComponent.isConcealed(target);
  }

  private boolean encounterEnded() {
    return stopped || isDead(entity) || isDead(target);
  }

  private static boolean isDead(Entity entity) {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  private void faceTarget() {
    Vector2 toTarget = target.getCenterPosition().sub(entity.getCenterPosition());
    Vector2 offset = mouthOffset();
    if (!toTarget.isZero()) {
      facingDegrees = toTarget.angleDeg();
      // Account for the mouth sitting above the sprite's centre so its forward ray aims at
      // the player. Inside the head's turning radius, use a stable centre-to-player bearing.
      if (toTarget.len2() > offset.len2()) {
        facingDegrees -= (float) Math.toDegrees(Math.asin(offset.y / toTarget.len()));
      }
    }
    if (animator != null) {
      animator.setRotation(facingDegrees);
    }
    aim.set(target.getCenterPosition()).sub(mouthPosition());
    if (aim.isZero()) {
      aim.set(1f, 0f).rotateDeg(facingDegrees);
    } else {
      aim.nor();
    }
  }

  private Vector2 mouthOffset() {
    Vector2 scale = entity.getScale();
    return new Vector2(scale.x * (MOUTH_X - 0.5f), scale.y * (MOUTH_Y - 0.5f));
  }

  private Vector2 mouthPosition() {
    return entity.getCenterPosition().add(mouthOffset().rotateDeg(facingDegrees));
  }

  private void resetFacing() {
    facingDegrees = 0f;
    if (animator != null) {
      animator.setRotation(0f);
    }
  }

  // Preserve original green artwork rather than inheriting the Snake's tint or glow pass.
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
    if (!spitting && shots.isEmpty()) {
      return;
    }
    if (frames == null) {
      frames = SnakePoisonAssets.load(ServiceLocator.getResourceService());
    }
    float colour = batch.getPackedColor();
    try {
      batch.setColor(1f, 1f, 1f, 1f);
      if (spitting) {
        int frame = Math.min(9, (int) (chargeAge / FORMING_DURATION * 10));
        drawFrame(batch, frames.forming()[frame], mouthPosition(), aim.angleDeg());
      }
      for (Shot shot : shots) {
        drawFrame(batch, shotFrame(shot), shot.position, shot.velocity.angleDeg());
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private TextureRegion shotFrame(Shot shot) {
    if (shot.fading) {
      return frames.fading()[Math.min(7, (int) (shot.fadeAge / FADING_DURATION * 8))];
    }
    if (shot.formingAge < FORMING_DURATION) {
      return frames.forming()[Math.min(9, (int) (shot.formingAge / FORMING_DURATION * 10))];
    }
    return frames.flying()[(int) (shot.age / FLYING_FRAME_DURATION) % 4];
  }

  private void drawFrame(SpriteBatch batch, TextureRegion frame, Vector2 centre, float angle) {
    float size = config.poisonVisualSize;
    // The source projectile points right; positive world angles rotate counterclockwise.
    batch.draw(
        frame,
        centre.x - size / 2f,
        centre.y - size / 2f,
        size / 2f,
        size / 2f,
        size,
        size,
        1f,
        1f,
        angle);
  }

  private void stop() {
    stopped = true;
    clear();
  }

  @Override
  public float getZIndex() {
    return Float.POSITIVE_INFINITY;
  }

  @Override
  public void dispose() {
    stop();
    frames = null;
    super.dispose();
  }

  private static final class Shot {
    private final Vector2 position;
    private final Vector2 velocity;
    private float age;
    private float formingAge;
    private float fadeAge;
    private boolean fading;
    private boolean fresh = true;

    private Shot(Vector2 position, Vector2 velocity) {
      this.position = position.cpy();
      this.velocity = velocity;
    }
  }
}
