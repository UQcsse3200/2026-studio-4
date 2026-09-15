package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Stage 3 sprites and floor effects; owns only the shared pixel used to draw geometry. */
public class FinalBossStageThreeVisualComponent extends RenderComponent {
  private static final float STATUE_SIZE = 1.6f;
  private static final int RING_SEGMENTS = 96;
  private FinalBossStageThreeComponent stage;
  private FinalBossPhaseControllerComponent phases;
  private TextureRegion[] idle;
  private TextureRegion[] run;
  private TextureRegion[] cast;
  private TextureRegion[] hit;
  private TextureRegion[] blue;
  private TextureRegion[] grey;
  private TextureRegion[] iceStart;
  private TextureRegion[] iceLoop;
  private TextureRegion[] iceHit;
  private TextureRegion[] freezeStart;
  private TextureRegion[] freezeLoop;
  private TextureRegion[] freezeEnd;
  private TextureRegion[] grandpa;
  private TextureRegion[] transform;
  private TextureRegion[] shield;
  private TextureRegion[] impact;
  private TextureRegion statue;
  private Texture pixel;
  private final float[] quadVertices = new float[20];
  private float elapsed;
  private RenderComponent groundEffects;
  private RenderComponent overlay;
  private final List<RenderComponent> statueRenderers = new ArrayList<>();

  @Override
  public void create() {
    stage = entity.getComponent(FinalBossStageThreeComponent.class);
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    idle = FinalBossStageThreeAssets.sheet("idle", 22, 28, 3, 0, 3);
    run = FinalBossStageThreeAssets.sheet("run", 22, 28, 4, 0, 4);
    cast = FinalBossStageThreeAssets.sheet("cast", 23, 28, 4, 0, 4);
    hit = FinalBossStageThreeAssets.sheet("hit", 43, 38, 2, 0, 2);
    blue = FinalBossStageThreeAssets.sheet("burst", 64, 64, 12, 24, 12);
    grey = FinalBossStageThreeAssets.sheet("burst", 64, 64, 12, 60, 12);
    iceStart = FinalBossStageThreeAssets.sheet("ice-start", 48, 32, 3, 0, 3);
    iceLoop = FinalBossStageThreeAssets.sheet("ice-loop", 48, 32, 10, 0, 10);
    iceHit = FinalBossStageThreeAssets.sheet("ice-hit", 48, 32, 8, 0, 8);
    freezeStart = FinalBossStageThreeAssets.sheet("freeze-start", 32, 32, 9, 0, 9);
    freezeLoop = FinalBossStageThreeAssets.sheet("freeze-loop", 32, 32, 8, 0, 8);
    freezeEnd = FinalBossStageThreeAssets.sheet("freeze-end", 32, 32, 18, 0, 18);
    statue = FinalBossStageThreeAssets.sheet("statue", 1024, 1024, 1, 0, 1)[0];
    grandpa = FinalBossVisualAssets.GRANDPA.loadFrames();
    transform = FinalBossVisualAssets.TRANSFORM.loadFrames();
    shield = FinalBossVisualAssets.SHIELD.loadFrames();
    impact = FinalBossVisualAssets.SHIELD_HIT.loadFrames();
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    try {
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      pixel = new Texture(pixmap);
    } finally {
      pixmap.dispose();
    }
    super.create();
    groundEffects =
        new RenderComponent() {
          @Override
          public float getZIndex() {
            // Terrain is layer 0; these effects are first in the actors' layer 1.
            return -Float.MAX_VALUE;
          }

          @Override
          protected void draw(SpriteBatch batch) {
            drawGroundEffects(batch);
          }
        };
    groundEffects.setEntity(entity);
    groundEffects.create();
    overlay =
        new RenderComponent() {
          @Override
          public float getZIndex() {
            return Float.MAX_VALUE;
          }

          @Override
          protected void draw(SpriteBatch batch) {
            drawOverlay(batch);
          }
        };
    overlay.setEntity(entity);
    overlay.create();
  }

  @Override
  public void update() {
    if (ServiceLocator.getTimeSource() == null) return;
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) elapsed += delta;
    while (statueRenderers.size() < stage.statues.size()) {
      FinalBossStageThreeComponent.Statue item = stage.statues.get(statueRenderers.size());
      RenderComponent renderer =
          new RenderComponent() {
            @Override
            public float getZIndex() {
              // Sort by the statue's feet, even while its sprite is airborne.
              return -item.entity.getPosition().y;
            }

            @Override
            protected void draw(SpriteBatch batch) {
              drawStatue(batch, item);
            }
          };
      renderer.setEntity(entity);
      renderer.create();
      statueRenderers.add(renderer);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (phases.getCurrentPhase() != FinalBossPhase.STAGE_THREE
        && phases.getCurrentPhase() != FinalBossPhase.DEFEATED) return;
    float colour = batch.getPackedColor();
    try {
      drawBoss(batch);
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void drawOverlay(SpriteBatch batch) {
    if (phases.getCurrentPhase() != FinalBossPhase.STAGE_THREE
        && phases.getCurrentPhase() != FinalBossPhase.DEFEATED) return;
    float colour = batch.getPackedColor();
    try {
      drawBolts(batch);
      drawPlayerEffects(batch);
      batch.setPackedColor(colour);
      for (FinalBossStageThreeComponent.Burst burst : stage.bursts) {
        TextureRegion frame =
            FinalBossStageThreeAssets.frame(
                burst.ice ? iceHit : (burst.grey ? grey : blue),
                burst.elapsed,
                stage.config.disappearanceDuration,
                false);
        centred(batch, frame, burst.position, 2.4f, 2.4f);
      }
      if (stage.getState() == FinalBossStageThreeState.ENDING) {
        batch.setColor(Color.WHITE);
        centred(
            batch,
            FinalBossStageThreeAssets.frame(
                transform, stage.getStateTime(), stage.config.returnTransformDuration, false),
            entity.getCenterPosition(),
            2.4f,
            2.4f);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  @Override
  public void dispose() {
    if (groundEffects != null) groundEffects.dispose();
    if (overlay != null) overlay.dispose();
    for (RenderComponent renderer : statueRenderers) renderer.dispose();
    statueRenderers.clear();
    if (pixel != null) {
      pixel.dispose();
      pixel = null;
    }
    super.dispose();
  }

  private void drawBoss(SpriteBatch batch) {
    FinalBossStageThreeState state = stage.getState();
    if (state == FinalBossStageThreeState.WAVE_TWO) return;
    Vector2 centre = entity.getCenterPosition();
    if (state == FinalBossStageThreeState.PEACEFUL || state == FinalBossStageThreeState.ENDING) {
      if (state == FinalBossStageThreeState.PEACEFUL
          || stage.getStateTime() >= stage.config.returnTransformDuration * 0.5f) {
        centred(batch, grandpa[1 + (int) (elapsed / 0.3f) % 2], centre, 1.6f, 1.6f);
      }
      return;
    }
    TextureRegion body = selectBossFrame(state);
    if (stage.hitRemaining > 0f) batch.setColor(1f, 0.45f, 0.45f, 1f);
    boolean left = stage.getTarget().getCenterPosition().x < centre.x;
    // Preserve source aspect ratio rather than stretching the narrow wizard.
    float height = 1.8f;
    float width = height * body.getRegionWidth() / body.getRegionHeight();
    batch.draw(
        body,
        left ? centre.x + width / 2f : centre.x - width / 2f,
        centre.y - height / 2f,
        left ? -width : width,
        height);
    batch.setColor(1f, 1f, 1f, 1f);
    if (stage.hitRemaining > 0f) {
      centred(
          batch,
          FinalBossStageThreeAssets.frame(blue, 0.24f - stage.hitRemaining, 0.24f, false),
          centre,
          1.8f,
          1.8f);
    }
    if (state == FinalBossStageThreeState.CHARGING || phases.isTransitioning())
      drawShield(batch, centre);
  }

  private TextureRegion selectBossFrame(FinalBossStageThreeState state) {
    if (stage.hitRemaining > 0f)
      return FinalBossStageThreeAssets.frame(hit, 0.24f - stage.hitRemaining, 0.24f, false);
    if (stage.castRemaining > 0f || state == FinalBossStageThreeState.CHARGING)
      return FinalBossStageThreeAssets.frame(cast, elapsed, 0.6f, true);
    com.csse3200.game.physics.components.PhysicsComponent physics =
        entity.getComponent(com.csse3200.game.physics.components.PhysicsComponent.class);
    TextureRegion[] frames = physics.getBody().getLinearVelocity().len2() > 0.05f ? run : idle;
    return FinalBossStageThreeAssets.frame(frames, elapsed, 0.7f, true);
  }

  private void drawShield(SpriteBatch batch, Vector2 centre) {
    boolean struck = stage.shieldHitRemaining > 0f;
    batch.setColor(1f, 1f, 1f, struck ? 1f : 0.7f + 0.2f * MathUtils.sin(elapsed * 5f));
    centred(
        batch,
        FinalBossStageThreeAssets.frame(shield, elapsed, 0.96f, true),
        centre,
        struck ? 2.3f : 2f,
        struck ? 2.3f : 2f);
    batch.setColor(1f, 1f, 1f, 1f);
    if (struck)
      centred(
          batch,
          FinalBossStageThreeAssets.frame(impact, 0.48f - stage.shieldHitRemaining, 0.48f, false),
          centre,
          3f,
          3f);
  }

  private void drawStatue(SpriteBatch batch, FinalBossStageThreeComponent.Statue item) {
    FinalBossStageThreeState state = stage.getState();
    if (state != FinalBossStageThreeState.WAVE_TWO || item.broken) return;
    float colour = batch.getPackedColor();
    try {
      Vector2 centre = item.entity.getCenterPosition();
      boolean warning = item.warningRemaining > 0f;
      float width = warning ? STATUE_SIZE * 1.08f : STATUE_SIZE;
      float height = warning ? STATUE_SIZE * 0.88f : STATUE_SIZE;
      float bottom = centre.y + item.jumpHeight - STATUE_SIZE / 2f;
      if (item.hitRemaining > 0f) batch.setColor(1f, 0.5f, 0.5f, 1f);
      else if (warning) batch.setColor(1f, 0.82f, 0.5f, 1f);
      else batch.setColor(Color.WHITE);
      batch.draw(statue, centre.x - width / 2f, bottom, width, height);
      drawStatueHealth(batch, item, centre.x, bottom + height + 0.14f);
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void drawStatueHealth(
      SpriteBatch batch, FinalBossStageThreeComponent.Statue item, float x, float y) {
    float width = 1.2f;
    float health = MathUtils.clamp((float) item.hitsRemaining / stage.config.statueHits, 0f, 1f);
    batch.setColor(0.06f, 0.06f, 0.06f, 1f);
    batch.draw(pixel, x - width / 2f - 0.04f, y - 0.04f, width + 0.08f, 0.22f);
    batch.setColor(0.32f, 0.32f, 0.32f, 1f);
    batch.draw(pixel, x - width / 2f, y, width, 0.14f);
    batch.setColor(0.45f, 0.88f, 0.48f, 1f);
    batch.draw(pixel, x - width / 2f, y, width * health, 0.14f);
  }

  private void drawGroundEffects(SpriteBatch batch) {
    if (phases.getCurrentPhase() != FinalBossPhase.STAGE_THREE
        || stage.getState() != FinalBossStageThreeState.WAVE_TWO) return;
    float colour = batch.getPackedColor();
    try {
      for (FinalBossStageThreeComponent.Shockwave wave : stage.shockwaves) {
        float fade = MathUtils.clamp((wave.maxRadius - wave.radius) / 0.5f, 0f, 1f);
        batch.setColor(0.7f, 0.7f, 0.7f, fade);
        drawRing(batch, wave.position, wave.radius, stage.config.shockwaveWidth + 0.08f);
        batch.setColor(0f, 0f, 0f, fade);
        drawRing(batch, wave.position, wave.radius, stage.config.shockwaveWidth);
      }
      for (FinalBossStageThreeComponent.Statue item : stage.statues) {
        if (item.broken) continue;
        Vector2 ground = FinalBossStageThreeComponent.groundPosition(item.entity);
        if (item.warningRemaining > 0f) {
          float pulse = 0.22f + 0.1f * MathUtils.sin(elapsed * 18f);
          batch.setColor(1f, 0.72f, 0.22f, pulse);
          drawEllipse(batch, ground.x, ground.y, 0.86f, 0.29f);
        }
        batch.setColor(0f, 0f, 0f, 0.3f);
        drawEllipse(batch, ground.x, ground.y, 0.58f / (1f + item.jumpHeight * 0.12f), 0.16f);
      }
      PlayerActions playerActions = stage.getTarget().getComponent(PlayerActions.class);
      if (playerActions != null && playerActions.isJumpEnabled()) {
        Vector2 ground = FinalBossStageThreeComponent.groundPosition(stage.getTarget());
        batch.setColor(0f, 0f, 0f, 0.3f);
        drawEllipse(
            batch, ground.x, ground.y, 0.4f / (1f + playerActions.getJumpHeight() * 0.18f), 0.12f);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  /** Draws a hollow annulus with solid quads, without allocating textures or vertices per frame. */
  private void drawRing(SpriteBatch batch, Vector2 centre, float radius, float width) {
    float inner = Math.max(0f, radius - width / 2f);
    float outer = radius + width / 2f;
    for (int i = 0; i < RING_SEGMENTS; i++) {
      float angle = MathUtils.PI2 * i / RING_SEGMENTS;
      float nextAngle = MathUtils.PI2 * (i + 1) / RING_SEGMENTS;
      float cos = MathUtils.cos(angle);
      float sin = MathUtils.sin(angle);
      float nextCos = MathUtils.cos(nextAngle);
      float nextSin = MathUtils.sin(nextAngle);
      drawQuad(
          batch,
          centre.x + inner * cos,
          centre.y + inner * sin,
          centre.x + outer * cos,
          centre.y + outer * sin,
          centre.x + outer * nextCos,
          centre.y + outer * nextSin,
          centre.x + inner * nextCos,
          centre.y + inner * nextSin);
    }
  }

  private void drawEllipse(SpriteBatch batch, float x, float y, float radiusX, float radiusY) {
    for (int i = 0; i < 24; i++) {
      float angle = MathUtils.PI2 * i / 24f;
      float nextAngle = MathUtils.PI2 * (i + 1) / 24f;
      drawQuad(
          batch,
          x,
          y,
          x + radiusX * MathUtils.cos(angle),
          y + radiusY * MathUtils.sin(angle),
          x + radiusX * MathUtils.cos(nextAngle),
          y + radiusY * MathUtils.sin(nextAngle),
          x,
          y);
    }
  }

  private void drawQuad(
      SpriteBatch batch,
      float x1,
      float y1,
      float x2,
      float y2,
      float x3,
      float y3,
      float x4,
      float y4) {
    float colour = batch.getPackedColor();
    setVertex(0, x1, y1, colour);
    setVertex(5, x2, y2, colour);
    setVertex(10, x3, y3, colour);
    setVertex(15, x4, y4, colour);
    batch.draw(pixel, quadVertices, 0, quadVertices.length);
  }

  private void setVertex(int offset, float x, float y, float colour) {
    quadVertices[offset] = x;
    quadVertices[offset + 1] = y;
    quadVertices[offset + 2] = colour;
    quadVertices[offset + 3] = 0.5f;
    quadVertices[offset + 4] = 0.5f;
  }

  private void drawBolts(SpriteBatch batch) {
    for (FinalBossStageThreeComponent.Bolt bolt : stage.bolts) {
      TextureRegion frame =
          bolt.elapsed < 0.12f
              ? FinalBossStageThreeAssets.frame(iceStart, bolt.elapsed, 0.12f, false)
              : FinalBossStageThreeAssets.frame(iceLoop, bolt.elapsed - 0.12f, 0.6f, true);
      float size = 0.9f;
      batch.draw(
          frame,
          bolt.position.x - size / 2f,
          bolt.position.y - size / 2f,
          size / 2f,
          size / 2f,
          size,
          size,
          1f,
          1f,
          bolt.velocity.angleDeg());
    }
  }

  private void drawPlayerEffects(SpriteBatch batch) {
    Vector2 centre = stage.getTarget().getCenterPosition();
    if (stage.isFrozen() || stage.thawRemaining > 0f) {
      TextureRegion frame;
      if (!stage.isFrozen())
        frame =
            FinalBossStageThreeAssets.frame(freezeEnd, 0.55f - stage.thawRemaining, 0.55f, false);
      else if (stage.freezeElapsed < 0.18f)
        frame = FinalBossStageThreeAssets.frame(freezeStart, stage.freezeElapsed, 0.18f, false);
      else frame = FinalBossStageThreeAssets.frame(freezeLoop, stage.freezeElapsed, 0.48f, true);
      centred(batch, frame, centre, 1.8f, 1.8f);
    }
    if (stage.playerHitRemaining > 0f)
      centred(
          batch,
          FinalBossStageThreeAssets.frame(blue, 0.24f - stage.playerHitRemaining, 0.24f, false),
          centre,
          1.5f,
          1.5f);
  }

  private static void centred(
      SpriteBatch batch, TextureRegion frame, Vector2 centre, float width, float height) {
    batch.draw(frame, centre.x - width / 2f, centre.y - height / 2f, width, height);
  }
}
