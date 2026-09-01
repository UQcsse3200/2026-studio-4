package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/** Render a static texture. */
public class TextureRenderComponent extends RenderComponent {
  private final Texture texture;
  private final TextureRegion textureRegion;

  /**
   * @param texturePath Internal path of static texture to render. Will be scaled to the entity's
   *     scale.
   */
  public TextureRenderComponent(String texturePath) {
    this(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
  }

  // ...
  /**
   * @param texture Static texture to render. Will be scaled to the entity's scale.
   */
  public TextureRenderComponent(Texture texture) {
    this.texture = texture;
    textureRegion = new TextureRegion(texture);
  }

  /** Creates a renderer for a region from a texture sheet. */
  public TextureRenderComponent(TextureRegion textureRegion) {
    texture = null;
    this.textureRegion = textureRegion;
  }

  /**
   * @return the region being rendered; spans the whole texture when one was given directly
   */
  public TextureRegion getTextureRegion() {
    return textureRegion;
  }

  /** Scale the entity to a width of 1 and a height matching the texture's ratio */
  public void scaleEntity() {
    entity.setScale(1f, (float) textureRegion.getRegionHeight() / textureRegion.getRegionWidth());
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    if (texture == null) {
      batch.draw(textureRegion, position.x, position.y, scale.x, scale.y);
    } else {
      batch.draw(texture, position.x, position.y, scale.x, scale.y);
    }
  }
}
