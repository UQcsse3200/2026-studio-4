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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeShieldComponentTest {
  private static final AtomicInteger TEXTURE_HANDLES = new AtomicInteger(1000);
  private final List<Draw> draws = new ArrayList<>();
  private final Color batchColour = new Color(0.7f, 0.2f, 0.3f, 0.6f);
  private Entity player;
  private Entity snake;
  private SnakeShieldComponent shield;
  private GameTime time;
  private RenderService renderer;
  private ResourceService resources;
  private SpriteBatch batch;

  @BeforeEach
  void setUp() {
    when(Gdx.gl.glGenTexture()).thenAnswer(ignored -> TEXTURE_HANDLES.incrementAndGet());
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    resources = new ResourceService();
    resources.loadTextures(new String[] {SnakeShieldComponent.SHIELD_TEXTURE});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    player = new Entity().addComponent(new CombatStatsComponent(20, 1));
    player.setPosition(4f, 7f);
    player.setScale(2f, 4f);
    shield = new SnakeShieldComponent(player, new SnakeMiniBossConfig());
    snake = new Entity().addComponent(new CombatStatsComponent(150, 1)).addComponent(shield);
    snake.setPosition(90f, 90f);
    shield.create();
    configureBatch();
  }

  @AfterEach
  void cleanUp() {
    shield.dispose();
    resources.dispose();
  }

  @Test
  void refillsOneCapacityAndOnlyActualBlocksSpendDurability() {
    assertEquals(6, shield.getCapacity());
    assertFalse(shield.isActive());
    assertFalse(shield.tryBlock());
    render();
    assertTrue(draws.isEmpty());
    shield.refill();
    tick(100f);
    assertEquals(6, shield.getDurability());
    assertTrue(shield.tryBlock());
    assertEquals(5, shield.getDurability());
    shield.refill();
    assertEquals(6, shield.getDurability());
    for (int i = 0; i < 6; i++) {
      assertTrue(shield.tryBlock());
    }
    assertFalse(shield.tryBlock());
    assertFalse(shield.isActive());
    render();
    assertTrue(draws.isEmpty());
    shield.refill();
    shield.clear();
    assertEquals(0, shield.getDurability());
    shield.refill();
    assertTrue(shield.isActive());
  }

  @Test
  void followsThePlayerWithOneVerticalBarWhoseFillTracksDurability() {
    shield.refill();
    render();
    assertEquals(4, draws.size(), "one shield and one border/background/fill bar");
    Draw circle = draws.getFirst();
    assertSame(
        resources.getAsset(SnakeShieldComponent.SHIELD_TEXTURE, Texture.class), circle.texture());
    assertEquals(3.2f, shield.getRadius(), 0.00001f);
    assertEquals(1.8f, circle.x(), 0.00001f);
    assertEquals(5.8f, circle.y(), 0.00001f);
    assertEquals(6.4f, circle.width(), 0.00001f);
    assertEquals(6.4f, circle.height(), 0.00001f);
    assertTrue(draws.get(1).x() > player.getPosition().x + player.getScale().x);
    assertEquals(draws.get(2).height(), draws.get(3).height(), 0.00001f);

    shield.tryBlock();
    shield.tryBlock();
    player.setPosition(10f, 12f);
    snake.setPosition(-100f, -100f);
    render();
    assertEquals(4, draws.size());
    assertEquals(7.8f, draws.getFirst().x(), 0.00001f);
    assertEquals(10.8f, draws.getFirst().y(), 0.00001f);
    assertEquals(4f / 6f, draws.get(3).height() / draws.get(2).height(), 0.00001f);
    assertEquals(draws.get(2).y(), draws.get(3).y(), "the remaining fill stays at the bottom");
    shield.refill();
    render();
    assertEquals(4, draws.size(), "another gem refills rather than adding a second bar");
  }

  @Test
  void flashesGreenOnBlockWithoutInheritingTintsOrDrainingOverTime() {
    StatusEffectsControllerComponent snakeEffects = addRedAppearance(snake);
    addRedAppearance(player);
    shield.setVisualSource(player);
    shield.refill();
    float originalColour = batchColour.toFloatBits();
    render();
    assertEquals(new Color(1f, 1f, 1f, 0.42f), draws.getFirst().colour());
    assertEquals(originalColour, batch.getPackedColor());
    shield.tryBlock();
    render();
    float blockAlpha = draws.getFirst().colour().a;
    assertTrue(blockAlpha > 0.42f);
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      tick(delta);
    }
    when(snakeEffects.isImmobilised()).thenReturn(true);
    tick(1f);
    render();
    assertEquals(blockAlpha, draws.getFirst().colour().a);
    when(snakeEffects.isImmobilised()).thenReturn(false);
    tick(0.16f);
    render();
    assertEquals(0.42f, draws.getFirst().colour().a, 0.00001f);
    assertEquals(5, shield.getDurability());
    assertEquals(4, draws.size(), "tint and glow must not add an extra shield or bar pass");
    assertEquals(originalColour, batch.getPackedColor());
  }

  @Test
  void restoresBatchColourWhenDrawingThrows() {
    IllegalStateException failure = new IllegalStateException("draw failed");
    doThrow(failure)
        .when(batch)
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    shield.refill();
    float originalColour = batchColour.toFloatBits();

    assertSame(failure, assertThrows(IllegalStateException.class, () -> shield.render(batch)));
    assertEquals(originalColour, batch.getPackedColor());
  }

  @Test
  void playerDeathStopsTheShieldOnQueryBeforeTheNextUpdate() {
    shield.refill();
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    assertFalse(shield.isActive());
    player.getComponent(CombatStatsComponent.class).setHealth(20);
    assertCannotReactivate();
  }

  @Test
  void snakeDeathPermanentlyStopsTheShieldThroughItsOwnListener() {
    shield.refill();
    snake.getComponent(CombatStatsComponent.class).setHealth(0);
    snake.getComponent(CombatStatsComponent.class).setHealth(150);
    assertCannotReactivate();
  }

  @Test
  void disposalReleasesOnlyTheOwnedPixelOnceAndCannotReactivate() {
    shield.refill();
    render();
    Texture shared = draws.getFirst().texture();
    Texture owned = draws.get(1).texture();
    int sharedHandle = shared.getTextureObjectHandle();
    int ownedHandle = owned.getTextureObjectHandle();
    assertTrue(sharedHandle > 0);
    assertTrue(ownedHandle > 0);
    assertEquals(1, owned.getWidth());
    assertEquals(1, owned.getHeight());
    shield.dispose();
    shield.dispose();
    assertEquals(0, owned.getTextureObjectHandle());
    assertEquals(sharedHandle, shared.getTextureObjectHandle());
    assertTrue(resources.containsAsset(SnakeShieldComponent.SHIELD_TEXTURE, Texture.class));
    verify(Gdx.gl, times(1)).glDeleteTexture(ownedHandle);
    verify(renderer, times(1)).unregister(shield);
    assertCannotReactivate();
  }

  private void assertCannotReactivate() {
    shield.refill();
    assertFalse(shield.tryBlock());
    assertFalse(shield.isActive());
    assertEquals(0, shield.getDurability());
    render();
    assertTrue(draws.isEmpty());
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    shield.update();
  }

  private void render() {
    draws.clear();
    shield.render(batch);
  }

  private static StatusEffectsControllerComponent addRedAppearance(Entity entity) {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    when(effects.getTint()).thenReturn(new Color(1f, 0.2f, 0.2f, 1f));
    when(effects.getGlow()).thenReturn(Color.RED);
    entity.addComponent(effects);
    return effects;
  }

  private void configureBatch() {
    batch = mock(SpriteBatch.class);
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
                  new Draw(
                      invocation.getArgument(0),
                      invocation.getArgument(1),
                      invocation.getArgument(2),
                      invocation.getArgument(3),
                      invocation.getArgument(4),
                      new Color(batchColour)));
              return null;
            })
        .when(batch)
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  private record Draw(Texture texture, float x, float y, float width, float height, Color colour) {}
}
