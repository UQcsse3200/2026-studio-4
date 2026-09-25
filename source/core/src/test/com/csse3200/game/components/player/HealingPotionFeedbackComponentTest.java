package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class HealingPotionFeedbackComponentTest {
  private CombatStatsComponent stats;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private HealingPotionFeedbackComponent feedback;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.3f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(new RenderService());
    stats = new CombatStatsComponent(100, 10);
    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    feedback = new HealingPotionFeedbackComponent();
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    Entity player =
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
  }

  @Test
  void successfulHealingPotionShowsTwoBarsThenFadesAway() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(40);
    inventory.addConsumable(ItemIds.HEALTH_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
      feedback.render(batch);
      Texture pixel = textures.constructed().get(0);
      verify(batch, times(2)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());

      feedback.update();
      feedback.update();
      clearInvocations(batch);
      feedback.render(batch);
      verify(batch, never()).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      feedback.dispose();
      verify(pixel).dispose();
    }
  }

  @Test
  void rejectedHealAndOtherPotionDoNotShowCross() {
    SpriteBatch batch = mock(SpriteBatch.class);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SPEED_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class)) {
      assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));
      assertTrue(consumables.tryUse(ItemIds.SPEED_POTION));
      feedback.render(batch);
      assertTrue(pixels.constructed().isEmpty());
    }
  }

  @Test
  void mediumAndLargeHealingPotionsAlsoShowCross() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(1);
    inventory.addConsumable(ItemIds.MEDIUM_HEALTH_POTION);
    inventory.addConsumable(ItemIds.LARGE_HEALTH_POTION);

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      assertTrue(consumables.tryUse(ItemIds.MEDIUM_HEALTH_POTION));
      feedback.render(batch);
      Texture pixel = textures.constructed().get(0);
      verify(batch, times(2)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());

      stats.setHealth(1);
      assertTrue(consumables.tryUse(ItemIds.LARGE_HEALTH_POTION));
      feedback.render(batch);
      verify(batch, times(4)).draw(eq(pixel), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }
  }

  @Test
  void customHealingPotionShowsTheSameFeedback() {
    SpriteBatch batch = mock(SpriteBatch.class);
    stats.setHealth(10);
    inventory.addConsumable("HEALTH_POTION_75");

    try (MockedConstruction<Pixmap> pixels = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      assertTrue(consumables.tryUse("HEALTH_POTION_75"));
      feedback.render(batch);
      verify(batch, times(2))
          .draw(eq(textures.constructed().get(0)), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }
  }
}
