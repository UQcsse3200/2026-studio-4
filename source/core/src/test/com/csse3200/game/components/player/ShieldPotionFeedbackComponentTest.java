package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShieldPotionFeedbackComponentTest {
  private final AtomicLong now = new AtomicLong();
  private final List<float[]> rings = new ArrayList<>();
  private final List<Float> alphas = new ArrayList<>();
  private Entity player;
  private CombatStatsComponent stats;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private ShieldPotionFeedbackComponent feedback;
  private StatusEffectsControllerComponent effects;
  private SpriteBatch batch;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(call -> now.get());
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(new RenderService());
    stats = new CombatStatsComponent(100, 10);
    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    feedback = new ShieldPotionFeedbackComponent();
    effects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables)
            .addComponent(feedback);
    player.setPosition(2f, 3f);
    effects.create();
    consumables.create();
    feedback.create();
    batch = mock(SpriteBatch.class);
    doAnswer(
            call -> {
              alphas.add(call.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    when(batch.getPackedColor()).thenReturn(0.25f);
    when(batch.getBlendSrcFunc()).thenReturn(GL20.GL_SRC_ALPHA);
    when(batch.getBlendDstFunc()).thenReturn(GL20.GL_ONE_MINUS_SRC_ALPHA);
    when(batch.getBlendSrcFuncAlpha()).thenReturn(GL20.GL_ONE);
    when(batch.getBlendDstFuncAlpha()).thenReturn(GL20.GL_ZERO);
    doAnswer(
            call -> {
              rings.add(
                  new float[] {call.getArgument(1), call.getArgument(2), call.getArgument(5)});
              return null;
            })
        .when(batch)
        .draw(
            any(Texture.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyBoolean());
  }

  @Test
  void shieldExpandsFollowsPlayerRefreshesAndEndsAtTheActualDeadline() {
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class)) {
      inventory.addConsumable(ItemIds.SHIELD, 2);
      assertTrue(consumables.tryUse(ItemIds.SHIELD));
      feedback.render(batch);
      float initialWidth = rings.get(0)[2];
      now.set(250);
      feedback.render(batch);
      assertTrue(rings.get(2)[2] > initialWidth);
      player.setPosition(6f, 7f);
      now.set(7999);
      rings.clear();
      alphas.clear();
      feedback.render(batch);
      assertEquals(2, rings.size());
      assertTrue(alphas.stream().allMatch(alpha -> alpha < 0.01f));
      assertEquals(player.getCenterPosition().x, rings.get(0)[0] + rings.get(0)[2] / 2f, 0.00001f);
      assertEquals(player.getCenterPosition().y, rings.get(0)[1] + rings.get(0)[2] / 2f, 0.00001f);
      assertTrue(consumables.tryUse(ItemIds.SHIELD));
      now.set(8000);
      rings.clear();
      feedback.render(batch);
      assertEquals(2, rings.size());
      now.set(15999);
      rings.clear();
      feedback.render(batch);
      assertTrue(rings.isEmpty());
      assertEquals(1, textures.constructed().size());
      feedback.dispose();
      feedback.dispose();
      verify(textures.constructed().get(0), times(1)).dispose();
    }
  }

  @Test
  void rejectedUseDoesNotCreateVisualsAndEarlyRemovalOrDeathHidesThem() {
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class)) {
      assertFalse(consumables.tryUse(ItemIds.SHIELD));
      feedback.render(batch);
      assertTrue(textures.constructed().isEmpty());
      inventory.addConsumable(ItemIds.SHIELD, 2);
      assertTrue(consumables.tryUse(ItemIds.SHIELD));
      feedback.render(batch);
      effects.clearStatusEffects();
      rings.clear();
      feedback.render(batch);
      assertTrue(rings.isEmpty());
      assertTrue(consumables.tryUse(ItemIds.SHIELD));
      stats.setHealth(0);
      feedback.render(batch);
      assertTrue(rings.isEmpty());
      feedback.dispose();
    }
  }

  @Test
  void renderingFailureRestoresBatchState() {
    inventory.addConsumable(ItemIds.SHIELD);
    assertTrue(consumables.tryUse(ItemIds.SHIELD));
    try (var pixels = mockConstruction(Pixmap.class);
        var textures = mockConstruction(Texture.class)) {
      doThrow(new IllegalStateException("draw failed"))
          .when(batch)
          .draw(
              any(Texture.class),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyFloat(),
              anyInt(),
              anyInt(),
              anyInt(),
              anyInt(),
              anyBoolean(),
              anyBoolean());
      assertThrows(IllegalStateException.class, () -> feedback.render(batch));
      verify(batch).setPackedColor(0.25f);
      verify(batch)
          .setBlendFunctionSeparate(
              GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ZERO);
      feedback.dispose();
    }
  }
}
