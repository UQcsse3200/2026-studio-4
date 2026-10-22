package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * Draws a piece of furniture or an ornament with three-quarter-view depth sorting.
 *
 * <ul>
 *   <li><b>Elevation</b> lifts the sprite (e.g. a book onto a table top) without changing where it
 *       sorts: it is ordered by the tile it stands on, just in front of what it rests on.
 *   <li><b>Floor coverings</b> (rugs) always sort beneath every standing entity.
 *   <li><b>Stretchable</b> sprites are drawn as left cap, repeated middle, right cap, so one table
 *       can be any length.
 * </ul>
 *
 * The entity's position is the bottom-left of the tile the prop stands on; its scale is the drawn
 * size. Elevation is applied at draw time only.
 */
public class PropRenderComponent extends RenderComponent {
  /** Z for floor coverings. Lower z draws first, so rugs draw before any standing entity. */
  public static final float FLOOR_Z = -1_000_000f;

  /** Per-unit-of-elevation nudge so stacked props draw in order (table, glow, book). */
  private static final float ELEVATION_SORT_NUDGE = 0.01f;

  private final TextureRegion region;
  private final TextureRegion[] slices;
  private final float elevation;
  private final boolean floorCovering;

  /**
   * @param region the sprite
   * @param elevation height above the floor to draw at, in world units
   * @param floorCovering whether this lies flat on the floor
   */
  public PropRenderComponent(TextureRegion region, float elevation, boolean floorCovering) {
    this.region = region;
    this.slices = null;
    this.elevation = elevation;
    this.floorCovering = floorCovering;
  }

  /**
   * A horizontally stretchable prop.
   *
   * @param region the whole sprite: left cap, middle section, right cap
   * @param capWidth width of each cap (and of the repeated middle) in texture pixels
   * @param elevation height above the floor to draw at, in world units
   */
  public PropRenderComponent(TextureRegion region, int capWidth, float elevation) {
    this.region = region;
    int height = region.getRegionHeight();
    int right = region.getRegionWidth() - capWidth;
    this.slices =
        new TextureRegion[] {
          new TextureRegion(region, 0, 0, capWidth, height),
          new TextureRegion(region, capWidth, 0, capWidth, height),
          new TextureRegion(region, right, 0, capWidth, height)
        };
    this.elevation = elevation;
    this.floorCovering = false;
  }

  public float getElevation() {
    return elevation;
  }

  @Override
  public float getZIndex() {
    if (floorCovering) {
      return FLOOR_Z;
    }
    return super.getZIndex() + elevation * ELEVATION_SORT_NUDGE;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float y = position.y + elevation;
    if (slices == null) {
      batch.draw(region, position.x, y, scale.x, scale.y);
      return;
    }
    // Each slice is drawn at the same world size as the caps.
    float sliceWorldWidth = scale.y * slices[0].getRegionWidth() / slices[0].getRegionHeight();
    int count = Math.max(2, Math.round(scale.x / sliceWorldWidth));
    for (int i = 0; i < count; i++) {
      TextureRegion slice;
      if (i == 0) {
        slice = slices[0];
      } else if (i == count - 1) {
        slice = slices[2];
      } else {
        slice = slices[1];
      }
      batch.draw(slice, position.x + i * sliceWorldWidth, y, sliceWorldWidth, scale.y);
    }
  }
}
