package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Red silhouettes and comic speed strokes behind a minotaur's actual dash movement. */
public class MinotaurDashTrailComponent extends RenderComponent {
  private static final float SAMPLE_INTERVAL = 0.06f;
  private static final float LIFETIME = 0.24f;
  private static final float PHYSICS_STEP_GRACE = 0.032f;
  private static final int MAX_GHOSTS = 4;
  private static final float MOVEMENT_EPSILON = 0.000001f;
  private static final float RED_GREEN = 53f / 255f;
  private static final float RED_BLUE = 76f / 255f;
  // Entity updates and rendering share the render thread. Copies are shared between minotaurs,
  // but belong to this effect, never to the resource service's original atlas.
  private static final Map<Texture, Silhouette> SILHOUETTES = new IdentityHashMap<>();
  private final Map<Texture, Silhouette> ownedSilhouettes = new IdentityHashMap<>();
  private final List<Ghost> ghosts = new ArrayList<>();
  private final Vector2 lastPosition = new Vector2();
  private final Vector2 direction = new Vector2();
  private final float[] vertices = new float[20];
  private AnimationRenderComponent body;
  private MinotaurAttackRenderComponent attack;
  private CombatStatsComponent stats;
  private PhysicsComponent physics;
  private PhysicsMovementComponent movement;
  private Ghost previousPose;
  private float sampleElapsed;
  private float stationaryElapsed;
  private boolean dashing;
  private boolean dead;
  private boolean disposed;

  @Override
  public void create() {
    super.create();
    body = entity.getComponent(AnimationRenderComponent.class);
    attack = entity.getComponent(MinotaurAttackRenderComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    physics = entity.getComponent(PhysicsComponent.class);
    movement = entity.getComponent(PhysicsMovementComponent.class);
    entity.getEvents().addListener("lungeDashStart", this::startDash);
    for (String event :
        new String[] {
          "lungeDashEnd",
          "lungeTelegraphStart",
          "wanderStart",
          "chaseStart",
          "patrolStart",
          "default"
        }) {
      entity.getEvents().addListener(event, this::stopDash);
    }
    for (String event : new String[] {"dieAnimation", "enemyDeathAnimation", "entityDied"}) {
      entity.getEvents().addListener(event, this::die);
    }
  }

  private void startDash() {
    if (disposed || dead) return;
    dashing = true;
    resetHistory();
  }

  private void stopDash() {
    dashing = false;
    resetHistory();
  }

  private void die() {
    dead = true;
    stopDash();
  }

  private void resetHistory() {
    ghosts.clear();
    direction.setZero();
    sampleElapsed = 0f;
    stationaryElapsed = 0f;
    lastPosition.set(entity.getPosition());
    previousPose = null;
  }

  private boolean canDraw() {
    return dashing
        && !disposed
        && !dead
        && (stats == null || !stats.isDead())
        && !StatusEffectsControllerComponent.isImmobilised(entity)
        && (attack == null || attack.getCurrentAnimation() == null)
        && body != null
        && body.getCurrentAnimation() != null
        && (movement == null || movement.getMoving());
  }

  @Override
  public void update() {
    // Sample after AI and physics components have settled this update's position and state.
    ServiceLocator.getEntityService().runAfterUpdate(this::sampleMovement);
  }

  private void sampleMovement() {
    if (!canDraw()) {
      resetHistory();
      return;
    }
    TextureRegion frame = body.getCurrentFrame();
    if (frame == null) {
      resetHistory();
      return;
    }
    float delta = Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime());
    Vector2 position = entity.getPosition();
    Vector2 displacement = position.cpy().sub(lastPosition);
    float distanceSquared = displacement.len2();
    // At the normal dash speed (6 units/s), even a long update is continuous. A jump well
    // beyond that is a teleport and must not leave a streak across the room.
    float maxDistance = Math.max(1f, 12f * delta);
    if (distanceSquared > maxDistance * maxDistance) {
      resetHistory();
      return;
    }
    if (distanceSquared <= MOVEMENT_EPSILON) {
      // Physics takes fixed steps. At a higher render rate a moving body can have an unchanged
      // position for one update; age its history without inventing an extra snapshot.
      // Desired velocity can stay nonzero against a wall, so allow at most two physics steps.
      stationaryElapsed += delta;
      if (stationaryElapsed > PHYSICS_STEP_GRACE
          || physics == null
          || physics.getBody().getLinearVelocity().len2() <= MOVEMENT_EPSILON) {
        resetHistory();
      } else {
        ageGhosts(delta);
        sampleElapsed += delta;
      }
      return;
    }
    stationaryElapsed = 0f;
    displacement.nor();
    if (direction.dot(displacement) < 0f) {
      ghosts.clear();
      previousPose = null;
    }
    direction.set(displacement);
    ageGhosts(delta);
    sampleElapsed += delta;
    if (sampleElapsed + MOVEMENT_EPSILON >= SAMPLE_INTERVAL) {
      addGhost(frame, position);
      sampleElapsed %= SAMPLE_INTERVAL;
    }
    previousPose = capturePose(frame);
    lastPosition.set(position);
  }

  private void addGhost(TextureRegion frame, Vector2 position) {
    Ghost snapshot = previousPose == null ? capturePose(frame) : previousPose;
    // On the first moving update the current pose was also the last visible pose.
    if (previousPose == null) {
      snapshot.x = lastPosition.x;
      snapshot.y += lastPosition.y - position.y;
    }
    ghosts.add(snapshot);
    if (ghosts.size() > MAX_GHOSTS) ghosts.remove(0);
  }

  private void ageGhosts(float delta) {
    for (Ghost ghost : ghosts) ghost.age += delta;
    ghosts.removeIf(ghost -> ghost.age >= LIFETIME);
  }

  private Ghost capturePose(TextureRegion frame) {
    Silhouette silhouette = ownedSilhouettes.get(frame.getTexture());
    if (silhouette == null) {
      silhouette = SILHOUETTES.computeIfAbsent(frame.getTexture(), Silhouette::new);
      silhouette.references++;
      ownedSilhouettes.put(frame.getTexture(), silhouette);
    }
    // Copy UVs verbatim, including any atlas flip, then compose the body's own facing.
    TextureRegion region = new TextureRegion(silhouette.texture);
    region.setRegion(frame.getU(), frame.getV(), frame.getU2(), frame.getV2());
    region.flip(body.isFlipX(), body.isFlipY());
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    return new Ghost(
        region, silhouette, position.x, position.y + body.getVerticalOffset(), scale.x, scale.y);
  }

  @Override
  public void render(SpriteBatch batch) {
    // This fixed red effect has its own alpha. It must neither inherit body tints nor run an
    // additive status-glow pass (which would also change the caller's blend state).
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!canDraw() || ghosts.isEmpty()) return;
    Color color = batch.getColor();
    float r = color.r;
    float g = color.g;
    float b = color.b;
    float a = color.a;
    try {
      for (Ghost ghost : ghosts) {
        float alpha = 0.54f * (float) Math.pow(1f - ghost.age / LIFETIME, 1.1);
        batch.setColor(1f, RED_GREEN, RED_BLUE, alpha);
        batch.draw(ghost.region, ghost.x, ghost.y, ghost.width, ghost.height);
      }
      drawSpeedStrokes(batch, ghosts.get(ghosts.size() - 1).silhouette);
    } finally {
      batch.setColor(r, g, b, a);
    }
  }

  private void drawSpeedStrokes(SpriteBatch batch, Silhouette silhouette) {
    Vector2 scale = entity.getScale();
    Vector2 centre = entity.getCenterPosition().add(0f, body.getVerticalOffset());
    for (Stroke stroke : STROKES) {
      drawStroke(batch, silhouette, centre, scale, stroke);
    }
  }

  // Distances are fractions of the source 148x130 frame, so strokes follow entity scale.
  private static final Stroke[] STROKES = {
    new Stroke(-0.26f, 0.20f, 38f, 1f, 0.62f, false),
    new Stroke(-0.13f, 0.30f, 47.5f, 2f, 0.62f, false),
    new Stroke(0f, 0.20f, 57f, 1f, 0.62f, false),
    new Stroke(0.13f, 0.30f, 66.5f, 2f, 0.62f, false),
    new Stroke(0.26f, 0.20f, 76f, 1f, 0.62f, false),
    new Stroke(-0.24f, 0.12f, 67f, 4f, 0.30f, true),
    new Stroke(0f, 0.18f, 83f, 5f, 0.36f, true),
    new Stroke(0.24f, 0.24f, 99f, 6f, 0.42f, true)
  };

  private record Stroke(
      float side, float head, float length, float width, float alpha, boolean wedge) {}

  private void drawStroke(
      SpriteBatch batch, Silhouette silhouette, Vector2 centre, Vector2 scale, Stroke stroke) {
    float px = -direction.y;
    float py = direction.x;
    float hx = centre.x - direction.x * stroke.head * scale.x + px * stroke.side * scale.y;
    float hy = centre.y - direction.y * stroke.head * scale.x + py * stroke.side * scale.y;
    float length = stroke.length / 148f * scale.x;
    float tx = hx - direction.x * length;
    float ty = hy - direction.y * length;
    float halfWidth = stroke.width / 130f * scale.y * 0.5f;
    float packed = Color.toFloatBits(1f, RED_GREEN, RED_BLUE, stroke.alpha);
    // A wedge uses a duplicated head vertex: SpriteBatch's two triangles form one tapered line.
    vertex(0, tx + px * halfWidth, ty + py * halfWidth, packed, silhouette);
    vertex(5, tx - px * halfWidth, ty - py * halfWidth, packed, silhouette);
    vertex(
        10,
        hx - (stroke.wedge ? 0f : px * halfWidth),
        hy - (stroke.wedge ? 0f : py * halfWidth),
        packed,
        silhouette);
    vertex(
        15,
        hx + (stroke.wedge ? 0f : px * halfWidth),
        hy + (stroke.wedge ? 0f : py * halfWidth),
        packed,
        silhouette);
    batch.draw(silhouette.texture, vertices, 0, vertices.length);
  }

  private void vertex(int offset, float x, float y, float color, Silhouette silhouette) {
    vertices[offset] = x;
    vertices[offset + 1] = y;
    vertices[offset + 2] = color;
    vertices[offset + 3] = silhouette.u;
    vertices[offset + 4] = silhouette.v;
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.1f;
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    stopDash();
    for (Map.Entry<Texture, Silhouette> entry : ownedSilhouettes.entrySet()) {
      Silhouette silhouette = entry.getValue();
      if (--silhouette.references == 0) {
        SILHOUETTES.remove(entry.getKey());
        silhouette.texture.dispose();
      }
    }
    ownedSilhouettes.clear();
    super.dispose();
  }

  private static final class Ghost {
    final TextureRegion region;
    final Silhouette silhouette;
    final float width;
    final float height;
    float x;
    float y;
    float age;

    Ghost(
        TextureRegion region, Silhouette silhouette, float x, float y, float width, float height) {
      this.region = region;
      this.silhouette = silhouette;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
  }

  private static final class Silhouette {
    final Texture texture;
    float u;
    float v;
    int references;

    Silhouette(Texture source) {
      TextureData data = source.getTextureData();
      if (!data.isPrepared()) data.prepare();
      Pixmap original = data.consumePixmap();
      Pixmap white = new Pixmap(original.getWidth(), original.getHeight(), Pixmap.Format.RGBA8888);
      white.setBlending(Pixmap.Blending.None);
      try {
        for (int y = 0; y < original.getHeight(); y++) {
          for (int x = 0; x < original.getWidth(); x++) {
            int alpha = original.getPixel(x, y) & 0xff;
            white.drawPixel(x, y, 0xffffff00 | alpha);
            if (alpha == 255) {
              u = (x + 0.5f) / white.getWidth();
              v = (y + 0.5f) / white.getHeight();
            }
          }
        }
        texture = new Texture(white);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      } finally {
        white.dispose();
        if (data.disposePixmap()) original.dispose();
      }
    }
  }
}
