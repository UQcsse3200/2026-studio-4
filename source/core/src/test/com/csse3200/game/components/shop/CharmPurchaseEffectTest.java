package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.Charm;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CharmPurchaseEffectTest {
  private final CharmPurchaseEffect effect = new CharmPurchaseEffect();

  @Test
  void repeatedPurchasesCreateIndependentCharmsUsingExistingEquipment() {
    for (String id :
        List.of(ItemIds.STRENGTH_CHARM, ItemIds.SPEED_CHARM, ItemIds.ATTACK_SPEED_CHARM)) {
      InventoryComponent inventory = new InventoryComponent(80);
      CombatStatsComponent stats = new CombatStatsComponent(100, 20);
      new Entity().addComponent(inventory).addComponent(stats);
      int attack = stats.getEffectiveBaseAttack();
      float speed = stats.getEffectiveMovementSpeed();
      float attackSpeed = stats.getEffectiveAttackSpeed();

      assertEquals(ShopPurchaseResult.SUCCESS, effect.purchase(inventory, id, 40));
      assertEquals(ShopPurchaseResult.SUCCESS, effect.purchase(inventory, id, 40));
      assertEquals(0, inventory.getGold());
      assertEquals(2, inventory.getCharmCount());
      Charm first = inventory.getCharms().get(0);
      Charm second = inventory.getCharms().get(1);
      assertNotSame(first, second);
      assertEquals(id, first.getId());
      assertEquals(id, second.getId());
      assertFalse(first.isEquipped());
      assertFalse(second.isEquipped());
      inventory.setCharmEquipped(first, true);
      inventory.setCharmEquipped(first, true);
      assertEquals(
          attack + (id.equals(ItemIds.STRENGTH_CHARM) ? 10 : 0), stats.getEffectiveBaseAttack());
      assertEquals(
          speed + (id.equals(ItemIds.SPEED_CHARM) ? 1 : 0), stats.getEffectiveMovementSpeed());
      assertEquals(
          attackSpeed + (id.equals(ItemIds.ATTACK_SPEED_CHARM) ? 1 : 0),
          stats.getEffectiveAttackSpeed());
      inventory.setCharmEquipped(first, false);
      assertEquals(attack, stats.getEffectiveBaseAttack());
      assertEquals(speed, stats.getEffectiveMovementSpeed());
      assertEquals(attackSpeed, stats.getEffectiveAttackSpeed());
      assertEquals(ShopPurchaseResult.INSUFFICIENT_GOLD, effect.purchase(inventory, id, 40));
      assertEquals(2, inventory.getCharmCount());
    }
  }

  @Test
  void rejectionLeavesBalanceOwnershipAndEventsUntouched() {
    InventoryComponent inventory = new InventoryComponent(39);
    Entity player = new Entity().addComponent(inventory);
    player
        .getEvents()
        .<Integer>addListener("goldChanged", gold -> fail("Rejected purchase changed gold"));
    player
        .getEvents()
        .<Charm>addListener("charmAdded", charm -> fail("Rejected purchase added a charm"));
    for (String id : new String[] {null, "missing", ItemIds.HEALTH_POTION, ItemIds.GOLD_COIN}) {
      assertEquals(ShopPurchaseResult.INVALID_OFFER, effect.purchase(inventory, id, 1));
    }
    for (int price : new int[] {0, -1, Integer.MIN_VALUE}) {
      assertEquals(
          ShopPurchaseResult.INVALID_OFFER,
          effect.purchase(inventory, ItemIds.STRENGTH_CHARM, price));
    }
    assertEquals(
        ShopPurchaseResult.INSUFFICIENT_GOLD,
        effect.purchase(inventory, ItemIds.STRENGTH_CHARM, 40));
    assertEquals(39, inventory.getGold());
    assertEquals(0, inventory.getCharmCount());
  }

  @Test
  void bothEventsObserveCompletedPaymentAndOwnership() {
    InventoryComponent inventory = new InventoryComponent(50);
    Entity player = new Entity().addComponent(inventory);
    List<String> observations = new ArrayList<>();
    player
        .getEvents()
        .<Integer>addListener(
            "goldChanged",
            gold -> {
              assertEquals(10, gold);
              observations.add(inventory.getGold() + ":" + inventory.getCharmCount());
            });
    player
        .getEvents()
        .<Charm>addListener(
            "charmAdded",
            charm -> {
              assertSame(charm, inventory.getCharms().getFirst());
              observations.add(inventory.getGold() + ":" + inventory.getCharmCount());
            });
    assertEquals(
        ShopPurchaseResult.SUCCESS, effect.purchase(inventory, ItemIds.STRENGTH_CHARM, 40));
    assertEquals(List.of("10:1", "10:1"), observations);
  }
}
