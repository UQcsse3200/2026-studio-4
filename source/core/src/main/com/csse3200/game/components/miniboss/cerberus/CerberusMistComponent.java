package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Owns the left head's mist area, visual and debuff integration events. */
public class CerberusMistComponent extends RenderComponent {
  public static final String ENTERED = "cerberusMistEntered";
  public static final String EXITED = "cerberusMistExited";

  private static final float COOLDOWN = 5f;
  private static final float DURATION = 3f;
  private static final float CAST_RANGE = 7f;
  private static final float RADIUS = 2f;

  private final Entity target;
  private final Vector2 centre = new Vector2();

  private CombatStatsComponent stats;
  private CombatStatsComponent targetStats;
  private float cooldownReamining = COOLDOWN;
  private float remaining;
  private boolean active;
  private boolean inside;
  private boolean stopped;
  private Texture mistTexture;

  public CerberusMistComponent(Entity target) {
    this.target = target;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("entityDied", this::stop);
    super.create();
  }

  @Override
  public void update() {
    if (stopped || stats.isDead()) {
      stop();
      return;
    }

    if (targetStats == null || targetStats.isDead()) {
      clearMist();
      cooldownReamining = COOLDOWN;
      return;
    }

    float delta = Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime());

    if (active) {
      remaining -= delta;
      if (remaining <= 0f) {
        clearMist();
        cooldownReamining = COOLDOWN;
        return;
      }

      updateOccupancy();
      return;
    }

    cooldownReamining = Math.max(0f, cooldownReamining - delta);
    if (cooldownReamining > 0f
        || entity.getCenterPosition().dst2(target.getCenterPosition()) > CAST_RANGE * CAST_RANGE) {
      return;
    }

    centre.set(target.getCenterPosition());
    remaining = DURATION;
    active = true;
    updateOccupancy();
  }

  private void updateOccupancy() {
    boolean nowInside = target.getCenterPosition().dst2(centre) <= RADIUS * RADIUS;

    if (nowInside == inside) {
      return;
    }

    inside = nowInside;
    target.getEvents().trigger(inside ? ENTERED : EXITED, entity);
  }

  private void clearMist() {
    active = false;
    remaining = 0f;

    if (inside) {
      inside = false;
      target.getEvents().trigger(EXITED, entity);
    }
  }

  private void stop() {
    stopped = true;
    clearMist();
  }

  public boolean isMistActive() {
    return active;
  }

  public Vector2 getMistCentre() {
    return centre.cpy();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!active) {
      return;
    }

    if (mistTexture == null) {
      createMistTexture();
    }

    float originalColour = batch.getPackedColor();
    try {
      float elapsed = DURATION - remaining;
      float opacity = 0.85f + 0.15f * (float) Math.sin(elapsed * 5f);
      batch.setColor(1f, 1f, 1f, opacity);
      batch.draw(mistTexture, centre.x - RADIUS, centre.y - RADIUS, RADIUS * 2f, RADIUS * 2f);
    } finally {
      batch.setPackedColor(originalColour);
    }
  }

  private void createMistTexture() {
    int size = 128;
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    try {
      pixmap.setBlending(Pixmap.Blending.None);

      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          float dx = (x + 0.5f - size / 2f) / (size / 2f);
          float dy = (y + 0.5f - size / 2f) / (size / 2f);
          float distance = (float) Math.sqrt(dx * dx + dy * dy);
          if (distance > 1f) {
            continue;
          }

          float alpha = 0.25f + 0.3f * (1f - distance);
          if (distance > 0.94f) {
            alpha = 0.65f;
          }

          pixmap.setColor(0.4f, 0.12f, 0.55f, alpha);
          pixmap.drawPixel(x, y);
        }
      }

      mistTexture = new Texture(pixmap);
      mistTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    } finally {
      pixmap.dispose();
    }
  }

  @Override
  public float getZIndex() {
    return Float.NEGATIVE_INFINITY;
  }

  @Override
  public void dispose() {
    stop();

    if (mistTexture != null) {
      mistTexture.dispose();
      mistTexture = null;
    }

    super.dispose();
  }
}
