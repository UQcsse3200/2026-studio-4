package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Keeps explosion playback alive after damage and wave-removal events have fired. */
public class FinalBossSummonVisualComponent extends RenderComponent {
  private static final float SPAWN_DURATION = 0.6f;
  private static final float EXPLOSION_DURATION = 0.32f;
  private TextureRegion[] idle;
  private TextureRegion[] explosion;
  private TextureRegion[] spawn;
  private FinalBossExplosiveSummonComponent explosive;
  private float elapsed;
  private float warningElapsed;
  private float explosionElapsed;
  private boolean exploded;
  private boolean disposalScheduled;

  @Override
  public void create() {
    idle = FinalBossVisualAssets.BOMB_IDLE.loadFrames();
    explosion = FinalBossVisualAssets.BOMB_EXPLOSION.loadFrames();
    spawn = FinalBossVisualAssets.SPAWN.loadFrames();
    // Ignore the idle sheet's transparent padding so the visible bomb fills its collider area.
    for (int i = 0; i < idle.length; i++) {
      idle[i] = new TextureRegion(idle[i], 12, 22, 27, 28);
    }
    explosive = entity.getComponent(FinalBossExplosiveSummonComponent.class);
    entity.getEvents().addListener(FinalBossEvents.SUMMON_EXPLODED, this::onExplosion);
    super.create();
  }

  private void onExplosion() {
    if (!exploded) {
      exploded = true;
      explosionElapsed = 0f;
    }
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    elapsed += delta;
    if (exploded) {
      explosionElapsed += delta;
      if (explosionElapsed >= EXPLOSION_DURATION && !disposalScheduled) {
        disposalScheduled = true;
        ServiceLocator.getEntityService().scheduleDisposal(entity);
      }
    } else if (explosive.isWarningActive()) {
      warningElapsed += delta;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 pos = entity.getPosition();
    Vector2 size = entity.getScale();
    if (exploded || explosive.isWarningActive()) {
      // Frames 0-8 are the warning; only frames 9-10 are the actual blast.
      // Immediate detonations therefore never acquire a new warning delay.
      int frame =
          exploded
              ? 9 + FinalBossVisualAssets.once(explosionElapsed, EXPLOSION_DURATION, 2)
              : FinalBossVisualAssets.once(warningElapsed, explosive.getWarningDuration(), 9);
      float pixelWidth = size.x / 27f;
      float pixelHeight = size.y / 28f;
      batch.draw(
          explosion[frame], pos.x - 12f * pixelWidth, pos.y, 50f * pixelWidth, 50f * pixelHeight);
    } else {
      batch.draw(idle[(int) (elapsed / 0.16f) % idle.length], pos.x, pos.y, size.x, size.y);
    }
    if (!exploded && elapsed < SPAWN_DURATION) {
      batch.draw(
          spawn[FinalBossVisualAssets.once(elapsed, SPAWN_DURATION, spawn.length)],
          pos.x - size.x * 0.2f,
          pos.y - size.y * 0.2f,
          size.x * 1.4f,
          size.y * 1.4f);
    }
  }
}
