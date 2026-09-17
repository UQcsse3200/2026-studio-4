package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableVisualComponentTest {
  private Entity player;
  private ConsumableVisualComponent visual;
  private ConsumableEffectComponent effects;
  private InventoryComponent inventory;
  private CombatStatsComponent stats;
  private AtomicLong now;
  private SpriteBatch batch;

  @BeforeEach
  void setup() {
    now = new AtomicLong();
    GameTime clock = mock(GameTime.class);
    when(clock.getTime()).thenAnswer(inv -> now.get());
    ServiceLocator.registerTimeSource(clock);
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerEntityService(new EntityService());
    visual = new ConsumableVisualComponent();
    stats = new CombatStatsComponent(100, 10);
    inventory = new InventoryComponent(0);
    effects = new ConsumableEffectComponent();
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(inventory)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(effects)
            .addComponent(visual);
    player.create();
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(123f);
  }

  @AfterEach
  void cleanup() {
    player.dispose();
  }

  @Test
  void successfulHealingDrawsExactlyThreePlusesThenExpires() {
    stats.setHealth(50);
    inventory.addConsumable(ItemType.HEALTH_POTION);
    assertTrue(effects.tryUse(ItemType.HEALTH_POTION));
    visual.draw(batch);
    verify(batch, times(3))
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    verify(batch).setPackedColor(123f);
    clearInvocations(batch);
    now.set(1200);
    visual.draw(batch);
    verifyNoInteractions(batch);
  }

  @Test
  void failedUseAndDeathDoNotShowHealingFeedback() {
    inventory.addConsumable(ItemType.HEALTH_POTION);
    assertFalse(effects.tryUse(ItemType.HEALTH_POTION));
    visual.draw(batch);
    verifyNoInteractions(batch);
    stats.setHealth(50);
    inventory.removeConsumable(ItemType.HEALTH_POTION);
    assertFalse(effects.tryUse(ItemType.HEALTH_POTION));
    visual.draw(batch);
    verifyNoInteractions(batch);
    inventory.addConsumable(ItemType.HEALTH_POTION);
    assertTrue(effects.tryUse(ItemType.HEALTH_POTION));
    stats.setHealth(0);
    visual.draw(batch);
    verifyNoInteractions(batch);
  }

  @Test
  void shieldVisualFollowsRefreshExpiryAndEarlyRemoval() {
    inventory.addConsumable(ItemType.SHIELD, 3);
    assertTrue(effects.tryUse(ItemType.SHIELD));
    visual.draw(batch);
    verify(batch)
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
            eq(false),
            eq(false));
    now.set(7000);
    assertTrue(effects.tryUse(ItemType.SHIELD));
    now.set(8000);
    clearInvocations(batch);
    visual.draw(batch);
    verify(batch).setPackedColor(123f);
    now.set(15000);
    clearInvocations(batch);
    visual.draw(batch);
    verifyNoInteractions(batch);
    assertTrue(effects.tryUse(ItemType.SHIELD));
    player.getComponent(StatusEffectsControllerComponent.class).clearStatusEffects();
    visual.draw(batch);
    verifyNoInteractions(batch);
  }
}
