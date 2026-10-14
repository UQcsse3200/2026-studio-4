package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Map;

/**
 * Renders the minotaur's full attack canvas around its existing walking sprite anchor. Transparent
 * padding and the extended axe affect only drawing, never the entity size or physics fixtures.
 */
public class MinotaurAttackRenderComponent extends RenderComponent {
  public static final String NORMAL_ATTACK = "normalAttack";
  public static final String CHARGE_ATTACK = "chargeAttack";
  public static final String ATLAS = "images/minotaur-combat/attacks.atlas";
  private final Map<String, Animation<TextureRegion>> animations;
  private final GameTime timeSource;
  private Animation<TextureRegion> currentAnimation;
  private String currentAnimationName;
  private float playTime;
  private float drawnPlayTime;
  private boolean flipX;

  /** Registers A01–A07 and A08–A16 at the author's 100 ms per frame. */
  public MinotaurAttackRenderComponent(TextureAtlas atlas) {
    animations =
        Map.of(
            NORMAL_ATTACK,
                new Animation<>(0.1f, atlas.findRegions(NORMAL_ATTACK), Animation.PlayMode.NORMAL),
            CHARGE_ATTACK,
                new Animation<>(0.1f, atlas.findRegions(CHARGE_ATTACK), Animation.PlayMode.NORMAL));
    timeSource = ServiceLocator.getTimeSource();
  }

  /** Starts a visual strike; facing remains fixed for the duration of that strike. */
  public void startAnimation(String name, boolean flipX) {
    Animation<TextureRegion> animation = animations.get(name);
    if (animation == null) {
      throw new IllegalArgumentException("Unknown minotaur attack: " + name);
    }
    currentAnimation = animation;
    currentAnimationName = name;
    this.flipX = flipX;
    playTime = 0f;
    drawnPlayTime = 0f;
  }

  /**
   * Removes only the attack drawing; ownership of the original walk/death sprite stays separate.
   */
  public void stopAnimation() {
    currentAnimation = null;
    currentAnimationName = null;
    playTime = 0f;
  }

  public String getCurrentAnimation() {
    return currentAnimationName;
  }

  public TextureRegion getCurrentFrame() {
    return currentAnimation == null ? null : currentAnimation.getKeyFrame(playTime);
  }

  public boolean isFinished() {
    return currentAnimation != null && currentAnimation.isAnimationFinished(playTime);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (currentAnimation == null) {
      return;
    }
    TextureRegion frame = currentAnimation.getKeyFrame(isRepeatPass() ? drawnPlayTime : playTime);
    if (flipX) {
      frame = new TextureRegion(frame);
      frame.flip(true, false);
    }
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    // The original 148 x 130 walk is crop (70, 30, 218, 160) of this 288 x 160 canvas.
    // Both share the same bottom edge and horizontal centre, including when mirrored.
    batch.draw(
        frame,
        position.x - scale.x * 70f / 148f,
        position.y,
        scale.x * 288f / 148f,
        scale.y * 160f / 130f);
    if (!isRepeatPass()) {
      drawnPlayTime = playTime;
      if (!StatusEffectsControllerComponent.isImmobilised(entity)) {
        playTime += timeSource.getDeltaTime();
      }
    }
  }
}
