package com.csse3200.game.components.tutorial;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Draws an icon bobbing gently over the middle of an entity, e.g. the item on a supply stand. */
public class FloatingIconComponent extends RenderComponent {
  private static final float ICON_SIZE = 0.7f;
  private static final float BASE_HEIGHT = 0.5f;
  private static final float BOB_HEIGHT = 0.04f;
  private static final float BOB_SPEED = 3f;

  private final String texturePath;
  private Texture texture;
  private float elapsed;

  /**
   * @param texturePath asset path of the icon, already loaded by the resource service
   */
  public FloatingIconComponent(String texturePath) {
    this.texturePath = texturePath;
  }

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(texturePath, Texture.class);
  }

  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time != null) {
      elapsed += time.getDeltaTime();
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null) {
      return;
    }
    Vector2 position = entity.getPosition();
    float x = position.x + (entity.getScale().x - ICON_SIZE) / 2f;
    float y = position.y + BASE_HEIGHT + MathUtils.sin(elapsed * BOB_SPEED) * BOB_HEIGHT;
    batch.draw(texture, x, y, ICON_SIZE, ICON_SIZE);
  }

  @Override
  public float getZIndex() {
    // in front of the stand it hovers over
    return super.getZIndex() - 0.001f;
  }
}
