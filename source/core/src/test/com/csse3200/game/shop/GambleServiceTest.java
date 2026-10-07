package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GambleServiceTest {
  private static Random fixed(int value) {
    return new Random(42) {
      @Override
      public int nextInt(int bound) {
        return value;
      }
    };
  }

  private static GambleService game(InventoryComponent inventory, GambleEntry entry) {
    return new GambleService(
        new GambleTable(List.of(entry)), new InventoryGambleWallet(inventory), 10, fixed(0));
  }

  @Test
  void winningRewardAndDebitAreVisibleTogetherToInventoryListeners() {
    InventoryComponent inventory = new InventoryComponent(50);
    new Entity().addComponent(inventory);
    inventory
        .getEntity()
        .getEvents()
        .addListener(
            "goldChanged",
            (Integer gold) -> {
              assertEquals(40, gold);
              assertEquals(3, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
            });
    assertEquals(
        GambleResult.Status.WON,
        game(inventory, GambleEntry.of(ItemIds.HEALTH_POTION, 3, 1)).spin().status());
    assertEquals(40, inventory.getGold());
    assertEquals(3, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void invalidRewardDoesNotChargeOrClaimAWin() {
    InventoryComponent inventory = new InventoryComponent(50);
    GambleResult result = game(inventory, GambleEntry.of("health_potion", 1, 1)).spin();
    assertNotEquals(GambleResult.Status.WON, result.status());
    assertEquals(50, inventory.getGold());
  }

  @Test
  void rewardQuantityOverflowDoesNotChargeOrRemoveExistingItems() {
    InventoryComponent inventory = new InventoryComponent(50);
    inventory.addConsumable(ItemIds.HEALTH_POTION, Integer.MAX_VALUE);
    GambleResult result = game(inventory, GambleEntry.of(ItemIds.HEALTH_POTION, 2, 1)).spin();
    assertNotEquals(GambleResult.Status.WON, result.status());
    assertEquals(50, inventory.getGold());
    assertEquals(Integer.MAX_VALUE, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void normalBustChargesOnceWithoutGrantingItems() {
    InventoryComponent inventory = new InventoryComponent(50);
    assertEquals(GambleResult.Status.BUST, game(inventory, GambleEntry.bust(1)).spin().status());
    assertEquals(40, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void insufficientFundsDoesNotRollOrMutateInventory() {
    InventoryComponent inventory = new InventoryComponent(5);
    Random random =
        new Random() {
          @Override
          public int nextInt(int bound) {
            fail("No affordable draw must roll");
            return 0;
          }
        };
    GambleService game =
        new GambleService(
            new GambleTable(List.of(GambleEntry.bust(1))),
            new InventoryGambleWallet(inventory),
            10,
            random);
    assertEquals(GambleResult.Status.INSUFFICIENT_FUNDS, game.spin().status());
    assertEquals(5, inventory.getGold());
  }

  @Test
  void nonPositiveCostCannotMintGold() {
    InventoryComponent inventory = new InventoryComponent(50);
    GambleTable table = new GambleTable(List.of(GambleEntry.bust(1)));
    for (int cost : new int[] {0, -10}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GambleService(table, new InventoryGambleWallet(inventory), cost, fixed(0)));
    }
    assertEquals(50, inventory.getGold());
  }

  @Test
  void weightsSelectEveryBoundaryAndRejectOverflow() {
    GambleEntry win = GambleEntry.of(ItemIds.HEALTH_POTION, 1, 2);
    GambleEntry bust = GambleEntry.bust(3);
    GambleTable table = new GambleTable(List.of(win, bust));
    assertEquals(win, table.roll(fixed(0)));
    assertEquals(win, table.roll(fixed(1)));
    assertEquals(bust, table.roll(fixed(2)));
    assertEquals(bust, table.roll(fixed(4)));
    assertEquals(0.4, table.probabilityOf(win), 0.000001);
    assertThrows(
        IllegalArgumentException.class,
        () -> new GambleTable(List.of(GambleEntry.bust(Integer.MAX_VALUE), GambleEntry.bust(1))));
  }

  @Test
  void injectedRandomRetainsDevsWeightedDistribution() {
    GambleTable table =
        new GambleTable(List.of(GambleEntry.of(ItemIds.HEALTH_POTION, 1, 1), GambleEntry.bust(4)));
    InventoryComponent inventory = new InventoryComponent(1_000_000);
    GambleService game =
        new GambleService(table, new InventoryGambleWallet(inventory), 10, new Random(42));
    int wins = 0;
    for (int i = 0; i < 100_000; i++) if (game.spin().status() == GambleResult.Status.WON) wins++;
    assertTrue(Math.abs(wins / 100_000.0 - 0.2) < 0.01);
    assertEquals(wins, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(0, inventory.getGold());
  }
}
