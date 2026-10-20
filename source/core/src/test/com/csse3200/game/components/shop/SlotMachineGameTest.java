package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotMachineGameTest {
  @Test
  void threeMatchingReelsPayOutTheSymbolsMultiplierOnTheBet() {
    InventoryComponent inventory = new InventoryComponent(50);
    SlotMachineGame game = new SlotMachineGame(inventory, () -> 0); // always CHERRY
    SlotMachineGame.Outcome outcome = game.spin();
    assertEquals(
        new SlotMachineGame.Outcome(
            List.of(
                SlotMachineGame.Symbol.CHERRY,
                SlotMachineGame.Symbol.CHERRY,
                SlotMachineGame.Symbol.CHERRY),
            2,
            10),
        outcome);
    assertTrue(outcome.won());
    assertEquals(60, inventory.getGold());
  }

  @Test
  void mismatchedReelsLoseExactlyTheBet() {
    InventoryComponent inventory = new InventoryComponent(50);
    int[] rolls = {0, 1, 2};
    int[] index = {0};
    SlotMachineGame game = new SlotMachineGame(inventory, () -> rolls[index[0]++ % rolls.length]);
    SlotMachineGame.Outcome outcome = game.spin();
    assertFalse(outcome.won());
    assertEquals(-10, outcome.goldDelta());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void betClampsToBalanceAndAllowsLastFewCoins() {
    InventoryComponent inventory = new InventoryComponent(25);
    SlotMachineGame game = new SlotMachineGame(inventory, () -> 0);
    game.adjustBet(Integer.MAX_VALUE);
    assertEquals(25, game.getBet());
    game.adjustBet(Integer.MIN_VALUE);
    assertEquals(10, game.getBet());
    inventory.setGold(5);
    game.refresh();
    assertEquals(5, game.getBet());
  }

  @Test
  void zeroOrOverflowBalanceRejectsWithoutSpinningOrChangingGold() {
    InventoryComponent inventory = new InventoryComponent(50);
    int[] spins = {0};
    SlotMachineGame game =
        new SlotMachineGame(
            inventory,
            () -> {
              spins[0]++;
              return 0;
            });
    inventory.setGold(0);
    assertNull(game.spin());
    inventory.setGold(Integer.MAX_VALUE);
    game.refresh();
    assertFalse(game.canSpin());
    assertNull(game.spin());
    assertEquals(Integer.MAX_VALUE, inventory.getGold());
    assertEquals(0, spins[0]);
  }

  @Test
  void higherRaritySymbolsPayOutMoreThanLowerRarityOnes() {
    InventoryComponent inventory = new InventoryComponent(1000);
    SlotMachineGame diamonds = new SlotMachineGame(inventory, () -> 3); // DIAMOND index
    SlotMachineGame.Outcome outcome = diamonds.spin();
    assertEquals(20, outcome.payoutMultiplier());
    assertEquals(SlotMachineGame.Symbol.DIAMOND, outcome.reels().get(0));
  }
}
