package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.PlayerPetrificationComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Plays stone impacts at the marked location and a cue while the player is actually slowed. */
public class FinalBossPetrificationVisualComponent extends RenderComponent {
  private static final float IMPACT_DURATION = 0.32f;
  private final Entity target;
  private final FinalBossStageOneConfig config;
  private final Vector2 markedPosition = new Vector2();
  private TextureRegion[] stones;
  private Texture ring;
  private float impactRemaining;
  private PlayerPetrificationComponent petrification;

  public FinalBossPetrificationVisualComponent(Entity target, FinalBossStageOneConfig config) {
    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    petrification = target.getComponent(PlayerPetrificationComponent.class);
    stones = FinalBossVisualAssets.STONE.loadFrames();
    entity.getEvents().addListener(FinalBossEvents.PETRIFICATION_WARNING, this::onWarning);
    entity.getEvents().addListener(FinalBossEvents.PETRIFICATION_HIT, this::onHit);
    entity.getEvents().addListener(FinalBossEvents.PETRIFICATION_MISSED, this::onMiss);
    entity.getEvents().addListener(FinalBossEvents.STAGE_ONE_STATE_CHANGED, this::onStateChanged);
    super.create();
  }

  private void onWarning(Vector2 position, Float radius, Float duration) {
    markedPosition.set(position);
  }

  private void onHit(Entity player) {
    impactRemaining = IMPACT_DURATION;
  }

  private void onMiss(Vector2 position) {
    markedPosition.set(position);
    impactRemaining = IMPACT_DURATION;
  }

  private void onStateChanged(FinalBossStageOneState state) {
    if (state != FinalBossStageOneState.WAVE_TWO) {
      impactRemaining = 0f;
    }
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      impactRemaining = Math.max(0f, impactRemaining - delta);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (petrification != null && petrification.isPetrified()) {
      ensureRing();
      Vector2 feet = target.getPosition();
      Vector2 size = target.getScale();
      batch.draw(ring, feet.x, feet.y - size.y * 0.08f, size.x, size.y * 0.3f);
    }
    if (impactRemaining > 0f && stones != null && stones.length > 0) {
      int frame =
          FinalBossVisualAssets.once(
              IMPACT_DURATION - impactRemaining, IMPACT_DURATION, stones.length);
      float size = config.petrificationRadius * 3f;
      // The stone artwork grows from the bottom of each cell.
      batch.draw(
          stones[frame],
          markedPosition.x - size * 0.5f,
          markedPosition.y - target.getScale().y * 0.5f,
          size,
          size);
    }
  }

  @Override
  public float getZIndex() {
    return -target.getPosition().y + 0.01f;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (ring != null) {
      ring.dispose();
      ring = null;
    }
  }

  private void ensureRing() {
    if (ring != null) {
      return;
    }
    Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
    try {
      pixmap.setBlending(Pixmap.Blending.None);
      pixmap.setColor(0.65f, 0.65f, 0.7f, 0.8f);
      pixmap.fillCircle(16, 16, 14);
      pixmap.setColor(0f, 0f, 0f, 0f);
      pixmap.fillCircle(16, 16, 11);
      ring = new Texture(pixmap);
    } finally {
      pixmap.dispose();
    }
  }
}
