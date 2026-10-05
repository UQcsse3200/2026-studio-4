package com.csse3200.game.components.traps;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** One-shot growth and melting of Coloritmic's ice spikes, anchored to the dungeon floor. */
public class IceTrapRenderComponent extends RenderComponent {
  public static final String TEXTURE = "images/traps/ice-spikes.png";
  private static final float FRAME_SECONDS = 0.1f;
  private static final float GROWTH_SECONDS = 1.3f;
  private static final float HOLD_SECONDS = 0.5f;
  private static final float MELT_SECONDS = 0.7f;
  private final TextureRegion[] frames = new TextureRegion[20];
  private boolean triggered;
  private float elapsed;

  public IceTrapRenderComponent() {
    Texture texture = ServiceLocator.getResourceService().getAsset(TEXTURE, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegion[][] sheet = TextureRegion.split(texture, 32, 32);
    for (int i = 0; i < frames.length; i++) {
      frames[i] = sheet[i / 5][i % 5];
    }
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("trapTriggered", () -> triggered = true);
  }

  @Override
  public void update() {
    if (triggered) {
      elapsed += ServiceLocator.getTimeSource().getDeltaTime();
    }
  }

  /** A small visible spike warns of an armed trap; null means the spent trap has melted. */
  TextureRegion currentFrame() {
    if (!triggered) {
      return frames[0];
    }
    if (elapsed < GROWTH_SECONDS) {
      return frames[Math.min(12, (int) (elapsed / FRAME_SECONDS))];
    }
    if (elapsed < GROWTH_SECONDS + HOLD_SECONDS) {
      return frames[12];
    }
    float melting = elapsed - GROWTH_SECONDS - HOLD_SECONDS;
    if (melting >= MELT_SECONDS) {
      return null;
    }
    return frames[13 + Math.min(6, (int) (melting / FRAME_SECONDS))];
  }

  @Override
  protected void draw(SpriteBatch batch) {
    TextureRegion frame = currentFrame();
    if (frame == null) {
      return;
    }
    Vector2 position = entity.getPosition();
    // 32-pixel artwork spans two 16-pixel tiles; the sensor remains one floor tile.
    float tileSize = entity.getScale().x;
    batch.draw(frame, position.x - tileSize / 2f, position.y, tileSize * 2f, tileSize * 2f);
  }

  @Override
  public int getLayer() {
    return 0; // Below characters, matching the fire trap.
  }

  @Override
  public float getZIndex() {
    return 1f; // Above terrain.
  }
}
