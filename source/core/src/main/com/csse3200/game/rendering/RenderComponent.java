package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
  private boolean repeatPass;

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
    } else {
      // SpriteBatch exposes its mutable Color, so preserve channel values, not the reference.
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

    Color glow = StatusEffectsControllerComponent.getGlow(source);
    if (glow != null) {
      drawGlow(batch, glow);
    }
  }

  /**
   * Lays the glow over the sprite already drawn, by drawing it a second time additively.
   *
   * <p>A tint alone leaves a dark sprite dark, because multiplying black by a colour is still
   * black, so an effect on something like a near-black enemy would be invisible. Adding light on
   * top instead shows on any sprite, and the sprite's own alpha keeps the glow to its silhouette.
   */
  private void drawGlow(SpriteBatch batch, Color glow) {
    int blendSrc = batch.getBlendSrcFunc();
    int blendDst = batch.getBlendDstFunc();
    Color color = batch.getColor();
    float r = color.r;
    float g = color.g;
    float b = color.b;
    float a = color.a;
    repeatPass = true;
    try {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      batch.setColor(glow.r, glow.g, glow.b, glow.a * a);
      draw(batch);
    } finally {
      repeatPass = false;
      batch.setColor(r, g, b, a);
      batch.setBlendFunction(blendSrc, blendDst);
    }
  }

  /**
   * Returns whether this is the glow pass redrawing the same frame rather than a new one.
   *
   * <p>Subclasses whose {@code draw} advances something once per frame, such as an animation's
   * playhead, must not advance it again on the repeat.
   */
  protected boolean isRepeatPass() {
    return repeatPass;
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
