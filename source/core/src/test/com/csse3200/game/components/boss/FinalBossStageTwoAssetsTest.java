package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoAssetsTest {
  private static final String ROOT = "images/final-boss/stage2/";

  @Test
  void selectedArtworkLoadsWithVisibleFramesWithinTextureBoundsAndReusesTextures() {
    ResourceService resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    String[] paths = FinalBossStageTwoAssets.paths();
    assertEquals(19, paths.length);
    assertEquals(paths.length, new HashSet<>(Arrays.asList(paths)).size());
    resources.loadTextures(paths);
    resources.loadAll();
    try {
      TextureRegion[] transform = FinalBossStageTwoAssets.transformFrames();
      assertEquals(9, transform.length);
      assertRegion(transform[0], 0, 320, 64);
      assertRegion(transform[8], 512, 320, 64);
      assertVisibleRegions(resources, ROOT + "transform/01.png", transform);
      assertSharedTextures(transform, FinalBossStageTwoAssets.transformFrames());

      TextureRegion[] iceSpawn = FinalBossStageTwoAssets.iceSpawnFrames();
      assertEquals(9, iceSpawn.length);
      assertRegion(iceSpawn[0], 0, 384, 64);
      assertRegion(iceSpawn[8], 512, 384, 64);
      assertVisibleRegions(resources, ROOT + "transform/01.png", iceSpawn);
      assertSharedTextures(iceSpawn, FinalBossStageTwoAssets.iceSpawnFrames());
      assertSame(transform[0].getTexture(), iceSpawn[0].getTexture());

      TextureRegion[] shield = FinalBossStageTwoAssets.shieldFrames();
      assertEquals(61, shield.length);
      assertRegion(shield[0], 0, 0, 50);
      assertRegion(shield[60], 200, 350, 50);
      assertVisibleRegions(resources, ROOT + "shield/fire_circles_50x50px.png", shield);
      assertSharedTextures(shield, FinalBossStageTwoAssets.shieldFrames());

      TextureRegion[] fireball = FinalBossStageTwoAssets.fireballFrames();
      assertEquals(5, fireball.length);
      assertIndividualFrames(resources, "fireball", fireball);
      assertSharedTextures(fireball, FinalBossStageTwoAssets.fireballFrames());

      TextureRegion[] impact = FinalBossStageTwoAssets.impactFrames();
      assertEquals(7, impact.length);
      assertIndividualFrames(resources, "impact", impact);
      assertSharedTextures(impact, FinalBossStageTwoAssets.impactFrames());

      TextureRegion obstacle = FinalBossStageTwoAssets.obstacleRegion();
      assertEquals(128, obstacle.getRegionX());
      assertEquals(64, obstacle.getRegionY());
      assertEquals(32, obstacle.getRegionWidth());
      assertEquals(64, obstacle.getRegionHeight());
      assertVisibleRegions(
          resources, ROOT + "obstacle/crystal-icy.png", new TextureRegion[] {obstacle});
      assertSame(obstacle.getTexture(), FinalBossStageTwoAssets.obstacleRegion().getTexture());

      TextureRegion[] shatter = FinalBossStageTwoAssets.shatterFrames();
      assertEquals(6, shatter.length);
      int[] x = {228, 341, 455, 569, 683, 796};
      int[] widths = {113, 114, 114, 114, 113, 114};
      for (int i = 0; i < shatter.length; i++) {
        assertEquals(x[i], shatter[i].getRegionX());
        assertEquals(227, shatter[i].getRegionY());
        assertEquals(widths[i], shatter[i].getRegionWidth());
        assertEquals(114, shatter[i].getRegionHeight());
      }
      assertVisibleRegions(
          resources, ROOT + "shatter/PixelTraps-IceShards_spritesheet.png", shatter);
      assertSharedTextures(shatter, FinalBossStageTwoAssets.shatterFrames());

      TextureRegion[] pickup = FinalBossStageTwoAssets.pickupFrames();
      assertHorizontalFrames(
          resources, ROOT + "pickup/GEM 1 - BLUE - Spritesheet.png", pickup, 18, 30, 10);
      assertSharedTextures(pickup, FinalBossStageTwoAssets.pickupFrames());
      TextureRegion[] spark = FinalBossStageTwoAssets.pickupSparkFrames();
      assertHorizontalFrames(resources, ROOT + "pickup/Spark - Spritesheet.png", spark, 20, 19, 10);
      assertEquals(200, spark[9].getRegionX() + spark[9].getRegionWidth());
      assertSharedTextures(spark, FinalBossStageTwoAssets.pickupSparkFrames());
      TextureRegion[] aura = FinalBossStageTwoAssets.iceAuraFrames();
      assertHorizontalFrames(resources, ROOT + "ice-aura/ice_sparkles.png", aura, 48, 64, 5);
      assertSharedTextures(aura, FinalBossStageTwoAssets.iceAuraFrames());
    } finally {
      resources.unloadAssets(paths);
    }
  }

  @Test
  void oneShotAnimationRetainsItsLastFrameAtAndAfterTheEndpoint() {
    TextureRegion[] frames = frames();
    assertSame(frames[0], FinalBossStageTwoAssets.frame(frames, -1f, 1f, false));
    assertSame(frames[2], FinalBossStageTwoAssets.frame(frames, 0.5f, 1f, false));
    assertSame(frames[3], FinalBossStageTwoAssets.frame(frames, 1f, 1f, false));
    assertSame(frames[3], FinalBossStageTwoAssets.frame(frames, 2f, 1f, false));
    assertSame(frames[3], FinalBossStageTwoAssets.frame(frames, 0f, 0f, false));
  }

  @Test
  void loopWrapsToTheFirstFrameWithoutAnEmptyFrameAtTheEndpoint() {
    TextureRegion[] frames = frames();
    assertSame(frames[0], FinalBossStageTwoAssets.frame(frames, -1f, 1f, true));
    assertSame(frames[3], FinalBossStageTwoAssets.frame(frames, 0.75f, 1f, true));
    assertSame(frames[0], FinalBossStageTwoAssets.frame(frames, 1f, 1f, true));
    assertSame(frames[1], FinalBossStageTwoAssets.frame(frames, 1.25f, 1f, true));
  }

  private static TextureRegion[] frames() {
    return new TextureRegion[] {
      new TextureRegion(), new TextureRegion(), new TextureRegion(), new TextureRegion()
    };
  }

  private static void assertIndividualFrames(
      ResourceService resources, String folder, TextureRegion[] frames) {
    for (int i = 0; i < frames.length; i++) {
      assertRegion(frames[i], 0, 0, 64);
      String path = ROOT + folder + "/00" + (i + 1) + ".png";
      assertVisibleRegions(resources, path, new TextureRegion[] {frames[i]});
    }
  }

  private static void assertRegion(TextureRegion frame, int x, int y, int size) {
    assertEquals(x, frame.getRegionX());
    assertEquals(y, frame.getRegionY());
    assertEquals(size, frame.getRegionWidth());
    assertEquals(size, frame.getRegionHeight());
  }

  private static void assertHorizontalFrames(
      ResourceService resources,
      String path,
      TextureRegion[] frames,
      int width,
      int height,
      int count) {
    assertEquals(count, frames.length);
    for (int i = 0; i < frames.length; i++) {
      assertEquals(i * width, frames[i].getRegionX());
      assertEquals(0, frames[i].getRegionY());
      assertEquals(width, frames[i].getRegionWidth());
      assertEquals(height, frames[i].getRegionHeight());
    }
    assertVisibleRegions(resources, path, frames);
  }

  private static void assertSharedTextures(TextureRegion[] first, TextureRegion[] second) {
    assertEquals(first.length, second.length);
    for (int i = 0; i < first.length; i++) {
      assertSame(first[i].getTexture(), second[i].getTexture());
    }
  }

  private static void assertVisibleRegions(
      ResourceService resources, String path, TextureRegion[] frames) {
    Texture texture = resources.getAsset(path, Texture.class);
    assertEquals(Texture.TextureFilter.Nearest, texture.getMinFilter());
    assertEquals(Texture.TextureFilter.Nearest, texture.getMagFilter());
    Pixmap source = new Pixmap(Gdx.files.internal(path));
    try {
      for (TextureRegion frame : frames) {
        assertSame(texture, frame.getTexture());
        assertTrue(frame.getRegionX() >= 0);
        assertTrue(frame.getRegionY() >= 0);
        assertTrue(frame.getRegionX() + frame.getRegionWidth() <= texture.getWidth());
        assertTrue(frame.getRegionY() + frame.getRegionHeight() <= texture.getHeight());
        assertTrue(hasVisiblePixel(source, frame), path + " contains an empty selected frame");
      }
    } finally {
      source.dispose();
    }
  }

  private static boolean hasVisiblePixel(Pixmap source, TextureRegion frame) {
    for (int y = frame.getRegionY(); y < frame.getRegionY() + frame.getRegionHeight(); y++) {
      for (int x = frame.getRegionX(); x < frame.getRegionX() + frame.getRegionWidth(); x++) {
        if ((source.getPixel(x, y) & 0xff) != 0) {
          return true;
        }
      }
    }
    return false;
  }
}
