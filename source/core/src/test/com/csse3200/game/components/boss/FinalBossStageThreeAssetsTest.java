package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageThreeAssetsTest {
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
      assertEquals(14, FinalBossStageThreeAssets.paths().length);
      TextureRegion[] tornado = FinalBossStageThreeAssets.tornadoFrames();
      assertEquals(60, tornado.length);
      assertEquals(80, tornado[0].getRegionWidth());
      assertEquals(45, tornado[0].getRegionHeight());
      assertEquals(480, tornado[59].getRegionX() + tornado[59].getRegionWidth());
      assertEquals(225, tornado[59].getRegionY() + tornado[59].getRegionHeight());
      assertNotSame(tornado[0].getTexture(), tornado[30].getTexture());
      assertSame(
          tornado[0].getTexture(), FinalBossStageThreeAssets.tornadoFrames()[0].getTexture());
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
