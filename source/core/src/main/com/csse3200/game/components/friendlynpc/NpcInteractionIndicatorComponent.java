package com.csse3200.game.components.friendlynpc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws a soft, bobbing marker above an NPC's head while the player can interact with it.
 *
 * <p>Visibility follows NpcInteractionEvents INDICATOR_SHOWN / NpcInteractionEvents
 * INDICATOR_HIDDEN}, which NpcInteractableComponent fires on the same entity. The marker texture is
 * generated, so no art asset is required.
 */
public class NpcInteractionIndicatorComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 32;
  private static final float SIZE = 0.3f;
  private static final float GAP_ABOVE_HEAD = 0.12f;
  private static final float BOB_HEIGHT = 0.06f;
  private static final float BOB_SPEED = 4f;
  private static final Color COLOUR = new Color(1f, 0.85f, 0.3f, 0.95f);

  private Texture texture;
  private boolean visible;
  private float elapsed;

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(NpcInteractionEvents.INDICATOR_SHOWN, () -> visible = true);
    entity.getEvents().addListener(NpcInteractionEvents.INDICATOR_HIDDEN, () -> visible = false);
  }

  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time != null) {
      elapsed += time.getDeltaTime();
    }
  }

  public boolean isVisible() {
    return visible;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!visible) {
      return;
    }
    if (texture == null) {
      // Created on first draw so a GL context is guaranteed to exist
      texture = RadialTextureFactory.create(TEXTURE_SIZE, d -> d < 0.55f ? 1f : (1f - d) / 0.45f);
    }
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float bob = MathUtils.sin(elapsed * BOB_SPEED) * BOB_HEIGHT;
    float x = position.x + (scale.x - SIZE) / 2f;
    float y = position.y + scale.y + GAP_ABOVE_HEAD + bob;

    Color previous = batch.getColor();
    float r = previous.r;
    float g = previous.g;
    float b = previous.b;
    float a = previous.a;
    try {
      batch.setColor(r * COLOUR.r, g * COLOUR.g, b * COLOUR.b, a * COLOUR.a);
      batch.draw(texture, x, y, SIZE, SIZE);
    } finally {
      batch.setColor(r, g, b, a);
    }
  }

  @Override
  public float getZIndex() {
    // always in front of the NPC sprite it sits above
    return super.getZIndex() + 0.001f;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
  }
}
