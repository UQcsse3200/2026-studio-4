package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FlashEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class MinotaurAttackRenderComponentTest {
  private GameTime time;
  private TextureAtlas atlas;
  private Entity entity;
  private StatusEffectsControllerComponent effects;
  private MinotaurAttackRenderComponent renderer;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    // Keep the atlas and UV handling real; only the GPU-backed texture is unavailable headlessly.
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(4096);
    when(texture.getHeight()).thenReturn(512);
    atlas = new TextureAtlas();
    addFrames(texture, "normalAttack", 7, 0);
    addFrames(texture, "chargeAttack", 9, 160);
    renderer = new MinotaurAttackRenderComponent(atlas);
    effects = new StatusEffectsControllerComponent();
    entity = new Entity().addComponent(effects).addComponent(renderer);
    entity.setPosition(2f, 3f);
    entity.setScale(1.5f, 1.5f);
  }

  @Test
  void renderingAloneCannotAdvanceTheCombatTimeline() {
    renderer.startAnimation("normalAttack", false);
    SpriteBatch batch = mock(SpriteBatch.class);
    for (int i = 0; i < 20; i++) {
      renderer.render(batch);
    }
    assertFrame(renderer.getCurrentFrame(), "normalAttack", 0);
    assertFalse(renderer.isFinished());
  }

  @Test
  void fullAttackCanvasUsesOldSpritePixelScaleAndPreservesGroundAnchor() {
    renderer.startAnimation("normalAttack", false);
    SpriteBatch batch = mock(SpriteBatch.class);
    renderer.draw(batch);
    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    ArgumentCaptor<Float> x = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> y = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> width = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> height = ArgumentCaptor.forClass(Float.class);
    verify(batch)
        .draw(frame.capture(), x.capture(), y.capture(), width.capture(), height.capture());

    assertEquals(288, frame.getValue().getRegionWidth());
    assertEquals(160, frame.getValue().getRegionHeight());
    assertEquals(1.2905405f, x.getValue(), 0.0001f);
    assertEquals(3f, y.getValue(), 0.0001f);
    assertEquals(2.9189188f, width.getValue(), 0.0001f);
    assertEquals(1.8461539f, height.getValue(), 0.0001f);
    assertEquals(new Vector2(2f, 3f), entity.getPosition());
    assertEquals(new Vector2(1.5f, 1.5f), entity.getScale());
  }

  @Test
  void normalAttackPlaysSevenFramesOnceAtOneTenthSecondEach() {
    assertPlaysOnce("normalAttack", 7);
  }

  @Test
  void chargeAttackPlaysNineFramesOnceAtOneTenthSecondEach() {
    assertPlaysOnce("chargeAttack", 9);
  }

  @Test
  void mirroringIsLocalToOneRendererAndDoesNotChangeSharedAtlasFrames() {
    MinotaurAttackRenderComponent other = new MinotaurAttackRenderComponent(atlas);
    new Entity().addComponent(other);
    renderer.startAnimation("normalAttack", true);
    other.startAnimation("normalAttack", false);
    SpriteBatch mirroredBatch = mock(SpriteBatch.class);
    SpriteBatch originalBatch = mock(SpriteBatch.class);
    renderer.draw(mirroredBatch);
    other.draw(originalBatch);
    ArgumentCaptor<TextureRegion> mirrored = ArgumentCaptor.forClass(TextureRegion.class);
    ArgumentCaptor<TextureRegion> original = ArgumentCaptor.forClass(TextureRegion.class);
    verify(mirroredBatch).draw(mirrored.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    verify(originalBatch).draw(original.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    assertTrue(mirrored.getValue().isFlipX());
    assertFalse(original.getValue().isFlipX());
    assertFalse(atlas.findRegions("normalAttack").first().isFlipX());
    assertEquals(0f, atlas.findRegions("normalAttack").first().getU());
    assertEquals(0.0703125f, atlas.findRegions("normalAttack").first().getU2());
    assertEquals(0.0703125f, mirrored.getValue().getU());
    assertEquals(0f, mirrored.getValue().getU2());
  }

  @Test
  void frozenAttackHoldsItsCurrentFrameAndResumesFromThatFrameAfterThaw() {
    renderer.startAnimation("chargeAttack", false);
    SpriteBatch batch = mock(SpriteBatch.class);
    renderer.draw(batch);
    renderer.advance(0.1f);
    TextureRegion held = renderer.getCurrentFrame();
    assertFrame(held, "chargeAttack", 1);
    effects.addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000L));
    for (int i = 0; i < 12; i++) {
      renderer.advance(0.1f);
      renderer.draw(batch);
    }
    assertFrame(renderer.getCurrentFrame(), "chargeAttack", 1);
    assertFalse(renderer.isFinished());

    effects.clearStatusEffects();
    renderer.advance(0.1f);
    renderer.draw(batch);
    assertFrame(renderer.getCurrentFrame(), "chargeAttack", 2);
  }

  @Test
  void glowReplaysTheSameCombatFrameWithoutAdvancingTime() {
    effects.addStatusEffect(
        new FlashEffect(time, 2000L, null, new Color(0.35f, 0.65f, 0.95f, 0.5f)));
    renderer.startAnimation("normalAttack", false);
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
    ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);

    renderer.render(batch);
    verify(batch, times(2)).draw(frames.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    List<TextureRegion> drawn = frames.getAllValues();
    assertFrame(drawn.get(0), "normalAttack", 0);
    assertFrame(drawn.get(1), "normalAttack", 0);
    assertFrame(renderer.getCurrentFrame(), "normalAttack", 0);

    renderer.advance(0.1f);
    clearInvocations(batch);
    renderer.render(batch);
    frames = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch, times(2)).draw(frames.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    drawn = frames.getAllValues();
    assertFrame(drawn.get(0), "normalAttack", 1);
    assertFrame(drawn.get(1), "normalAttack", 1);
    assertFrame(renderer.getCurrentFrame(), "normalAttack", 1);
  }

  @Test
  void stoppingAnAttackClearsVisibleFrameAndPreventsFurtherDrawing() {
    renderer.startAnimation("normalAttack", true);
    renderer.stopAnimation();
    assertNull(renderer.getCurrentAnimation());
    assertNull(renderer.getCurrentFrame());
    SpriteBatch batch = mock(SpriteBatch.class);
    renderer.draw(batch);
    verifyNoInteractions(batch);
    renderer.startAnimation("chargeAttack", false);
    assertEquals("chargeAttack", renderer.getCurrentAnimation());
    assertFrame(renderer.getCurrentFrame(), "chargeAttack", 0);
    assertFalse(renderer.isFinished());
  }

  private void assertPlaysOnce(String name, int count) {
    renderer.startAnimation(name, false);
    SpriteBatch batch = mock(SpriteBatch.class);
    for (int i = 0; i < count; i++) {
      assertFalse(renderer.isFinished());
      assertFrame(renderer.getCurrentFrame(), name, i);
      renderer.draw(batch);
      renderer.advance(0.1f);
    }
    assertTrue(renderer.isFinished());
    assertFrame(renderer.getCurrentFrame(), name, count - 1);
    renderer.draw(batch);
    assertTrue(renderer.isFinished());
    assertFrame(renderer.getCurrentFrame(), name, count - 1);
  }

  private void assertFrame(TextureRegion actual, String name, int index) {
    TextureRegion expected = atlas.findRegions(name).get(index);
    assertEquals(expected.getU(), actual.getU());
    assertEquals(expected.getV(), actual.getV());
    assertEquals(expected.getU2(), actual.getU2());
    assertEquals(expected.getV2(), actual.getV2());
  }

  private void addFrames(Texture texture, String name, int count, int y) {
    for (int i = 0; i < count; i++) {
      atlas.addRegion(name, texture, i * 288, y, 288, 160).index = i;
    }
  }
}
