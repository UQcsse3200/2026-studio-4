package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
      assertEquals(12, FinalBossStageThreeAssets.paths().length);
      for (String path : FinalBossStageThreeAssets.paths()) assertFalse(path.contains("tornado"));
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
}
