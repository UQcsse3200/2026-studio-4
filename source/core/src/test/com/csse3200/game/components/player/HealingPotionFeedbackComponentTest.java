package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class HealingPotionFeedbackComponentTest {
  @Test
  void floatingNumberHasVisibleGlyphHeightInWorldCoordinates() {
    Texture atlas = mock(Texture.class);
    when(atlas.getWidth()).thenReturn(512);
    when(atlas.getHeight()).thenReturn(512);
    BitmapFont actualFont =
        new BitmapFont(
            new BitmapFont.BitmapFontData(
                com.badlogic.gdx.Gdx.files.internal("flat-earth/skin/fonts/pixel_16.fnt"), false),
            new com.badlogic.gdx.graphics.g2d.TextureRegion(atlas),
            false);
    try (var fonts =
            mockConstruction(
                BitmapFont.class,
                (font, context) -> {
                  when(font.getData()).thenReturn(actualFont.getData());
                  when(font.getCapHeight()).thenReturn(actualFont.getCapHeight());
                  doAnswer(
                          call -> {
                            actualFont.setUseIntegerPositions(call.getArgument(0));
                            return null;
                          })
                      .when(font)
                      .setUseIntegerPositions(org.mockito.ArgumentMatchers.anyBoolean());
                  doAnswer(
                          call ->
                              actualFont.draw(
                                  call.getArgument(0),
                                  call.getArgument(1),
                                  call.getArgument(2),
                                  call.getArgument(3),
                                  call.getArgument(4),
                                  call.getArgument(5),
                                  call.getArgument(6)))
                      .when(font)
                      .draw(
                          org.mockito.ArgumentMatchers.any(SpriteBatch.class),
                          org.mockito.ArgumentMatchers.anyString(),
                          anyFloat(),
                          anyFloat(),
                          anyFloat(),
                          org.mockito.ArgumentMatchers.anyInt(),
                          org.mockito.ArgumentMatchers.anyBoolean());
                });
        var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class)) {
      stats.setHealth(40);
      inventory.addConsumable(ItemIds.HEALTH_POTION);
      assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
      feedback.render(mock(SpriteBatch.class));
      float[] vertices = actualFont.getCache().getVertices(0);
      assertTrue(
          Math.abs(vertices[6] - vertices[1]) > 0.1f,
          "Scaled world-space glyphs must retain a visible height");
      feedback.dispose();
    } finally {
      actualFont.dispose();
    }
  }

  private CombatStatsComponent stats;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private HealingPotionFeedbackComponent feedback;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.3f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(new RenderService());
    stats = new CombatStatsComponent(100, 10);
    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    feedback = new HealingPotionFeedbackComponent();
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    Entity player =
        new Entity()
            .addComponent(stats)
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables)
            .addComponent(feedback);
    player.setPosition(2f, 3f);
    effects.create();
    consumables.create();
    feedback.create();
  }

  @Test
  void successfulHealingPotionShowsCrossBurstThenFadesAway() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(40);
    inventory.addConsumable(ItemIds.HEALTH_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
      feedback.render(batch);
      Texture pixel = textures.constructed().get(0);
      verify(batch, times(8)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());

      for (int i = 0; i < 5; i++) feedback.update();
      clearInvocations(batch);
      feedback.render(batch);
      verify(batch, never()).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      feedback.dispose();
      verify(pixel).dispose();
    }
  }

  @Test
  void rejectedHealAndOtherPotionDoNotShowCross() {
    SpriteBatch batch = mock(SpriteBatch.class);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SPEED_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class)) {
      assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));
      assertTrue(consumables.tryUse(ItemIds.SPEED_POTION));
      feedback.render(batch);
      assertTrue(pixels.constructed().isEmpty());
    }
  }

  @Test
  void mediumAndLargeHealingPotionsAlsoShowCross() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(1);
    inventory.addConsumable(ItemIds.MEDIUM_HEALTH_POTION);
    inventory.addConsumable(ItemIds.LARGE_HEALTH_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      assertTrue(consumables.tryUse(ItemIds.MEDIUM_HEALTH_POTION));
      feedback.render(batch);
      Texture pixel = textures.constructed().get(0);
      verify(batch, times(8)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());

      stats.setHealth(1);
      assertTrue(consumables.tryUse(ItemIds.LARGE_HEALTH_POTION));
      feedback.render(batch);
      verify(batch, times(16)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }
  }

  @Test
  void customHealingPotionShowsTheSameFeedback() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(10);
    inventory.addConsumable("HEALTH_POTION_75");

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      assertTrue(consumables.tryUse("HEALTH_POTION_75"));
      feedback.render(batch);
      verify(batch, times(8))
          .draw(eq(textures.constructed().get(0)), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }
  }

  @Test
  void healingPulseExpandsCrossesRiseAndLargerPotionsHaveStrongerFeedback() {
    SpriteBatch batch = mock(SpriteBatch.class);
    List<float[]> crosses = new ArrayList<>();
    List<Float> pulseWidths = new ArrayList<>();
    doAnswer(
            call -> {
              crosses.add(new float[] {call.getArgument(2), call.getArgument(3)});
              return null;
            })
        .when(batch)
        .draw(
            org.mockito.ArgumentMatchers.any(Texture.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());
    doAnswer(
            call -> {
              pulseWidths.add(call.getArgument(5));
              return null;
            })
        .when(batch)
        .draw(
            org.mockito.ArgumentMatchers.any(Texture.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.anyBoolean());
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      stats.setHealth(1);
      inventory.addConsumable(ItemIds.HEALTH_POTION);
      assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
      feedback.render(batch);
      float smallWidth = crosses.get(2)[1];
      float initialY = crosses.get(2)[0];
      float initialPulse = pulseWidths.get(0);
      when(ServiceLocator.getTimeSource().getDeltaTime()).thenReturn(0.15f);
      feedback.update();
      crosses.clear();
      pulseWidths.clear();
      feedback.render(batch);
      assertTrue(crosses.get(2)[0] > initialY);
      assertTrue(pulseWidths.get(0) > initialPulse);
      stats.setHealth(1);
      inventory.addConsumable(ItemIds.LARGE_HEALTH_POTION);
      assertTrue(consumables.tryUse(ItemIds.LARGE_HEALTH_POTION));
      crosses.clear();
      feedback.render(batch);
      assertTrue(crosses.get(2)[1] > smallWidth);
      assertTrue(textures.constructed().size() == 1);
      stats.setHealth(0);
      crosses.clear();
      feedback.render(batch);
      assertTrue(crosses.isEmpty());
      feedback.dispose();
    }
  }

  @Test
  void healingDrawFailureRestoresColourAndSeparateBlendFunctions() {
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(0.25f);
    when(batch.getBlendSrcFunc()).thenReturn(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA);
    when(batch.getBlendDstFunc()).thenReturn(com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    when(batch.getBlendSrcFuncAlpha()).thenReturn(com.badlogic.gdx.graphics.GL20.GL_ONE);
    when(batch.getBlendDstFuncAlpha()).thenReturn(com.badlogic.gdx.graphics.GL20.GL_ZERO);
    stats.setHealth(1);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      doThrow(new IllegalStateException("draw failed"))
          .when(batch)
          .draw(
              org.mockito.ArgumentMatchers.any(Texture.class),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat());
      assertThrows(IllegalStateException.class, () -> feedback.render(batch));
      verify(batch).setPackedColor(0.25f);
      verify(batch)
          .setBlendFunctionSeparate(
              com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
              com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA,
              com.badlogic.gdx.graphics.GL20.GL_ONE,
              com.badlogic.gdx.graphics.GL20.GL_ZERO);
      feedback.dispose();
      feedback.dispose();
      verify(textures.constructed().get(0), times(1)).dispose();
    }
  }

  private MockedConstruction<BitmapFont> mockFonts() {
    return mockConstruction(
        BitmapFont.class,
        (font, context) -> {
          when(font.getData()).thenReturn(mock(BitmapFont.BitmapFontData.class));
          when(font.getCapHeight()).thenReturn(16f);
        });
  }

  @Test
  void numberMatchesActualHealingForEachPotionAndHealthCap() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setMaxHealth(200);
    String[] ids = {
      ItemIds.HEALTH_POTION,
      ItemIds.MEDIUM_HEALTH_POTION,
      "HEALTH_POTION_75",
      ItemIds.LARGE_HEALTH_POTION
    };
    int[] healing = {25, 50, 75, 100};
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class);
        var fonts = mockFonts()) {
      for (int i = 0; i < ids.length; i++) {
        stats.setHealth(1);
        inventory.addConsumable(ids[i]);
        assertTrue(consumables.tryUse(ids[i]));
        feedback.render(batch);
        verify(fonts.constructed().get(0), times(2))
            .draw(
                eq(batch),
                eq("+" + healing[i]),
                anyFloat(),
                anyFloat(),
                anyFloat(),
                eq(Align.center),
                eq(false));
      }
      stats.setHealth(195);
      inventory.addConsumable(ItemIds.HEALTH_POTION);
      assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
      feedback.render(batch);
      BitmapFont font = fonts.constructed().get(0);
      verify(font, times(2))
          .draw(
              eq(batch), eq("+5"), anyFloat(), anyFloat(), anyFloat(), eq(Align.center), eq(false));
      clearInvocations(font);
      for (int i = 0; i < 5; i++) feedback.update();
      feedback.render(batch);
      org.mockito.Mockito.verifyNoInteractions(font);
      feedback.dispose();
      feedback.dispose();
      verify(font).dispose();
    }
  }
}
