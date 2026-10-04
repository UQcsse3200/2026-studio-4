package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakePlayerHitVisualComponentTest {
  private final List<DrawCall> draws = new ArrayList<>();
  private final Color batchColour = new Color(0.6f, 0.2f, 0.3f, 0.7f);
  private long now;
  private Entity target;
  private Entity snake;
  private SnakePlayerHitVisualComponent visual;
  private SpriteBatch batch;
  private ResourceService resources;
  private RenderService renderer;

  @BeforeEach
  void setUp() {
    now = 1000L;
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(ignored -> now);
    ServiceLocator.registerTimeSource(time);
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    resources = new ResourceService();
    resources.loadTextures(new String[] {SnakePlayerHitVisualComponent.HIT_SHEET});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);

    target = new Entity().addComponent(new CombatStatsComponent(20, 1));
    target.setPosition(4f, 7f);
    target.setScale(2f, 4f);
    visual = new SnakePlayerHitVisualComponent(target);
    snake = new Entity().addComponent(new CombatStatsComponent(20, 1)).addComponent(visual);
    snake.setPosition(90f, 90f);
    visual.create();

    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(batchColour);
    when(batch.getPackedColor()).thenAnswer(ignored -> batchColour.toFloatBits());
    doAnswer(
            invocation -> {
              batchColour.set(
                  invocation.getArgument(0),
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    doAnswer(
            invocation -> {
              Color.abgr8888ToColor(batchColour, (float) invocation.getArgument(0));
              return null;
            })
        .when(batch)
        .setPackedColor(anyFloat());
    doAnswer(
            invocation -> {
              draws.add(
                  new DrawCall(
                      invocation.getArgument(0),
                      invocation.getArgument(1),
                      invocation.getArgument(2),
                      invocation.getArgument(3),
                      invocation.getArgument(4),
                      new Color(batchColour)));
              return null;
            })
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @AfterEach
  void releaseTextures() {
    resources.dispose();
  }

  @Test
  void shouldPlayAllFourteenGreenFramesAndStopAt560Millis() {
    visual.render(batch);
    assertTrue(draws.isEmpty());
    visual.play();
    Texture sheet = resources.getAsset(SnakePlayerHitVisualComponent.HIT_SHEET, Texture.class);
    assertEquals(896, sheet.getWidth());
    assertEquals(576, sheet.getHeight());

    for (int frame = 0; frame < 14; frame++) {
      for (long offset : new long[] {0L, 39L}) {
        now = 1000L + frame * 40L + offset;
        visual.render(batch);
        TextureRegion region = draws.getLast().region();
        assertSame(sheet, region.getTexture());
        assertEquals(frame * 64, region.getRegionX());
        assertEquals(192, region.getRegionY());
        assertEquals(64, region.getRegionWidth());
        assertEquals(64, region.getRegionHeight());
      }
    }

    now = 1560L;
    visual.update();
    visual.render(batch);
    assertFalse(visual.isPlaying());
    assertEquals(28, draws.size());
  }

  @Test
  void shouldFollowTheMovingPlayerRatherThanTheSnake() {
    visual.play();
    visual.render(batch);
    DrawCall first = draws.getLast();
    assertEquals(2f, first.x());
    assertEquals(6f, first.y());
    assertEquals(6f, first.width());
    assertEquals(6f, first.height());

    target.setPosition(10f, 12f);
    snake.setPosition(-100f, -100f);
    now += 80L;
    visual.render(batch);
    DrawCall moved = draws.getLast();
    assertEquals(8f, moved.x());
    assertEquals(11f, moved.y());
    assertEquals(Float.POSITIVE_INFINITY, visual.getZIndex());
  }

  @Test
  void shouldPreserveGreenArtworkAndBatchColourDespiteEntityTintAndGlow() {
    addRedAppearance(snake);
    addRedAppearance(target);
    float originalPackedColour = batchColour.toFloatBits();
    visual.play();
    visual.render(batch);
    visual.setVisualSource(target);
    visual.render(batch);

    assertEquals(2, draws.size(), "entity glow must not add a second impact pass");
    for (DrawCall draw : draws) {
      assertEquals(new Color(1f, 1f, 1f, 0.85f), draw.colour());
    }
    assertEquals(originalPackedColour, batch.getPackedColor());
  }

  @Test
  void shouldRestoreBatchColourEvenIfDrawingFails() {
    IllegalStateException failure = new IllegalStateException("draw failed");
    doThrow(failure)
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    float originalPackedColour = batchColour.toFloatBits();
    visual.play();

    assertSame(failure, assertThrows(IllegalStateException.class, () -> visual.render(batch)));
    assertEquals(originalPackedColour, batch.getPackedColor());
  }

  @Test
  void shouldRestartTheSingleImpactOnAnotherHit() {
    visual.play();
    now += 320L;
    visual.render(batch);
    assertEquals(512, draws.getLast().region().getRegionX());
    visual.play();
    visual.render(batch);
    assertEquals(0, draws.getLast().region().getRegionX());
    now += 559L;
    assertTrue(visual.isPlaying());
    now++;
    assertFalse(visual.isPlaying());
    verify(renderer).register(visual);
  }

  @Test
  void shouldStopAfterPlayerDeath() {
    assertStops(() -> target.getComponent(CombatStatsComponent.class).setHealth(0));
  }

  @Test
  void shouldStopAfterSnakeDeath() {
    assertStops(() -> snake.getComponent(CombatStatsComponent.class).setHealth(0));
  }

  @Test
  void shouldStopAndUnregisterAfterDisposal() {
    assertStops(visual::dispose);
    verify(renderer).unregister(visual);
  }

  private void assertStops(Runnable endEncounter) {
    visual.play();
    visual.render(batch);
    endEncounter.run();

    visual.update();
    assertFalse(visual.isPlaying());
    visual.play();
    visual.render(batch);
    assertFalse(visual.isPlaying());
    assertEquals(1, draws.size());
    assertTrue(resources.containsAsset(SnakePlayerHitVisualComponent.HIT_SHEET, Texture.class));
  }

  private static void addRedAppearance(Entity entity) {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    when(effects.getTint()).thenReturn(new Color(1f, 0.2f, 0.2f, 1f));
    when(effects.getGlow()).thenReturn(Color.RED);
    entity.addComponent(effects);
  }

  private record DrawCall(
      TextureRegion region, float x, float y, float width, float height, Color colour) {}
}
