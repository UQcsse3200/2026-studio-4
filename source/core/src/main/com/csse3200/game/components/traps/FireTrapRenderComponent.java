package com.csse3200.game.components.traps;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Floor-level flame jet using the artist's 18x59 frames at 100 ms per frame. */
public class FireTrapRenderComponent extends RenderComponent {
  public static final String START_TEXTURE = "images/traps/fire-start.png";
  public static final String LOOP_TEXTURE = "images/traps/fire-loop.png";
  public static final String END_TEXTURE = "images/traps/fire-end.png";
  private static final float FRAME_SECONDS = 0.1f;
  private static final float SEQUENCE_SECONDS = 0.4f;
  private final TextureRegion[] ignition = frames(START_TEXTURE);
  private final TextureRegion[] burning = frames(LOOP_TEXTURE);
  private final TextureRegion[] extinguishing = frames(END_TEXTURE);
  private float elapsed;
  private float triggeredElapsed;
  private boolean triggered;

  private static TextureRegion[] frames(String path) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return TextureRegion.split(texture, 18, 59)[0];
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("trapTriggered", this::onTriggered);
  }

  private void onTriggered() {
    if (!triggered) {
      triggered = true;
      triggeredElapsed = 0f;
    }
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    elapsed += delta;
    if (triggered) {
      triggeredElapsed += delta;
    }
  }

  /** Null once the consumed trap has finished extinguishing. */
  TextureRegion currentFrame() {
    if (triggered && triggeredElapsed >= 0.8f) {
      float ending = triggeredElapsed - 0.8f;
      if (ending >= SEQUENCE_SECONDS) {
        return null;
      }
      return extinguishing[Math.min(3, (int) (ending / FRAME_SECONDS))];
    }
    if (elapsed < SEQUENCE_SECONDS) {
      return ignition[Math.min(3, (int) (elapsed / FRAME_SECONDS))];
    }
    return burning[(int) ((elapsed - SEQUENCE_SECONDS) / FRAME_SECONDS) % 4];
  }

  @Override
  protected void draw(SpriteBatch batch) {
    TextureRegion frame = currentFrame();
    if (frame == null) {
      return;
    }
    Vector2 position = entity.getPosition();
    // Preserve native pixel density and anchor the flame to its one-tile sensor.
    // The tall transparent canvas must not determine the collision footprint.
    float pixelSize = entity.getScale().x / 16f;
    batch.draw(frame, position.x - pixelSize, position.y, 18 * pixelSize, 59 * pixelSize);
  }

  @Override
  public int getLayer() {
    return 0; // Terrain layer: beneath characters (layer 1).
  }

  @Override
  public float getZIndex() {
    return 1f; // After terrain, whose z-index is 0.
  }
}
