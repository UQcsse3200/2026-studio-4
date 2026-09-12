package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * A generic component for rendering an entity. Registers itself with the render service in order to
 * be rendered each frame. Child classes can implement different kinds of rendering behaviour.
 */
public abstract class RenderComponent extends Component implements Renderable, Disposable {
  private static final int DEFAULT_LAYER = 1;
  private Entity visualSource;

  /** Use another entity's current player ability appearance; null uses this entity. */
  public void setVisualSource(Entity visualSource) {
    this.visualSource = visualSource;
  }

  @Override
  public void create() {
    ServiceLocator.getRenderService().register(this);
  }

  @Override
  public void dispose() {
    ServiceLocator.getRenderService().unregister(this);
  }

  @Override
  public void render(SpriteBatch batch) {
    // Effects say how they look; the renderer never asks which ability, or whose, they are.
    Entity source = visualSource == null ? entity : visualSource;
    Color tint = StatusEffectsControllerComponent.getTint(source);
    if (tint == null) {
      draw(batch);
      return;
    }

    // SpriteBatch exposes its mutable Color, so preserve channel values rather than the reference.
    Color color = batch.getColor();
    float r = color.r;
    float g = color.g;
    float b = color.b;
    float a = color.a;
    try {
      batch.setColor(r * tint.r, g * tint.g, b * tint.b, a * tint.a);
      draw(batch);
    } finally {
      batch.setColor(r, g, b, a);
    }
  }

  @Override
  public int compareTo(Renderable o) {
    return Float.compare(getZIndex(), o.getZIndex());
  }

  @Override
  public int getLayer() {
    return DEFAULT_LAYER;
  }

  @Override
  public float getZIndex() {
    // The smaller the Y value, the higher the Z index, so that closer entities are drawn in front
    return -entity.getPosition().y;
  }

  /**
   * Draw the renderable. Should be called only by the renderer, not manually.
   *
   * @param batch Batch to render to.
   */
  protected abstract void draw(SpriteBatch batch);
}
