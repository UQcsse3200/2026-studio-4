package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Protects the existing Dungeon Two clips against invalid source crops and lost blank frames. */
@ExtendWith(GameExtension.class)
class DungeonTwoAnimationAssetsTest {
  @ParameterizedTest
  @ValueSource(strings = {"beetle", "mummy", "sandeye", "crab", "wasp", "snake"})
  void everyRegionSamplesPixelsInsideItsPage(String enemy) {
    for (Region region : atlas(enemy).getRegions()) {
      Pixmap source = new Pixmap(region.page.textureFile);
      try {
        assertEquals(source.getWidth(), (int) region.page.width);
        assertEquals(source.getHeight(), (int) region.page.height);
        assertInsidePage(region, source);
      } finally {
        source.dispose();
      }
    }
  }

  @Test
  void beetleFuseRetainsOneVisibleFrameAndOneBlankFrame() {
    int visibleFrames = 0;
    int blankFrames = 0;
    for (Region region : atlas("beetle").getRegions()) {
      if (!"fuse".equals(region.name)) {
        continue;
      }
      if (hasVisiblePixel(region)) {
        visibleFrames++;
      } else {
        blankFrames++;
      }
    }
    assertEquals(1, visibleFrames, "The existing fuse must keep its visible half of the blink");
    assertEquals(1, blankFrames, "The existing fuse must keep its blank half of the blink");
  }

  @Test
  void mummyDeathRetainsItsBlankFrame() {
    int deathFrames = 0;
    for (Region region : atlas("mummy").getRegions()) {
      if ("dieAnimation".equals(region.name)) {
        assertFalse(hasVisiblePixel(region), "Mummy death must not select a live walking pose");
        deathFrames++;
      }
    }
    assertEquals(1, deathFrames);
  }

  @Test
  void sandeyeShiftedFramesRetainTheOriginalBottomEdgePixels() {
    Pixmap source = new Pixmap(Gdx.files.internal("images/sandeye.png"));
    try {
      assertTrue(source.getHeight() >= 101, "The five-pixel bob must remain inside the image");
      for (int y = 96; y < 101; y++) {
        for (int x = 0; x < 96; x++) {
          assertEquals(
              source.getPixel(x, 95),
              source.getPixel(x, y),
              "Bobbing must retain the previously clamped bottom-edge pixels");
        }
      }
    } finally {
      source.dispose();
    }
  }

  private static TextureAtlasData atlas(String enemy) {
    FileHandle file = Gdx.files.internal("images/" + enemy + ".atlas");
    return new TextureAtlasData(file, file.parent(), false);
  }

  private static boolean hasVisiblePixel(Region region) {
    Pixmap source = new Pixmap(region.page.textureFile);
    try {
      assertInsidePage(region, source);
      for (int y = region.top; y < region.top + region.height; y++) {
        for (int x = region.left; x < region.left + region.width; x++) {
          if ((source.getPixel(x, y) & 0xff) != 0) {
            return true;
          }
        }
      }
      return false;
    } finally {
      source.dispose();
    }
  }

  private static void assertInsidePage(Region region, Pixmap source) {
    assertTrue(
        region.left >= 0
            && region.top >= 0
            && region.width > 0
            && region.height > 0
            && region.left + region.width <= source.getWidth()
            && region.top + region.height <= source.getHeight(),
        region.page.textureFile.name() + " region " + region.name + " is outside its source image");
  }
}
