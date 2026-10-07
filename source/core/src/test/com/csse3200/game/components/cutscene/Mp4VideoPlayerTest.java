package com.csse3200.game.components.cutscene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.locks.LockSupport;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class Mp4VideoPlayerTest {
  private static final long TIMEOUT_MILLIS = 20_000;

  @Test
  void rgbBytesAreConvertedToOpaqueRgba() {
    Picture rgb = Picture.create(2, 1, ColorSpace.RGB);
    byte[] data = rgb.getPlaneData(0);
    // JCodec stores each channel as value - 128
    data[0] = -128; // pixel 0: r = 0
    data[1] = 0; // g = 128
    data[2] = 127; // b = 255
    data[3] = 127; // pixel 1: r = 255
    data[4] = -128; // g = 0
    data[5] = 0; // b = 128

    byte[] rgba = Mp4VideoPlayer.toRgba(rgb, 0, 0, 2, 1);

    assertArrayEquals(
        new byte[] {0, (byte) 128, (byte) 255, (byte) 255, (byte) 255, 0, (byte) 128, (byte) 255},
        rgba);
  }

  @Test
  void onlyTheRequestedWindowIsCopied() {
    // 2x2 picture: top row 0, bottom row 255 (like decoder padding below the visible area)
    Picture rgb = Picture.create(2, 2, ColorSpace.RGB);
    byte[] data = rgb.getPlaneData(0);
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i < 6 ? -128 : 127);
    }

    byte[] top = Mp4VideoPlayer.toRgba(rgb, 0, 0, 2, 1);
    byte[] bottom = Mp4VideoPlayer.toRgba(rgb, 0, 1, 2, 1);

    assertEquals(2 * 1 * 4, top.length);
    assertEquals(0, top[0]);
    assertEquals((byte) 255, top[3]);
    assertEquals((byte) 255, bottom[0]);
  }

  @Test
  void demoVideoDecodesFramesAndFinishes() throws Exception {
    Mp4VideoPlayer player = new Mp4VideoPlayer(Gdx.files.internal("videos/demo.mp4"));
    try {
      long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
      while (!player.isFinished() && !player.hasFailed() && System.currentTimeMillis() < deadline) {
        player.update(1f / 30f);
        LockSupport.parkNanos(2_000_000L);
      }

      assertFalse(player.hasFailed(), "decoding should not fail");
      assertTrue(player.isFinished(), "the whole video should play out");
      assertTrue(player.hasStarted(), "at least one frame should have been shown");
      assertNotNull(player.getTexture());
      assertEquals(640, player.getWidth());
      assertEquals(360, player.getHeight());
    } finally {
      player.dispose();
    }
  }

  @Test
  void notAnMp4IsRejected() {
    org.junit.jupiter.api.Assertions.assertThrows(
        Exception.class, () -> new Mp4VideoPlayer(Gdx.files.internal("configs/player.json")));
  }
}
