package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import java.util.Map;

/**
 * Renders the minotaur's full attack canvas around its existing walking sprite anchor. Transparent
 * padding and the extended axe affect only drawing, never the entity size or physics fixtures.
 */
public class MinotaurAttackRenderComponent extends RenderComponent {
  public static final String NORMAL_ATTACK = "normalAttack";
  public static final String CHARGE_ATTACK = "chargeAttack";
  public static final String ATLAS = "images/minotaur-combat/attacks.atlas";
  private static final float FRAME_DURATION = 0.1f;
  private static final float TIME_EPSILON = 0.000001f;
  private final Map<String, Animation<TextureRegion>> animations;
  private Animation<TextureRegion> currentAnimation;
  private String currentAnimationName;
  private float playTime;
  private boolean flipX;

  /** Registers A01–A07 and A08–A16 at the author's 100 ms per frame. */
  public MinotaurAttackRenderComponent(TextureAtlas atlas) {
    animations =
        Map.of(
            NORMAL_ATTACK,
                new Animation<>(
                    FRAME_DURATION, atlas.findRegions(NORMAL_ATTACK), Animation.PlayMode.NORMAL),
            CHARGE_ATTACK,
                new Animation<>(
                    FRAME_DURATION, atlas.findRegions(CHARGE_ATTACK), Animation.PlayMode.NORMAL));
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

  /** Advances the one combat/visual clock from the controller, never from a rendering pass. */
  public void advance(float delta) {
    if (currentAnimation == null
        || !Float.isFinite(delta)
        || delta <= 0f
        || StatusEffectsControllerComponent.isImmobilised(entity)) {
      return;
    }
    float nextTime = playTime + delta;
    float impactTime = impactTime();
    if (playTime < impactTime && nextTime + TIME_EPSILON >= impactTime) {
      // Even a long frame must present the strike when it lands, rather than skipping to recovery.
      playTime = impactTime;
    } else {
      float duration = currentAnimation.getAnimationDuration();
      playTime = nextTime + TIME_EPSILON >= duration ? duration : nextTime;
    }
  }

  /** A05 for normal strikes; A11 (fourth frame of A08–A16) for charge strikes. */
  public boolean hasReachedImpactFrame() {
    return currentAnimation != null && playTime >= impactTime();
  }

  private float impactTime() {
    return FRAME_DURATION * (NORMAL_ATTACK.equals(currentAnimationName) ? 4 : 3);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (currentAnimation == null) {
      return;
    }
    TextureRegion frame = currentAnimation.getKeyFrame(playTime);
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
  }
}
