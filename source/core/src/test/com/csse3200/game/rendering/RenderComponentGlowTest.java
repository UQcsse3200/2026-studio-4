package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The glow pass: an effect's colour laid on top of the sprite additively.
 *
 * <p>Tinting alone multiplies, and multiplying a near-black sprite by any colour leaves it near
 * black, so a frozen Cerberus looked completely untouched. These pin the second pass that fixes it,
 * and the guarantees the rest of the renderer relies on: one batch state left exactly as found.
 */
@ExtendWith(GameExtension.class)
class RenderComponentGlowTest {
  private GameTime time;
  private SpriteBatch batch;
  private Color batchColour;
  private int blendSrc;
  private int blendDst;

  /** Records the batch state each time it is asked to draw. */
  private class RecordingRender extends RenderComponent {
    final List<Color> drawColours = new ArrayList<>();
    final List<Integer> drawBlendSrc = new ArrayList<>();
    final List<Integer> drawBlendDst = new ArrayList<>();
    final List<Boolean> drawRepeatPass = new ArrayList<>();
    RuntimeException failure;

    @Override
    protected void draw(SpriteBatch drawBatch) {
      drawColours.add(new Color(batchColour));
      drawBlendSrc.add(blendSrc);
      drawBlendDst.add(blendDst);
      drawRepeatPass.add(isRepeatPass());
      if (failure != null) {
        throw failure;
      }
    }
  }

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerRenderService(mock(RenderService.class));

    batchColour = new Color(1f, 1f, 1f, 1f);
    blendSrc = GL20.GL_SRC_ALPHA;
    blendDst = GL20.GL_ONE_MINUS_SRC_ALPHA;
    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(batchColour);
    when(batch.getBlendSrcFunc()).thenAnswer(invocation -> blendSrc);
    when(batch.getBlendDstFunc()).thenAnswer(invocation -> blendDst);
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
              blendSrc = invocation.getArgument(0);
              blendDst = invocation.getArgument(1);
              return null;
            })
        .when(batch)
        .setBlendFunction(anyInt(), anyInt());
  }

  /** A render component on an entity carrying the given effects. */
  private RecordingRender renderOn(StatusEffectsControllerComponent effects) {
    RecordingRender render = new RecordingRender();
    Entity entity =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(effects);
    entity.addComponent(render);
    entity.create();
    return render;
  }

  @Test
  void aSpriteWithNoGlowingEffectIsDrawnOnce() {
    RecordingRender render = renderOn(new StatusEffectsControllerComponent());

    render.render(batch);

    assertEquals(1, render.drawColours.size());
    verify(batch, never()).setBlendFunction(anyInt(), anyInt());
  }

  @Test
  void aGlowingEffectDrawsTheSpriteASecondTimeInItsColour() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    FrozenEffect frozen = new FrozenEffect(time, 5000L);
    effects.addStatusEffect(frozen);

    render.render(batch);

    assertEquals(2, render.drawColours.size(), "one normal pass, then the glow on top");
    Color glowPass = render.drawColours.get(1);
    assertEquals(frozen.getGlow().r, glowPass.r);
    assertEquals(frozen.getGlow().g, glowPass.g);
    assertEquals(frozen.getGlow().b, glowPass.b);
  }

  @Test
  void theGlowPassIsAdditiveSoItShowsOnADarkSprite() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    render.render(batch);

    assertEquals(GL20.GL_SRC_ALPHA, render.drawBlendSrc.get(0));
    assertEquals(
        GL20.GL_ONE_MINUS_SRC_ALPHA, render.drawBlendDst.get(0), "normal pass blends as usual");
    assertEquals(GL20.GL_SRC_ALPHA, render.drawBlendSrc.get(1));
    assertEquals(
        GL20.GL_ONE,
        render.drawBlendDst.get(1),
        "the glow pass adds to what is already there instead of replacing it");
  }

  @Test
  void theGlowPassTellsTheSpriteItIsARedrawOfTheSameFrame() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    render.render(batch);

    assertEquals(List.of(false, true), render.drawRepeatPass);
  }

  @Test
  void leavesTheBatchExactlyAsItFoundIt() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    render.render(batch);

    assertEquals(new Color(1f, 1f, 1f, 1f), batchColour, "every later sprite depends on this");
    assertEquals(GL20.GL_SRC_ALPHA, blendSrc);
    assertEquals(GL20.GL_ONE_MINUS_SRC_ALPHA, blendDst);
  }

  @Test
  void restoresTheBatchEvenWhenTheGlowPassThrows() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    render.failure = new IllegalStateException("draw failed");

    assertThrows(IllegalStateException.class, () -> render.render(batch));

    assertEquals(new Color(1f, 1f, 1f, 1f), batchColour);
    assertEquals(GL20.GL_SRC_ALPHA, blendSrc);
    assertEquals(GL20.GL_ONE_MINUS_SRC_ALPHA, blendDst);
  }

  @Test
  void aGlowingEffectIsAlsoTintedSoLighterSpritesReadTheSameWay() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    FrozenEffect frozen = new FrozenEffect(time, 5000L);
    effects.addStatusEffect(frozen);

    render.render(batch);

    Color normalPass = render.drawColours.get(0);
    assertEquals(frozen.getTint().r, normalPass.r);
    assertEquals(frozen.getTint().b, normalPass.b);
    assertTrue(normalPass.b > normalPass.r);
  }

  @Test
  void stopsGlowingTheInstantTheEffectExpires() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    RecordingRender render = renderOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    when(time.getTime()).thenReturn(5000L);
    render.render(batch);

    assertEquals(1, render.drawColours.size());
  }

  @Test
  void glowsWithWhicheverEntityItWasToldToLookLike() {
    // A weapon sprite shows its wielder's effects, so the glow must follow the same source.
    StatusEffectsControllerComponent wielderEffects = new StatusEffectsControllerComponent();
    Entity wielder =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(wielderEffects);
    wielder.create();
    RecordingRender weapon = new RecordingRender();
    new Entity().addComponent(weapon);
    weapon.setVisualSource(wielder);
    wielderEffects.addStatusEffect(new FrozenEffect(time, 5000L));

    weapon.render(batch);

    assertEquals(2, weapon.drawColours.size());
  }
}
