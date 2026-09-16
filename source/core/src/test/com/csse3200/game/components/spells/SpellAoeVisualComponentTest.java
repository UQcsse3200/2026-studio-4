package com.csse3200.game.components.spells;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link SpellAoeVisualComponent}: the disc showing the area a cast just covered. */
@ExtendWith(GameExtension.class)
class SpellAoeVisualComponentTest {
  private static final Color PURPLE = new Color(0.55f, 0.25f, 0.85f, 0.85f);

  private GameTime time;
  private Entity caster;
  private SpellAoeVisualComponent visual;
  private SpriteBatch batch;
  private Color drawColour;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));

    visual = new SpellAoeVisualComponent();
    caster = new Entity().addComponent(visual);
    caster.setPosition(10f, 20f);

    batch = mock(SpriteBatch.class);
    drawColour = new Color();
    Color batchColour = new Color(Color.WHITE);
    when(batch.getColor()).thenReturn(batchColour);
    doAnswer(
            invocation -> {
              batchColour.set(
                  invocation.getArgument(0),
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    doAnswer(
            invocation -> {
              drawColour.set(batchColour);
              return null;
            })
        .when(batch)
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  private void advance(float seconds) {
    when(time.getDeltaTime()).thenReturn(seconds);
    visual.update();
  }

  @Test
  void drawsNothingUntilASpellIsCast() {
    visual.draw(batch);

    verify(batch, never()).draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @Test
  void anIdleDiscDoesNotKeepAskingForTheTime() {
    visual.update();

    verify(time, never()).getDeltaTime();
  }

  @Test
  void drawsTheDiscCentredOnTheCasterAndSizedToTheRadius() {
    visual.show(PURPLE, 3f);

    visual.draw(batch);

    // Entity scale defaults to 1, so its centre sits half a unit past its position.
    verify(batch).draw(any(Texture.class), eq(7.5f), eq(17.5f), eq(6f), eq(6f));
  }

  @Test
  void drawsInTheColourTheCastAskedForRatherThanWhateverTheBatchWasSetTo() {
    visual.show(PURPLE, 3f);

    visual.draw(batch);

    assertEquals(PURPLE.r, drawColour.r);
    assertEquals(PURPLE.g, drawColour.g);
    assertEquals(PURPLE.b, drawColour.b);
  }

  @Test
  void restoresTheBatchColourAfterwardsSoTheNextSpriteIsUnaffected() {
    visual.show(PURPLE, 3f);

    visual.draw(batch);

    verify(batch).setPackedColor(anyFloat());
  }

  @Test
  void fadesOutAndThenStopsDrawingAltogether() {
    visual.show(PURPLE, 3f);

    visual.draw(batch);
    float fullAlpha = drawColour.a;
    advance(0.3f);
    visual.draw(batch);
    float fadedAlpha = drawColour.a;

    assertTrue(fadedAlpha < fullAlpha, "the disc should be fading");
    assertTrue(fadedAlpha > 0f, "but still be on screen partway through");

    advance(0.3f);
    visual.draw(batch);

    verify(batch, times(2))
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @Test
  void aRadiuslessStrategyDrawsNoCircleAtAll() {
    // Targeting everything on screen, say, has no circle that would honestly describe it.
    visual.show(PURPLE, 0f);

    visual.draw(batch);

    verify(batch, never()).draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @Test
  void aSecondCastReplacesTheFadeOfTheFirst() {
    visual.show(PURPLE, 3f);
    advance(0.4f);
    visual.show(Color.CYAN, 5f);

    visual.draw(batch);

    assertEquals(Color.CYAN.r, drawColour.r);
    assertEquals(Color.CYAN.b, drawColour.b);
    verify(batch).draw(any(Texture.class), eq(5.5f), eq(15.5f), eq(10f), eq(10f));
  }

  @Test
  void survivesAFrameWithNoUsableTimeStep() {
    visual.show(PURPLE, 3f);

    when(time.getDeltaTime()).thenReturn(Float.NaN);
    visual.update();
    when(time.getDeltaTime()).thenReturn(-1f);
    visual.update();
    visual.draw(batch);

    assertEquals(PURPLE.a, drawColour.a, 1e-6f, "a bad delta must not eat the fade");
  }

  @Test
  void sitsBehindEverySpriteSoItReadsAsPaintedOnTheFloor() {
    assertEquals(-Float.MAX_VALUE, visual.getZIndex());
  }

  @Test
  void disposingTwiceIsHarmless() {
    visual.show(PURPLE, 3f);
    visual.draw(batch);
    visual.dispose();

    assertDoesNotThrow(visual::dispose, "the disc must not be handed back to GL twice");
  }
}
