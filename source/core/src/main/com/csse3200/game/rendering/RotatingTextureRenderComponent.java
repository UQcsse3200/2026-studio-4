package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/**
 * Renders a texture that can be rotated and sized independently of its entity.
 *
 * <p>{@link TextureRenderComponent} draws axis-aligned from the entity's bottom-left corner at the
 * entity's own scale. That suits terrain and fixtures, but not a weapon sprite: an attack hitbox is
 * sized for collision, not for looking right, and a swinging blade has to point where it swings.
 *
 * <p>This component solves both. The sprite is drawn centred on {@link
 * com.csse3200.game.entities.Entity#getCenterPosition()} so it pivots about its middle rather than
 * orbiting a corner, at {@link #setVisualScale(Vector2)} if one is set and the entity's scale
 * otherwise, turned by {@link #setRotation(float)} plus a fixed {@link #setRotationOffset(float)}.
 *
 * <p>The offset exists because sprite sheets often draw weapons as inventory icons at some resting
 * angle. Correct that here rather than rotating the source image: rotating pixel art off-axis
 * resamples it and destroys the clean edges, whereas this rotation happens on the GPU at draw time.
 */
public class RotatingTextureRenderComponent extends RenderComponent {
  private final TextureRegion textureRegion;
  private Vector2 visualScale;
  private final Vector2 visualOffset = new Vector2();
  private float rotationDeg;
  private float rotationOffsetDeg;

  /**
   * @param texturePath internal path of the texture to render
   */
  public RotatingTextureRenderComponent(String texturePath) {
    this(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
  }

  /**
   * @param texture texture to render
   */
  public RotatingTextureRenderComponent(Texture texture) {
    this(new TextureRegion(texture));
  }

  /**
   * @param textureRegion region to render; may span a whole texture or a sheet cell
   */
  public RotatingTextureRenderComponent(TextureRegion textureRegion) {
    this.textureRegion = textureRegion;
  }

  /**
   * Draw size in metres, independent of the entity's scale. Use this to keep a sprite's aspect
   * ratio when the entity is sized for collision rather than for display.
   *
   * @param visualScale width and height in metres, or null to follow the entity's scale
   */
  public void setVisualScale(Vector2 visualScale) {
    this.visualScale = visualScale == null ? null : visualScale.cpy();
  }

  /**
   * @return copy of the draw size, or null when following the entity's scale
   */
  public Vector2 getVisualScale() {
    return visualScale == null ? null : visualScale.cpy();
  }

  /**
   * Shifts the sprite in the frame that {@link #setRotation(float)} defines, where +x points along
   * the facing direction. Use it to anchor a held weapon: a swept blade wants its handle near the
   * wielder, not its midpoint on the arc.
   *
   * @param visualOffset offset in metres, relative to the facing direction
   */
  public void setVisualOffset(Vector2 visualOffset) {
    this.visualOffset.set(visualOffset == null ? Vector2.Zero : visualOffset);
  }

  /**
   * @return copy of the facing-relative offset
   */
  public Vector2 getVisualOffset() {
    return visualOffset.cpy();
  }

  /**
   * Facing of the sprite, before {@link #setRotationOffset(float)} is applied. Safe to call every
   * frame, e.g. from a sweep or a projectile.
   *
   * @param rotationDeg rotation in degrees, counter-clockwise
   */
  public void setRotation(float rotationDeg) {
    this.rotationDeg = rotationDeg;
  }

  /**
   * @return rotation in degrees, excluding the offset
   */
  public float getRotation() {
    return rotationDeg;
  }

  /**
   * Constant correction for a sprite that is not drawn pointing right at 0 degrees.
   *
   * @param rotationOffsetDeg degrees added to every rotation
   */
  public void setRotationOffset(float rotationOffsetDeg) {
    this.rotationOffsetDeg = rotationOffsetDeg;
  }

  /**
   * @return the constant rotation correction in degrees
   */
  public float getRotationOffset() {
    return rotationOffsetDeg;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The offset is rotated by the facing only: the sprite offset merely corrects how the
    // artwork was drawn and must not swing the anchor point around with it.
    Vector2 centre = entity.getCenterPosition().add(visualOffset.cpy().rotateDeg(rotationDeg));
    Vector2 size = visualScale == null ? entity.getScale() : visualScale;
    float halfWidth = size.x / 2f;
    float halfHeight = size.y / 2f;
    batch.draw(
        textureRegion,
        centre.x - halfWidth,
        centre.y - halfHeight,
        halfWidth,
        halfHeight,
        size.x,
        size.y,
        1f,
        1f,
        rotationDeg + rotationOffsetDeg);
  }
}
