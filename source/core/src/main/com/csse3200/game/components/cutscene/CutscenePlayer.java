package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.csse3200.game.ui.UIComponent;
import java.io.IOException;
import org.jcodec.api.JCodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays {@link CutsceneScript}s: an mp4 shown full screen over the game (see {@link
 * Mp4VideoPlayer}). The world is frozen by the manager while it plays.
 */
public class CutscenePlayer extends UIComponent implements CutsceneView {
  private static final Logger logger = LoggerFactory.getLogger(CutscenePlayer.class);
  private static final float VIDEO_START_TIMEOUT_SECONDS = 5f;

  private VideoActor videoActor;
  private boolean active;
  private Runnable onFinished;
  private Mp4VideoPlayer videoPlayer;
  private float videoElapsed;

  @Override
  public void create() {
    super.create();
    if (stage == null) {
      return;
    }
    videoActor = new VideoActor();
    videoActor.setTouchable(Touchable.disabled);
    videoActor.setVisible(false);
    stage.addActor(videoActor);
  }

  @Override
  public void play(CutsceneScript script, Runnable finished) {
    if (active) {
      stop();
    }
    onFinished = finished;
    active = true;
    if (stage == null) {
      end();
      return;
    }
    if (script.video == null || !Gdx.files.internal(script.video).exists()) {
      logger.error("Cutscene '{}' is missing its video '{}'", script.id, script.video);
      end();
      return;
    }
    try {
      videoPlayer = new Mp4VideoPlayer(Gdx.files.internal(script.video));
      videoElapsed = 0f;
      videoActor.setPlayer(videoPlayer);
      videoActor.setVisible(true);
      videoActor.toFront();
    } catch (IOException | JCodecException | RuntimeException e) {
      // Never leave the player stuck behind a cutscene that cannot play
      logger.error("Could not play cutscene '{}'", script.id, e);
      end();
    }
  }

  @Override
  public void update() {
    if (!active || videoPlayer == null) {
      return;
    }
    float delta = Gdx.graphics.getDeltaTime();
    videoPlayer.update(delta);
    videoElapsed += delta;
    // Safety net: a video that never produces a frame must not leave the game frozen.
    boolean neverStarted = !videoPlayer.hasStarted() && videoElapsed > VIDEO_START_TIMEOUT_SECONDS;
    if (videoPlayer.isFinished() || videoPlayer.hasFailed() || neverStarted) {
      end();
    }
  }

  @Override
  public void stop() {
    if (active) {
      end();
    }
  }

  /** Hides the video, then reports back exactly once. */
  private void end() {
    if (!active) {
      return;
    }
    active = false;
    if (videoActor != null) {
      videoActor.setVisible(false);
      videoActor.setPlayer(null);
    }
    disposeVideo();
    Runnable callback = onFinished;
    onFinished = null;
    if (callback != null) {
      callback.run();
    }
  }

  private void disposeVideo() {
    if (videoPlayer == null) {
      return;
    }
    try {
      videoPlayer.dispose();
    } catch (RuntimeException e) {
      logger.warn("Error while closing video player", e);
    }
    videoPlayer = null;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public float getZIndex() {
    // Drawn after the HUD, which pulls itself to the front every frame.
    return 1000f;
  }

  @Override
  protected void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
    // Scene2D draws the video; keep it above the HUD.
    if (active && videoActor != null) {
      videoActor.toFront();
    }
  }

  @Override
  public void dispose() {
    onFinished = null;
    stop();
    disposeVideo();
    if (videoActor != null) {
      videoActor.remove();
    }
    super.dispose();
  }

  /** Draws the current video frame, scaled to fit the screen on a black background. */
  private static class VideoActor extends Actor {
    private Mp4VideoPlayer player;

    void setPlayer(Mp4VideoPlayer player) {
      this.player = player;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      if (player == null) {
        return;
      }
      float screenW = getStage().getWidth();
      float screenH = getStage().getHeight();
      batch.setColor(0f, 0f, 0f, 1f);
      skin.getDrawable("white").draw(batch, 0f, 0f, screenW, screenH);
      Texture frame = player.getTexture();
      if (frame != null && player.getWidth() > 0 && player.getHeight() > 0) {
        float scale = Math.min(screenW / player.getWidth(), screenH / player.getHeight());
        float w = player.getWidth() * scale;
        float h = player.getHeight() * scale;
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(frame, (screenW - w) / 2f, (screenH - h) / 2f, w, h);
      }
    }
  }
}
