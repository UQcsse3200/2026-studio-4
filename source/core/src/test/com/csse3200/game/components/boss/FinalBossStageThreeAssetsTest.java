package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderable;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageThreeAssetsTest {
  private static final String TORNADO_HIT = "images/final-boss/stage2/transform/01.png";

  @Test
  void selectedSheetsLoadAndSliceWithinTextureBounds() {
    ResourceService resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());
    resources.loadTextures(FinalBossStageThreeAssets.paths());
    resources.loadTextures(FinalBossVisualAssets.paths());
    resources.loadAll();
    try {
      TextureRegion[] burst = FinalBossStageThreeAssets.sheet("burst", 64, 64, 12, 60, 12);
      assertEquals(320, burst[0].getRegionY());
      assertEquals(768, burst[11].getRegionX() + burst[11].getRegionWidth());
      TextureRegion[] freeze = FinalBossStageThreeAssets.sheet("freeze-loop", 32, 32, 8, 0, 8);
      assertEquals(256, freeze[7].getRegionX() + freeze[7].getRegionWidth());
      assertEquals(15, FinalBossStageThreeAssets.paths().length);
      TextureRegion[] tornado = FinalBossStageThreeAssets.tornadoFrames();
      assertEquals(60, tornado.length);
      assertEquals(80, tornado[0].getRegionWidth());
      assertEquals(45, tornado[0].getRegionHeight());
      assertEquals(480, tornado[59].getRegionX() + tornado[59].getRegionWidth());
      assertEquals(225, tornado[59].getRegionY() + tornado[59].getRegionHeight());
      assertNotSame(tornado[0].getTexture(), tornado[30].getTexture());
      assertSame(
          tornado[0].getTexture(), FinalBossStageThreeAssets.tornadoFrames()[0].getTexture());
      TextureRegion[] tornadoHit = FinalBossStageThreeAssets.tornadoHitFrames();
      assertEquals(9, tornadoHit.length);
      Texture hitTexture = resources.getAsset(TORNADO_HIT, Texture.class);
      assertEquals(Texture.TextureFilter.Nearest, hitTexture.getMinFilter());
      assertEquals(Texture.TextureFilter.Nearest, hitTexture.getMagFilter());
      Pixmap hitSource = new Pixmap(Gdx.files.internal(TORNADO_HIT));
      try {
        for (int i = 0; i < tornadoHit.length; i++) {
          TextureRegion frame = tornadoHit[i];
          assertSame(hitTexture, frame.getTexture());
          assertEquals(i * 64, frame.getRegionX());
          assertEquals(192, frame.getRegionY());
          assertEquals(64, frame.getRegionWidth());
          assertEquals(64, frame.getRegionHeight());
          assertTrue(frame.getRegionX() + frame.getRegionWidth() <= hitTexture.getWidth());
          assertTrue(frame.getRegionY() + frame.getRegionHeight() <= hitTexture.getHeight());
          assertTrue(hasVisiblePixel(hitSource, frame), "White swirl frame " + i + " is empty");
        }
      } finally {
        hitSource.dispose();
      }
      assertSame(hitTexture, FinalBossStageThreeAssets.tornadoHitFrames()[0].getTexture());
      assertSame(hitTexture, FinalBossStageTwoAssets.transformFrames()[0].getTexture());
      assertSame(
          tornadoHit[8],
          FinalBossStageThreeAssets.frame(
              tornadoHit,
              FinalBossStageThreeComponent.TORNADO_HIT_DURATION,
              FinalBossStageThreeComponent.TORNADO_HIT_DURATION,
              false));
      FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
      when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
      FinalBossStageThreeComponent stage = mock(FinalBossStageThreeComponent.class);
      when(stage.getState()).thenReturn(FinalBossStageThreeState.PEACEFUL);
      // Construct the real renderer to verify all selected filenames and layouts.
      Entity boss =
          new Entity()
              .addComponent(stage)
              .addComponent(phases)
              .addComponent(new FinalBossStageThreeVisualComponent());
      boss.getComponent(FinalBossStageThreeVisualComponent.class).create();
      boss.getComponent(FinalBossStageThreeVisualComponent.class).dispose();
    } finally {
      resources.unloadAssets(FinalBossStageThreeAssets.paths());
      resources.unloadAssets(FinalBossVisualAssets.paths());
    }
  }

  @Test
  void whiteTornadoHitFollowsPlayerAndStopsWhenExpiredOrDeadWithoutTintingOtherDraws() {
    ResourceService resources = new ResourceService();
    RenderService renderer = mock(RenderService.class);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(renderer);
    resources.loadTextures(FinalBossStageThreeAssets.paths());
    resources.loadTextures(FinalBossVisualAssets.paths());
    resources.loadAll();
    FinalBossStageThreeVisualComponent visual = new FinalBossStageThreeVisualComponent();
    try {
      FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
      when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
      CombatStatsComponent stats = new CombatStatsComponent(100, 1);
      Entity player = new Entity().addComponent(stats);
      player.setPosition(4f, 5f);
      player.setScale(1f, 1f);
      FinalBossStageThreeComponent stage =
          new FinalBossStageThreeComponent(player, ignored -> {}, new FinalBossStageThreeConfig());
      new Entity().addComponent(stage).addComponent(phases).addComponent(visual);
      visual.create();
      ArgumentCaptor<Renderable> registered = ArgumentCaptor.forClass(Renderable.class);
      verify(renderer, times(3)).register(registered.capture());
      Renderable overlay = registered.getAllValues().get(2);
      assertEquals(Float.MAX_VALUE, overlay.getZIndex());
      SpriteBatch batch = mock(SpriteBatch.class);
      float colour = Color.toFloatBits(0.2f, 0.4f, 0.6f, 0.8f);
      when(batch.getPackedColor()).thenReturn(colour);
      stage.tornadoHitRemaining = FinalBossStageThreeComponent.TORNADO_HIT_DURATION;
      stage.playerHitRemaining = 0.24f;

      overlay.render(batch);

      ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch).draw(frame.capture(), eq(3.75f), eq(4.75f), eq(1.5f), eq(1.5f));
      assertSame(resources.getAsset(TORNADO_HIT, Texture.class), frame.getValue().getTexture());
      assertEquals(0, frame.getValue().getRegionX());
      assertEquals(192, frame.getValue().getRegionY());
      verify(batch, times(1))
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      verify(batch).setColor(Color.WHITE);
      verify(batch, atLeastOnce()).setPackedColor(colour);
      clearInvocations(batch);
      player.setPosition(6f, 7f);
      stage.tornadoHitRemaining = 0.001f;

      overlay.render(batch);

      verify(batch).draw(frame.capture(), eq(5.75f), eq(6.75f), eq(1.5f), eq(1.5f));
      assertEquals(512, frame.getValue().getRegionX());
      assertEquals(192, frame.getValue().getRegionY());
      clearInvocations(batch);
      stage.tornadoHitRemaining = 0f;
      stage.playerHitRemaining = 0f;

      overlay.render(batch);

      verify(batch, never())
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      stage.tornadoHitRemaining = FinalBossStageThreeComponent.TORNADO_HIT_DURATION;
      stats.setHealth(0);
      clearInvocations(batch);

      overlay.render(batch);

      verify(batch, never())
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      stats.setHealth(100);
      clearInvocations(batch);
      doThrow(new IllegalStateException("simulated draw failure"))
          .when(batch)
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

      assertThrows(IllegalStateException.class, () -> overlay.render(batch));

      verify(batch).setPackedColor(colour);
    } finally {
      visual.dispose();
      resources.unloadAssets(FinalBossStageThreeAssets.paths());
      resources.unloadAssets(FinalBossVisualAssets.paths());
    }
  }

  private static boolean hasVisiblePixel(Pixmap source, TextureRegion frame) {
    for (int y = frame.getRegionY(); y < frame.getRegionY() + frame.getRegionHeight(); y++) {
      for (int x = frame.getRegionX(); x < frame.getRegionX() + frame.getRegionWidth(); x++) {
        if ((source.getPixel(x, y) & 0xff) != 0) return true;
      }
    }
    return false;
  }

  @Test
  void tornadoBackgroundKeyRetainsOtherColoursAndRestoresPixmapBlending() {
    Pixmap pixmap = new Pixmap(3, 1, Pixmap.Format.RGBA8888);
    try {
      pixmap.setBlending(Pixmap.Blending.None);
      pixmap.drawPixel(0, 0, 0x161E23FF);
      pixmap.drawPixel(1, 0, 0x141619FF);
      pixmap.drawPixel(2, 0, 0xA0AFC8FF);
      pixmap.setBlending(Pixmap.Blending.SourceOver);
      FinalBossTornadoTextureData.makeBackgroundTransparent(pixmap);
      assertEquals(0x161E2300, pixmap.getPixel(0, 0));
      assertEquals(0x141619FF, pixmap.getPixel(1, 0));
      assertEquals(0xA0AFC8FF, pixmap.getPixel(2, 0));
      assertEquals(Pixmap.Blending.SourceOver, pixmap.getBlending());
    } finally {
      pixmap.dispose();
    }
  }
}
