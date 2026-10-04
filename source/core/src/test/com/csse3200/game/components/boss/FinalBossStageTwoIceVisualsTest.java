package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoIceVisualsTest {
  private static final String ROOT = "images/final-boss/stage2/";
  private static final String[] PATHS = {
    ROOT + "obstacle/crystal-icy.png",
    ROOT + "shatter/PixelTraps-IceShards_spritesheet.png",
    ROOT + "transform/01.png"
  };
  private static final float COLOUR = Color.toFloatBits(0.2f, 0.4f, 0.6f, 0.8f);
  private ResourceService resources;
  private SpriteBatch batch;
  private FinalBossStageTwoIceController ice;
  private FinalBossStageTwoIceVisuals visuals;

  @BeforeEach
  void setup() {
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(PATHS);
    resources.loadAll();
    visuals = new FinalBossStageTwoIceVisuals();
    ice =
        new FinalBossStageTwoIceController(
            new Entity(), new Entity(), new FinalBossStageTwoConfig(), new Random(1));
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(COLOUR);
    when(batch.getBlendSrcFunc()).thenReturn(GL20.GL_SRC_ALPHA);
    when(batch.getBlendDstFunc()).thenReturn(GL20.GL_ONE_MINUS_SRC_ALPHA);
    when(batch.getBlendSrcFuncAlpha()).thenReturn(GL20.GL_ONE);
    when(batch.getBlendDstFuncAlpha()).thenReturn(GL20.GL_ZERO);
  }

  @AfterEach
  void cleanup() {
    resources.unloadAssets(PATHS);
  }

  @Test
  void coverKeepsItsAspectRatioAndAddsVisibleCracksAfterEachOfTheFirstThreeHits() {
    FinalBossStageTwoIceController.Cover cover = cover(ice.getMaxHits());
    ice.covers.add(cover);
    long previousCracks = -1;
    for (int hits = ice.getMaxHits(); hits > 0; hits--) {
      cover.hitsRemaining = hits;
      visuals.draw(batch, ice);

      ArgumentCaptor<TextureRegion> body = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch).draw(body.capture(), eq(2f), eq(3f), eq(0.9f), eq(1.8f));
      assertSame(resources.getAsset(PATHS[0], Texture.class), body.getValue().getTexture());
      assertEquals(32, body.getValue().getRegionWidth());
      assertEquals(64, body.getValue().getRegionHeight());
      long cracks = rotatedDrawCount();
      if (hits == ice.getMaxHits()) assertEquals(0, cracks);
      else assertTrue(cracks > previousCracks, "Each additional hit should extend the cracks");
      previousCracks = cracks;
      verify(batch).setPackedColor(COLOUR);
      clearInvocations(batch);
    }
    cover.hitsRemaining = 0;

    visuals.draw(batch, ice);

    verify(batch, never())
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    assertEquals(0, rotatedDrawCount());
  }

  @Test
  void hitFlashRestoresSeparateBlendFunctionsAndColourEvenIfItsDrawFails() {
    FinalBossStageTwoIceController.Cover cover = cover(ice.getMaxHits() - 1);
    cover.hitFlashRemaining = 0.16f;
    ice.covers.add(cover);

    visuals.draw(batch, ice);

    verify(batch).setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    verifyRestoredBlendAndColour();
    clearInvocations(batch);
    doNothing()
        .doThrow(new IllegalStateException("simulated hit flash failure"))
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    assertThrows(IllegalStateException.class, () -> visuals.draw(batch, ice));

    verifyRestoredBlendAndColour();
  }

  @Test
  void destroyedBodyIsHiddenWhileDebrisAdvancesFadesAndThenStopsDrawing() {
    ice.covers.add(cover(0));
    FinalBossStageTwoIceController.Shatter burst =
        new FinalBossStageTwoIceController.Shatter(new Rectangle(2f, 3f, 0.9f, 1.8f));
    ice.shatters.add(burst);

    visuals.draw(batch, ice);

    TextureRegion first = onlySimpleDraw();
    assertSame(resources.getAsset(PATHS[1], Texture.class), first.getTexture());
    assertEquals(228, first.getRegionX());
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    burst.elapsed = ice.getShatterDuration() * 0.9f;

    visuals.draw(batch, ice);

    TextureRegion last = onlySimpleDraw();
    assertEquals(796, last.getRegionX());
    ArgumentCaptor<Float> alpha = ArgumentCaptor.forClass(Float.class);
    verify(batch).setColor(eq(1f), eq(1f), eq(1f), alpha.capture());
    assertTrue(alpha.getValue() > 0f && alpha.getValue() < 1f);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    burst.elapsed = ice.getShatterDuration();

    visuals.draw(batch, ice);

    verify(batch, never())
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @Test
  void missingControllerLeavesBatchUntouched() {
    visuals.draw(batch, null);

    verifyNoInteractions(batch);
  }

  @Test
  void blueArrivalAnimationPlaysOnceBehindTheAlreadyVisibleCrystal() {
    FinalBossStageTwoIceController.Cover cover = cover(ice.getMaxHits());
    cover.spawnElapsed = 0f;
    ice.covers.add(cover);
    for (int step = 0; step < 2; step++) {
      cover.spawnElapsed = ice.getSpawnDuration() * step / 2f;
      visuals.draw(batch, ice);
      ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch, times(2))
          .draw(frames.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      TextureRegion effect = frames.getAllValues().get(0);
      assertSame(resources.getAsset(PATHS[2], Texture.class), effect.getTexture());
      assertEquals(384, effect.getRegionY());
      assertEquals(step * 256, effect.getRegionX());
      assertSame(
          resources.getAsset(PATHS[0], Texture.class), frames.getAllValues().get(1).getTexture());
      verify(batch).setPackedColor(COLOUR);
      clearInvocations(batch);
    }
    cover.spawnElapsed = ice.getSpawnDuration();
    visuals.draw(batch, ice);
    assertSame(resources.getAsset(PATHS[0], Texture.class), onlySimpleDraw().getTexture());
    verify(batch).setPackedColor(COLOUR);
  }

  private FinalBossStageTwoIceController.Cover cover(int hits) {
    FinalBossStageTwoIceController.Cover cover =
        new FinalBossStageTwoIceController.Cover(
            new Entity(), new Rectangle(2f, 3f, 0.9f, 1.8f), hits);
    cover.spawnElapsed = ice.getSpawnDuration();
    return cover;
  }

  private long rotatedDrawCount() {
    return mockingDetails(batch).getInvocations().stream()
        .filter(
            call -> call.getMethod().getName().equals("draw") && call.getArguments().length == 10)
        .count();
  }

  private TextureRegion onlySimpleDraw() {
    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch).draw(frame.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    return frame.getValue();
  }

  private void verifyRestoredBlendAndColour() {
    verify(batch)
        .setBlendFunctionSeparate(
            GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ZERO);
    verify(batch).setPackedColor(COLOUR);
  }
}
