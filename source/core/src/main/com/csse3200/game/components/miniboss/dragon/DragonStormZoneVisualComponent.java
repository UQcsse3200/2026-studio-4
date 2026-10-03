package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;

/** Draws storm warnings and a brief flash when each zone strikes. */
public class DragonStormZoneVisualComponent extends RenderComponent {
  private DragonStormZoneComponent storm;
  private CombatStatsComponent stats;
  private Texture circle;
  private boolean disposed;

  @Override
  public void create() {
    storm = entity.getComponent(DragonStormZoneComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);

    if (storm == null || stats == null) {
      throw new IllegalStateException("Storm visuals require storm and combat stats components");
    }

    super.create();
  }

  /** Whether there are living storm zones to display. */
  public boolean isEffectVisible() {
    return !disposed
        && stats != null
        && !stats.isDead()
        && storm.isBusy()
        && !storm.getZones().isEmpty();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isEffectVisible()) {
      return;
    }

    if (circle == null) {
      circle = RadialTextureFactory.create(64, distance -> distance > 0.85f ? 1f : 0.2f);
    }

    float originalColour = batch.getPackedColor();
    try {
      for (DragonStormZoneComponent.ZoneView zone : storm.getZones()) {
        drawZone(batch, zone);
      }
    } finally {
      batch.setPackedColor(originalColour);
    }
  }

  private void drawZone(SpriteBatch batch, DragonStormZoneComponent.ZoneView zone) {
    float radius = DragonStormZoneComponent.RADIUS;

    if (zone.struck()) {
      batch.setColor(0.85f, 0.95f, 1f, 1f);
      drawCircle(batch, zone.x(), zone.y(), radius);
      return;
    }

    batch.setColor(0.3f, 0.7f, 1f, 0.75f);
    drawCircle(batch, zone.x(), zone.y(), radius);

    float progress = Math.clamp(zone.progress(), 0f, 1f);
    if (progress > 0f) {
      batch.setColor(0.65f, 0.9f, 1f, 0.85f);
      drawCircle(batch, zone.x(), zone.y(), radius * progress);
    }
  }

  private void drawCircle(SpriteBatch batch, float x, float y, float radius) {
    batch.draw(circle, x - radius, y - radius, radius * 2f, radius * 2f);
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.03f;
  }

  @Override
  public void dispose() {
    disposed = true;

    if (circle != null) {
      circle.dispose();
      circle = null;
    }

    super.dispose();
  }
}
