package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.csse3200.game.extensions.GameExtension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Regression coverage for clipped tails, omitted frames and incorrect attack sheet selection. */
@ExtendWith(GameExtension.class)
class MedusaAnimationAssetsTest {
  @ParameterizedTest
  @CsvSource({
    "default,Idle,0,7",
    "move,Walk,0,13",
    "chase,Run,0,7",
    "lungeTelegraph,Attack_3,0,6",
    "lungeDash,Attack_3,6,4",
    "hurt,Hurt,0,3",
    "dieAnimation,Dead,0,3"
  })
  void clipsLoadEveryOriginalFrameWithoutCropping(
      String clip, String sheet, int firstFrame, int frameCount) throws Exception {
    BufferedImage original = ImageIO.read(new File("images/split-enemy/medusa/" + sheet + ".png"));
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images/split-enemy/medusa.atlas"));
    try {
      var frames = atlas.findRegions(clip);
      assertEquals(frameCount, frames.size, clip + " must retain every source frame");
      for (int i = 0; i < frameCount; i++) {
        AtlasRegion frame = frames.get(i);
        assertEquals(i, frame.index, clip + " playback order");
        assertEquals(128, frame.getRegionWidth(), "Full cell retains body and tail");
        assertEquals(128, frame.getRegionHeight(), "Full cell retains the original aspect ratio");
        assertEquals(128, frame.originalWidth);
        assertEquals(128, frame.originalHeight);
        assertEquals(0f, frame.offsetX);
        assertEquals(0f, frame.offsetY);
        assertFalse(frame.rotate);
        assertEquals((firstFrame + i) * 128, frame.getRegionX());
        assertEquals(0, frame.getRegionY());
        BufferedImage actualSheet =
            ImageIO.read(
                ((FileTextureData) frame.getTexture().getTextureData()).getFileHandle().file());
        assertTrue(frame.getRegionX() >= 0 && frame.getRegionY() >= 0);
        assertTrue(frame.getRegionX() + frame.getRegionWidth() <= actualSheet.getWidth());
        assertTrue(frame.getRegionY() + frame.getRegionHeight() <= actualSheet.getHeight());
        int[] expectedPixels = original.getRGB((firstFrame + i) * 128, 0, 128, 128, null, 0, 128);
        int[] loadedPixels =
            actualSheet.getRGB(frame.getRegionX(), frame.getRegionY(), 128, 128, null, 0, 128);
        assertArrayEquals(expectedPixels, loadedPixels, clip + " frame " + i + " source pixels");
      }
    } finally {
      atlas.dispose();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Idle,931fdedfa04e2706c69452f890aedab74a92381c2169f7169738e066b5462722",
    "Walk,e110fbc0b68fa774dcada886ce6565bd465c8f40485f4a302c4e188c729ac166",
    "Run,d676d5d19dce27fb97fd103cf2ac79d31a5deb88d1963b5d1d136c1212a2cae2",
    "Attack_3,5695d934bdeb86b647b78ca46a3aca27467f915b0ee15ce1bc9604ab00be7cd2",
    "Hurt,590ca5853f3a30c782032faedd44c6a6a62cfe8c103ba8549d54068dd70ac3c0",
    "Dead,451c0a4f6391f2c03b8ef6bf5b52ef74994e1647f2c7afc9bb321ea85df30121"
  })
  void originalSheetsRetainTheirUnmodifiedPixels(String sheet, String sha256) throws Exception {
    byte[] bytes =
        Files.readAllBytes(new File("images/split-enemy/medusa/" + sheet + ".png").toPath());
    assertEquals(
        sha256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
  }

  @Test
  void atlasContainsOnlyTheCompletePlaybackClips() {
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images/split-enemy/medusa.atlas"));
    try {
      assertEquals(43, atlas.getRegions().size);
    } finally {
      atlas.dispose();
    }
  }
}
