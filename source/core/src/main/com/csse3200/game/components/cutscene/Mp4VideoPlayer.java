package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays an mp4 (H.264) file as a stream of textures, using the pure-Java JCodec decoder so it
 * behaves the same on every platform. Frames are decoded on a background thread and shown on the
 * render thread by timestamp, dropping frames if decoding falls behind. There is no audio.
 *
 * <p>Pure-Java decoding is fastest on modest resolutions (720p or below) and baseline/main H.264.
 */
class Mp4VideoPlayer implements Disposable {
  private static final Logger logger = LoggerFactory.getLogger(Mp4VideoPlayer.class);
  private static final int QUEUE_SIZE = 6;

  /** One decoded frame, ready to upload. */
  private static final class Frame {
    final int width;
    final int height;
    final byte[] rgba;
    final double timestamp;

    Frame(int width, int height, byte[] rgba, double timestamp) {
      this.width = width;
      this.height = height;
      this.rgba = rgba;
      this.timestamp = timestamp;
    }
  }

  private final BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
  private final Thread decoderThread;
  private volatile boolean decodingDone;
  private volatile boolean failed;
  private volatile boolean stopped;

  private Pixmap pixmap;
  private Texture texture;
  private boolean clockStarted;
  private double clock;
  private double firstTimestamp;

  /**
   * Opens the file and starts decoding. Throws if the file is not a decodable mp4.
   *
   * @param file the mp4
   */
  Mp4VideoPlayer(FileHandle file) throws IOException, JCodecException {
    byte[] bytes = file.readBytes();
    FrameGrab grab =
        FrameGrab.createFrameGrab(
            ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(bytes)));
    decoderThread = new Thread(() -> decodeLoop(grab), "cutscene-video-decoder");
    decoderThread.setDaemon(true);
    decoderThread.start();
  }

  private void decodeLoop(FrameGrab grab) {
    try {
      Transform toRgb = null;
      Picture rgb = null;
      PictureWithMetadata decoded;
      while (!stopped && (decoded = grab.getNativeFrameWithMetadata()) != null) {
        Picture source = decoded.getPicture();
        if (toRgb == null) {
          toRgb = ColorUtil.getTransform(source.getColor(), ColorSpace.RGB);
          rgb = Picture.create(source.getWidth(), source.getHeight(), ColorSpace.RGB);
        }
        toRgb.transform(source, rgb);
        queue.put(new Frame(rgb.getWidth(), rgb.getHeight(), toRgba(rgb), decoded.getTimestamp()));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException | RuntimeException e) {
      logger.error("Video decoding failed", e);
      failed = true;
    } finally {
      decodingDone = true;
    }
  }

  /** JCodec stores RGB as interleaved signed bytes (value - 128). */
  private static byte[] toRgba(Picture rgb) {
    byte[] src = rgb.getPlaneData(0);
    int pixels = rgb.getWidth() * rgb.getHeight();
    byte[] out = new byte[pixels * 4];
    for (int i = 0, s = 0, o = 0; i < pixels; i++) {
      out[o++] = (byte) (src[s++] + 128);
      out[o++] = (byte) (src[s++] + 128);
      out[o++] = (byte) (src[s++] + 128);
      out[o++] = (byte) 255;
    }
    return out;
  }

  /**
   * Moves playback forward and uploads the frame that should be on screen. Call from the render
   * thread once per frame.
   *
   * @param deltaSeconds time since the last call
   */
  void update(float deltaSeconds) {
    if (clockStarted) {
      clock += deltaSeconds;
    }
    Frame toShow = null;
    Frame next;
    while ((next = queue.peek()) != null) {
      if (!clockStarted) {
        clockStarted = true;
        firstTimestamp = next.timestamp;
      }
      if (next.timestamp - firstTimestamp > clock) {
        break;
      }
      toShow = queue.poll();
    }
    if (toShow != null) {
      upload(toShow);
    }
  }

  private void upload(Frame frame) {
    if (pixmap == null) {
      pixmap = new Pixmap(frame.width, frame.height, Pixmap.Format.RGBA8888);
      texture = new Texture(pixmap);
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }
    ByteBuffer pixels = pixmap.getPixels();
    pixels.clear();
    pixels.put(frame.rgba);
    pixels.clear();
    texture.draw(pixmap, 0, 0);
  }

  /**
   * @return the latest frame, or null before the first one is ready
   */
  Texture getTexture() {
    return texture;
  }

  int getWidth() {
    return pixmap == null ? 0 : pixmap.getWidth();
  }

  int getHeight() {
    return pixmap == null ? 0 : pixmap.getHeight();
  }

  /**
   * @return true once every frame has been shown (or decoding failed)
   */
  boolean isFinished() {
    return decodingDone && queue.isEmpty();
  }

  boolean hasFailed() {
    return failed;
  }

  /**
   * @return true if at least one frame has been shown
   */
  boolean hasStarted() {
    return texture != null;
  }

  @Override
  public void dispose() {
    stopped = true;
    decoderThread.interrupt();
    queue.clear();
    if (texture != null) {
      texture.dispose();
    }
    if (pixmap != null) {
      pixmap.dispose();
    }
    texture = null;
    pixmap = null;
  }
}
