package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;

/** Draws the brief orange flash of an ignited oil puddle. */
public class FireBurstRenderComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 64;
  private Texture texture;

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null) {
      texture = RadialTextureFactory.create(TEXTURE_SIZE, distance -> 1f - distance);
    }
    float oldColour = batch.getPackedColor();
    try {
      batch.setColor(1f, 0.3f, 0.02f, 0.9f);
      batch.draw(
          texture,
          entity.getPosition().x,
          entity.getPosition().y,
          entity.getScale().x,
          entity.getScale().y);
    } finally {
      batch.setPackedColor(oldColour);
    }
  }

  @Override
  public void dispose() {
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    super.dispose();
  }
}
