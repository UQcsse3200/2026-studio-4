package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

/** Draws a speaker's portrait (an animation or a still image) scaled to fit, keeping its shape. */
class PortraitActor extends Actor {
  private Animation<TextureRegion> animation;
  private TextureRegion still;
  private float stateTime;

  /** Shows an animated portrait. */
  void setAnimation(Array<? extends TextureRegion> frames, float frameSeconds) {
    this.animation = new Animation<>(frameSeconds, frames, Animation.PlayMode.LOOP);
    this.still = null;
    this.stateTime = 0f;
  }

  /** Shows a still portrait. */
  void setStill(TextureRegion region) {
    this.still = region;
    this.animation = null;
  }

  void clearPortrait() {
    animation = null;
    still = null;
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    stateTime += delta;
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    TextureRegion region = animation != null ? animation.getKeyFrame(stateTime) : still;
    if (region == null || region.getRegionHeight() == 0) {
      return;
    }
    float scale =
        Math.min(getWidth() / region.getRegionWidth(), getHeight() / region.getRegionHeight());
    float w = region.getRegionWidth() * scale;
    float h = region.getRegionHeight() * scale;
    batch.setColor(1f, 1f, 1f, parentAlpha);
    batch.draw(region, getX() + (getWidth() - w) / 2f, getY() + (getHeight() - h) / 2f, w, h);
  }
}
