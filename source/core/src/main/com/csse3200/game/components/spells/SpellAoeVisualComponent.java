package com.csse3200.game.components.spells;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws the disc a spell just covered, centred on the caster and fading out over a moment.
 *
 * <p>One of these serves every spell on the caster: each cast says what colour and how far it
 * reached, so the component never names a spell and a new spell needs nothing added here. A cast
 * during another cast's fade simply replaces it, which is what the player expects to see.
 */
public class SpellAoeVisualComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 128;

  /** Fraction of the radius at which the solid rim starts; inside that is a faint wash. */
  private static final float RIM_START = 0.84f;

  private static final float FILL_ALPHA = 0.3f;
  private static final float RIM_ALPHA = 0.9f;
  private static final float FADE_SECONDS = 0.55f;

  private Texture disc;
  private Color colour;
  private float radius;
  private float remaining;

  /**
   * Shows the area a cast just covered.
   *
   * @param colour colour of the disc; treated as read only
   * @param radius radius in world units, matching the area the spell actually hit. A radius of 0
   *     draws nothing, which is what an unbounded targeting strategy should show.
   */
  public void show(Color colour, float radius) {
    if (radius <= 0f) {
      return;
    }
    this.colour = colour;
    this.radius = radius;
    this.remaining = FADE_SECONDS;
  }

  @Override
  public void update() {
    if (remaining <= 0f) {
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      remaining = Math.max(0f, remaining - delta);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (remaining <= 0f) {
      return;
    }
    if (disc == null) {
      disc = createDisc();
    }

    Vector2 centre = entity.getCenterPosition();
    float size = radius * 2f;
    float fade = remaining / FADE_SECONDS;

    // Set rather than multiply: the caster's own tint, e.g. its damage flash, is not this spell's.
    float original = batch.getPackedColor();
    try {
      batch.setColor(colour.r, colour.g, colour.b, colour.a * fade);
      batch.draw(disc, centre.x - radius, centre.y - radius, size, size);
    } finally {
      batch.setPackedColor(original);
    }
  }

  /** A white disc, faint inside with a solid rim, tinted by whatever colour the cast asked for. */
  private static Texture createDisc() {
    return RadialTextureFactory.create(
        TEXTURE_SIZE, distance -> distance < RIM_START ? FILL_ALPHA : RIM_ALPHA);
  }

  /** Behind every sprite in the layer, so the area reads as being painted on the floor. */
  @Override
  public float getZIndex() {
    return -Float.MAX_VALUE;
  }

  @Override
  public void dispose() {
    if (disc != null) {
      disc.dispose();
      disc = null;
    }
    super.dispose();
  }
}
