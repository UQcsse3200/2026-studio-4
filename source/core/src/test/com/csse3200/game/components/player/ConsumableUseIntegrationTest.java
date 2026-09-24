package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMistComponent;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
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
    inventory.addConsumable(ItemType.HEALTH_POTION);
    AtomicInteger used = new AtomicInteger();
    player
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, (ItemType type) -> used.incrementAndGet());
    assertTrue(consumables.tryUse(ItemType.HEALTH_POTION));
    assertEquals(75, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemType.HEALTH_POTION));
    assertFalse(consumables.tryUse(ItemType.HEALTH_POTION));
    assertEquals(75, stats.getHealth());
    assertEquals(1, used.get());
  }

  @Test
  void twoPotionsAreNotBothDebitedByOneRequest() {
    stats.setHealth(90);
    inventory.addConsumable(ItemType.HEALTH_POTION, 2);
    player.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, ItemType.HEALTH_POTION);
    assertEquals(100, stats.getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
    assertFalse(consumables.tryUse(ItemType.HEALTH_POTION));
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void mediumAndLargeInstantPotionsHealTheirOwnAmounts() {
    stats.setHealth(10);
    inventory.addConsumable(ItemType.MEDIUM_HEALTH_POTION);
    inventory.addConsumable(ItemType.LARGE_HEALTH_POTION);

    assertTrue(consumables.tryUse(ItemType.MEDIUM_HEALTH_POTION));
    assertEquals(60, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemType.MEDIUM_HEALTH_POTION));
    assertTrue(consumables.tryUse(ItemType.LARGE_HEALTH_POTION));
    assertEquals(100, stats.getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemType.LARGE_HEALTH_POTION));
  }

  @Test
  void fullHealthDoesNotConsumeAnyInstantPotionSize() {
    for (ItemType type :
        new ItemType[] {
          ItemType.HEALTH_POTION, ItemType.MEDIUM_HEALTH_POTION, ItemType.LARGE_HEALTH_POTION
        }) {
      inventory.addConsumable(type);
      assertFalse(consumables.tryUse(type));
      assertEquals(1, inventory.getConsumableCount(type));
    }
  }

  @Test
  void shieldBlocksActualDamageAndExpiresBeforeControllerTick() {
    inventory.addConsumable(ItemType.SHIELD);
    assertTrue(consumables.tryUse(ItemType.SHIELD));
    assertTrue(consumables.isShielded());
    stats.takeDamage(30, new Entity());
    assertEquals(100, stats.getHealth());
    now.set(8000);
    assertFalse(consumables.isShielded());
    stats.takeDamage(30);
    assertEquals(70, stats.getHealth());
  }

  @Test
  void strengthRefreshDoesNotCompoundOrRemoveNewCharm() {
    inventory.addConsumable(ItemType.STRENGTH_POTION, 2);
    assertTrue(consumables.tryUse(ItemType.STRENGTH_POTION));
    assertEquals(15, stats.getEffectiveBaseAttack());
    new StrengthCharm().pickUp(player);
    assertEquals(20, stats.getBaseAttack());
    assertEquals(30, stats.getEffectiveBaseAttack());
    now.set(4000);
    assertTrue(consumables.tryUse(ItemType.STRENGTH_POTION));
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
        new TimedStatusEffect(time, 20000) {
          @Override
          public float getStatMultiplier(Stat stat) {
            return stat == Stat.MOVEMENT_SPEED ? 0.5f : 1f;
          }
        });
    inventory.addConsumable(ItemType.SPEED_POTION);
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    stats.setMovementSpeed(6);
    assertEquals(4.5f, stats.getEffectiveMovementSpeed());
    now.set(8000);
    effects.update();
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    assertEquals(6f, stats.getMovementSpeed());
  }

  @Test
  void speedPotionAndCerberusMistHaveIndependentLifetimes() {
    PlayerCerberusMistDebuffComponent mist = new PlayerCerberusMistDebuffComponent();
    player.addComponent(mist);
    mist.create();
    Entity source = new Entity();
    inventory.addConsumable(ItemType.SPEED_POTION, 2);
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));
    assertEquals(6f, stats.getEffectiveMovementSpeed());
    player.getEvents().trigger(CerberusMistComponent.ENTERED, source);
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    player.getEvents().trigger(CerberusMistComponent.EXITED, source);
    assertEquals(6f, stats.getEffectiveMovementSpeed());
    player.getEvents().trigger(CerberusMistComponent.ENTERED, source);
    now.set(8000);
    effects.update();
    assertEquals(2f, stats.getEffectiveMovementSpeed());
    assertTrue(mist.isMistDebuffed());
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    player.getEvents().trigger(CerberusMistComponent.EXITED, source);
    assertEquals(6f, stats.getEffectiveMovementSpeed());
    assertEquals(0, inventory.getConsumableCount(ItemType.SPEED_POTION));
    now.set(16000);
    effects.update();
    assertEquals(4f, stats.getEffectiveMovementSpeed());
    mist.dispose();
  }

  @Test
  void invalidDeadAndDisposedUsesKeepInventory() {
    inventory.addConsumable(ItemType.SHIELD, 3);
    assertFalse(consumables.tryUse(null));
    assertFalse(consumables.tryUse(ItemType.GOLD_COIN));
    stats.setHealth(0);
    assertFalse(consumables.tryUse(ItemType.SHIELD));
    stats.setHealth(100);
    consumables.dispose();
    assertFalse(consumables.tryUse(ItemType.SHIELD));
    assertEquals(3, inventory.getConsumableCount(ItemType.SHIELD));
  }

  @Test
  void disposalRemovesOnlyConsumableModifiersAndPreservesOtherImmunity() {
    stats.setInvulnerable(true);
    inventory.addConsumable(ItemType.SHIELD);
    inventory.addConsumable(ItemType.STRENGTH_POTION);
    consumables.tryUse(ItemType.SHIELD);
    consumables.tryUse(ItemType.STRENGTH_POTION);
    consumables.dispose();
    assertEquals(10, stats.getEffectiveBaseAttack());
    assertFalse(consumables.isShielded());
    assertTrue(stats.isInvulnerable());
  }

  @Test
  void successfulNotificationDoesNotConsumeAgain() {
    inventory.addConsumable(ItemType.SHIELD, 2);
    player.getEvents().trigger(ConsumableEffectComponent.USED, ItemType.SHIELD);
    assertEquals(2, inventory.getConsumableCount(ItemType.SHIELD));
    assertFalse(consumables.isShielded());
  }

  @Test
  void selectedSlotUsesItsInventoryItemOnce() {
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    player.addComponent(selection).addComponent(input);
    selection.create();
    inventory.addConsumable(ItemType.SHIELD);

    input.keyDown(Keys.Q);
    assertEquals(1, inventory.getConsumableCount(ItemType.SHIELD));
    input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemType.SHIELD));
    assertTrue(consumables.isShielded());
  }
}
