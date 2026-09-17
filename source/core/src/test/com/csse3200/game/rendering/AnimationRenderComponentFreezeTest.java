package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A frozen enemy holds the frame it was caught on rather than carrying on with its idle loop, and
 * the glow pass redraws that same frame instead of running the animation on at double speed.
 */
@ExtendWith(GameExtension.class)
class AnimationRenderComponentFreezeTest {
  private static final String ANIMATION = "idle";
  private static final int FRAMES = 4;

  private GameTime time;
  private SpriteBatch batch;
  private List<TextureRegion> drawn;
  private Array<TextureAtlas.AtlasRegion> regions;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    when(time.getDeltaTime()).thenReturn(1f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));

    regions = new Array<>();
    for (int i = 0; i < FRAMES; i++) {
      regions.add(mock(TextureAtlas.AtlasRegion.class));
    }

    drawn = new ArrayList<>();
    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
    doAnswer(
            invocation -> {
              drawn.add(invocation.getArgument(0));
              return null;
            })
        .when(batch)
        .draw(
            org.mockito.ArgumentMatchers.any(TextureRegion.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());
  }

  /** An animator on an entity carrying the given effects, playing a four frame loop. */
  private AnimationRenderComponent animatorOn(StatusEffectsControllerComponent effects) {
    TextureAtlas atlas = mock(TextureAtlas.class);
    when(atlas.findRegions(ANIMATION)).thenReturn(regions);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation(ANIMATION, 1f, Animation.PlayMode.LOOP);

    Entity entity = new Entity().addComponent(new CombatStatsComponent(100, 10));
    if (effects != null) {
      entity.addComponent(effects);
    }
    entity.addComponent(animator);
    entity.create();
    animator.startAnimation(ANIMATION);
    return animator;
  }

  @Test
  void aNormalEnemyAnimatesOnFrameByFrame() {
    AnimationRenderComponent animator = animatorOn(new StatusEffectsControllerComponent());

    animator.draw(batch);
    animator.draw(batch);
    animator.draw(batch);

    assertSame(regions.get(0), drawn.get(0));
    assertSame(regions.get(1), drawn.get(1));
    assertSame(regions.get(2), drawn.get(2));
  }

  @Test
  void aFrozenEnemyHoldsTheFrameItWasCaughtOn() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    AnimationRenderComponent animator = animatorOn(effects);
    animator.draw(batch);
    animator.draw(batch);

    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    animator.draw(batch);
    animator.draw(batch);
    animator.draw(batch);

    assertSame(regions.get(2), drawn.get(2), "frozen on the frame it had reached");
    assertSame(regions.get(2), drawn.get(3));
    assertSame(regions.get(2), drawn.get(4), "and it stays there");
  }

  @Test
  void aThawedEnemyCarriesOnFromWhereItStopped() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    AnimationRenderComponent animator = animatorOn(effects);
    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    animator.draw(batch);
    animator.draw(batch);

    when(time.getTime()).thenReturn(5000L);
    animator.draw(batch);
    animator.draw(batch);

    assertSame(regions.get(0), drawn.get(1), "held while frozen");
    assertSame(regions.get(0), drawn.get(2), "the thawing frame still shows where it was");
    assertSame(regions.get(1), drawn.get(3), "and then it moves on");
  }

  @Test
  void theGlowPassRedrawsTheSameFrameRatherThanRunningTheAnimationOnTwice() {
    // render() draws twice when something glows. Without this, every animation in the game would
    // play at double speed whenever an effect was on it.
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    AnimationRenderComponent animator = animatorOn(effects);
    effects.addStatusEffect(
        new com.csse3200.game.components.statuseffects.FlashEffect(
            time, 1000L, Color.PURPLE, Color.PURPLE));

    animator.render(batch);

    assertEquals(2, drawn.size(), "a glowing sprite is drawn twice");
    assertSame(drawn.get(0), drawn.get(1), "but both passes show the same frame");
  }

  @Test
  void aGlowingSpriteStillAnimatesOneFramePerRender() {
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    AnimationRenderComponent animator = animatorOn(effects);
    effects.addStatusEffect(
        new com.csse3200.game.components.statuseffects.FlashEffect(
            time, 10000L, Color.PURPLE, Color.PURPLE));

    animator.render(batch);
    animator.render(batch);

    assertSame(regions.get(0), drawn.get(0));
    assertSame(regions.get(0), drawn.get(1));
    assertNotSame(
        drawn.get(1), drawn.get(2), "the next render shows the next frame, not the third");
    assertSame(regions.get(1), drawn.get(2));
  }

  @Test
  void anAnimatorWithNothingPlayingDrawsNothing() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    when(atlas.findRegions(ANIMATION)).thenReturn(regions);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation(ANIMATION, 1f, Animation.PlayMode.LOOP);
    new Entity().addComponent(animator);

    animator.draw(batch);

    assertEquals(0, drawn.size());
  }

  @Test
  void anEntityWithNoStatusEffectsAtAllStillAnimates() {
    AnimationRenderComponent animator = animatorOn(null);

    animator.draw(batch);
    animator.draw(batch);

    assertSame(regions.get(0), drawn.get(0));
    assertSame(regions.get(1), drawn.get(1));
  }
}
