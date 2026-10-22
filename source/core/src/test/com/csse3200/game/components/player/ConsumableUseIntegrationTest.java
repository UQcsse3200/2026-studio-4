package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableUseIntegrationTest {
  private final AtomicLong now = new AtomicLong();
  private Entity player;
  private InventoryComponent inventory;
  private CombatStatsComponent stats;
  private ConsumableEffectComponent consumables;
  private StatusEffectsControllerComponent effects;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(inv -> now.get());
    ServiceLocator.registerTimeSource(time);
    stats = new CombatStatsComponent(100, 10, 4, 1);
    inventory = new InventoryComponent(0);
    effects = new StatusEffectsControllerComponent();
    consumables = new ConsumableEffectComponent();
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables);
    effects.create();
    consumables.create();
  }

  @Test
  void oneUseRequestHealsAndConsumesExactlyOnce() {
    stats.setHealth(50);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    AtomicInteger used = new AtomicInteger();
    player
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, (String type) -> used.incrementAndGet());
    assertTrue(consumables.tryUse(ItemIds.HEALTH_POTION));
    assertEquals(75, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));
    assertEquals(75, stats.getHealth());
    assertEquals(1, used.get());
  }

  @Test
  void twoPotionsAreNotBothDebitedByOneRequest() {
    stats.setHealth(90);
    inventory.addConsumable(ItemIds.HEALTH_POTION, 2);
    player.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, ItemIds.HEALTH_POTION);
    assertEquals(100, stats.getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void mediumAndLargeInstantPotionsHealTheirOwnAmounts() {
    stats.setHealth(10);
    inventory.addConsumable(ItemIds.MEDIUM_HEALTH_POTION);
    inventory.addConsumable(ItemIds.LARGE_HEALTH_POTION);

    assertTrue(consumables.tryUse(ItemIds.MEDIUM_HEALTH_POTION));
    assertEquals(60, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemIds.MEDIUM_HEALTH_POTION));
    assertTrue(consumables.tryUse(ItemIds.LARGE_HEALTH_POTION));
    assertEquals(100, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemIds.LARGE_HEALTH_POTION));
  }

  @Test
  void customPotionCanBePickedByIdAndUsedWithoutChangingLegacyPotions() {
    String customId = "HEALTH_POTION_75";
    stats.setHealth(10);
    inventory.addConsumable(customId);
    inventory.addConsumable(ItemIds.HEALTH_POTION);

    assertTrue(consumables.tryUse(customId));
    assertEquals(85, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(customId));
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void veryLargePositiveHealingAmountClampsToMaxHealth() {
    String id = "HEALTH_POTION_2147483647";
    stats.setHealth(10);
    inventory.addConsumable(id);

    assertTrue(consumables.tryUse(id));
    assertEquals(100, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(id));
  }

  @Test
  void fullHealthDoesNotConsumeAnyInstantPotionSize() {
    for (String type :
        new String[] {
          ItemIds.HEALTH_POTION, ItemIds.MEDIUM_HEALTH_POTION, ItemIds.LARGE_HEALTH_POTION
        }) {
      inventory.addConsumable(type);
      assertFalse(consumables.tryUse(type));
      assertEquals(1, inventory.getConsumableCount(type));
    }
  }

  @Test
  void fullHealthRejectionFiresUseFailedInsteadOfUsed() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    AtomicInteger used = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    player
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, (String type) -> used.incrementAndGet());
    player
        .getEvents()
        .addListener(
            ConsumableEffectComponent.USE_FAILED, (String type) -> failed.incrementAndGet());

    assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));

    assertEquals(0, used.get());
    assertEquals(1, failed.get());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void invalidOutOfStockOrDeadUsesDoNotFireUseFailed() {
    AtomicInteger failed = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            ConsumableEffectComponent.USE_FAILED, (String type) -> failed.incrementAndGet());

    assertFalse(consumables.tryUse(null));
    assertFalse(consumables.tryUse(ItemIds.GOLD_COIN));
    assertFalse(consumables.tryUse(ItemIds.SHIELD));
    stats.setHealth(0);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    assertFalse(consumables.tryUse(ItemIds.HEALTH_POTION));

    assertEquals(0, failed.get());
  }

  @Test
  void strengthRefreshDoesNotCompoundOrRemoveNewCharm() {
    inventory.addConsumable(ItemIds.STRENGTH_POTION, 2);
    assertTrue(consumables.tryUse(ItemIds.STRENGTH_POTION));
    assertEquals(15, stats.getEffectiveBaseAttack());
    new StrengthCharm().pickUp(player);
    assertEquals(20, stats.getBaseAttack());
    assertEquals(30, stats.getEffectiveBaseAttack());
    now.set(4000);
    assertTrue(consumables.tryUse(ItemIds.STRENGTH_POTION));
    assertEquals(30, stats.getEffectiveBaseAttack());
    now.set(8000);
    effects.update();
    assertEquals(30, stats.getEffectiveBaseAttack());
    now.set(12000);
    effects.update();
    assertEquals(20, stats.getBaseAttack());
    assertEquals(20, stats.getEffectiveBaseAttack());
  }

  @Test
  void speedComposesWithOtherEffectsAndPreservesRawChanges() {
    effects.addStatusEffect(
        new StatusEffect(time, 20000) {
          @Override
          public float getStatMultiplier(Stat stat) {
            return stat == Stat.MOVEMENT_SPEED ? 0.5f : 1f;
          }
        });
    inventory.addConsumable(ItemIds.SPEED_POTION);
    assertTrue(consumables.tryUse(ItemIds.SPEED_POTION));
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    stats.setMovementSpeed(6);
    assertEquals(4.5f, stats.getEffectiveMovementSpeed());
    now.set(8000);
    effects.update();
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    assertEquals(6f, stats.getMovementSpeed());
  }

  @Test
  void speedPotionProgressTracksActualEffectAndRefreshes() {
    assertEquals(0f, consumables.getRemainingFraction(ItemIds.SPEED_POTION));
    inventory.addConsumable(ItemIds.SPEED_POTION, 2);
    assertTrue(consumables.tryUse(ItemIds.SPEED_POTION));
    assertEquals(1f, consumables.getRemainingFraction(ItemIds.SPEED_POTION));

    now.set(4000);
    assertEquals(0.5f, consumables.getRemainingFraction(ItemIds.SPEED_POTION));
    assertTrue(consumables.tryUse(ItemIds.SPEED_POTION));
    assertEquals(1f, consumables.getRemainingFraction(ItemIds.SPEED_POTION));

    now.set(12000);
    assertEquals(0f, consumables.getRemainingFraction(ItemIds.SPEED_POTION));
  }

  @Test
  void invalidDeadAndDisposedUsesKeepInventory() {
    inventory.addConsumable(ItemIds.SHIELD, 3);
    assertFalse(consumables.tryUse(null));
    assertFalse(consumables.tryUse(ItemIds.GOLD_COIN));
    stats.setHealth(0);
    assertFalse(consumables.tryUse(ItemIds.SHIELD));
    stats.setHealth(100);
    consumables.dispose();
    assertFalse(consumables.tryUse(ItemIds.SHIELD));
    assertEquals(3, inventory.getConsumableCount(ItemIds.SHIELD));
  }

  @Test
  void disposalRemovesOnlyConsumableModifiersAndPreservesOtherImmunity() {
    stats.setInvulnerable(true);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.STRENGTH_POTION);
    consumables.tryUse(ItemIds.SHIELD);
    consumables.tryUse(ItemIds.STRENGTH_POTION);
    consumables.dispose();
    assertEquals(10, stats.getEffectiveBaseAttack());
    assertFalse(consumables.isShielded());
    assertTrue(stats.isInvulnerable());
  }

  @Test
  void successfulNotificationDoesNotConsumeAgain() {
    inventory.addConsumable(ItemIds.SHIELD, 2);
    player.getEvents().trigger(ConsumableEffectComponent.USED, ItemIds.SHIELD);
    assertEquals(2, inventory.getConsumableCount(ItemIds.SHIELD));
    assertFalse(consumables.isShielded());
  }

  @Test
  void selectedSlotUsesItsInventoryItemOnce() {
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    player.addComponent(selection).addComponent(input);
    selection.create();
    input.keyDown(Keys.Q);
    assertFalse(consumables.isShielded());
    inventory.addConsumable(ItemIds.SHIELD);
    assertEquals(ItemIds.SHIELD, selection.getSelectedType());
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemIds.SHIELD));
    assertTrue(consumables.isShielded());
  }
}
