package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/** Render a triggered texture. Changes on given triggers */
public class TriggeredRenderComponent extends RenderComponent {
  private Texture texture;

  /**
   * @param texturePath Internal path of initial texture to render. Will be scaled to the entity's
   *     scale.
   */
  public TriggeredRenderComponent(String texturePath) {
    this(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
  }

  // ...
  /**
   * @param texture Initial texture to render. Will be scaled to the entity's scale.
   */
  public TriggeredRenderComponent(Texture texture) {
    this.texture = texture;
  }

  /**
   * @param texturePath Internal path of texture to change to on trigger. Will be scaled for entity.
   * @param trigger The trigger on which to change to texture. Listens for this trigger then changes
   *     to given texture.
   * @param argNum The number of arguments passed with this trigger.
   */
  public void addTexture(String texturePath, String trigger, int argNum) {
    addTexture(
        ServiceLocator.getResourceService().getAsset(texturePath, Texture.class), trigger, argNum);
  }

  /**
   * @param texture Texture to change to on trigger. Will be scaled for entity.
   * @param trigger The trigger on which to change to texture. Listens for this trigger then changes
   *     to given texture.
   * @param argNum The number of arguments passed with this trigger. Will cause an error if you pass
   *     the wrong number of arguments.
   */
  public void addTexture(Texture texture, String trigger, int argNum) {
    if (argNum == 1) {
      entity.getEvents().addListener(trigger, (n) -> changeTexture(texture));
    } else if (argNum == 2) {
      entity.getEvents().addListener(trigger, (n, m) -> changeTexture(texture));
    } else if (argNum == 3) {
      entity.getEvents().addListener(trigger, (n, m, o) -> changeTexture(texture));
    } else if (argNum == 0) {
      entity.getEvents().addListener(trigger, () -> changeTexture(texture));
    }
  }

  /**
   * Changes texture to given texture.
   *
   * @param texture The texture to change to.
   */
  private void changeTexture(Texture texture) {
    if (this.texture != texture) {
      this.texture = texture;
    }
  }

  /** Scale the entity to a width of 1 and a height matching the texture's ratio */
  public void scaleEntity() {
    entity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    batch.draw(texture, position.x, position.y, scale.x, scale.y);
  }
}
