package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import org.junit.jupiter.api.Test;

class DiceDuelGameTest {
  @Test
  void stakeStartsAtMinimumAndClampsToBalance() {
    InventoryComponent inventory = new InventoryComponent(25);
    DiceDuelGame game = new DiceDuelGame(inventory, () -> 1);
    assertEquals(10, game.getStake());
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.adjustStake(Integer.MIN_VALUE);
    assertEquals(10, game.getStake());
  }

  @Test
  void lowBalanceCanStakeRemainingCoins() {
    InventoryComponent inventory = new InventoryComponent(5);
    DiceDuelGame game = new DiceDuelGame(inventory, () -> 1);
    assertEquals(5, game.getStake());
    assertTrue(game.canRoll());
  }

  @Test
  void cannotRollWithNoGoldOrOverflowRisk() {
    InventoryComponent inventory = new InventoryComponent(0);
    DiceDuelGame game = new DiceDuelGame(inventory, () -> 1);
    assertFalse(game.canRoll());
    inventory.setGold(Integer.MAX_VALUE);
    game.refresh();
    assertFalse(game.canRoll());
  }

  /**
   * Returns the given rolls in order: player die 1, player die 2, merchant die 1, merchant die 2.
   */
  private static java.util.function.IntSupplier rolls(int... values) {
    int[] next = {0};
    return () -> values[next[0]++];
  }

  @Test
  void winReturnsDoubleStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(6, 5, 1, 2));
    game.roll();
    assertEquals(40, inventory.getGold()); // stake taken when rolling
    DiceDuelGame.Outcome outcome = game.settle();
    assertEquals(DiceDuelGame.Result.WIN, outcome.result());
    assertEquals(11, outcome.playerTotal());
    assertEquals(3, outcome.merchantTotal());
    assertEquals(60, inventory.getGold()); // +10 overall
  }

  @Test
  void tieRefundsStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(3, 4, 5, 2));
    game.roll();
    assertEquals(DiceDuelGame.Result.TIE, game.settle().result());
    assertEquals(50, inventory.getGold());
  }

  @Test
  void lossKeepsStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(1, 1, 6, 6));
    game.roll();
    assertEquals(DiceDuelGame.Result.LOSE, game.settle().result());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void cannotRollTwiceOrSettleWithoutARound() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(2, 2, 2, 2));
    assertNull(game.settle());
    game.roll();
    assertTrue(game.isRoundActive());
    assertNull(game.roll());
    assertEquals(40, inventory.getGold()); // second roll did not take another stake
  }

  @Test
  void stakeIsLockedDuringARound() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(2, 2, 2, 2));
    game.roll();
    game.adjustStake(20);
    assertEquals(10, game.getStake());
  }

  @Test
  void invalidDieValueIsRejected() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, () -> 7);
    assertThrows(IllegalStateException.class, game::roll);
  }

  @Test
  void rerollChargesFeeAndOnlyChangesPlayerDice() {
    InventoryComponent inventory = new InventoryComponent(50);
    // player 1+1, merchant 4+4, then reroll player to 6+5
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(1, 1, 4, 4, 6, 5));
    game.roll();
    assertTrue(game.canReroll());
    DiceDuelGame.Round round = game.reroll();
    assertEquals(11, round.playerTotal());
    assertEquals(8, round.merchantTotal());
    assertEquals(35, inventory.getGold()); // 50 - 10 stake - 5 fee
    DiceDuelGame.Outcome outcome = game.settle();
    assertEquals(DiceDuelGame.Result.WIN, outcome.result());
    assertTrue(outcome.rerolled());
    assertEquals(55, inventory.getGold()); // fee is not refunded
  }

  @Test
  void rerollOnlyOncePerRound() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(1, 1, 4, 4, 2, 2));
    game.roll();
    game.reroll();
    assertFalse(game.canReroll());
    assertNull(game.reroll());
    assertEquals(35, inventory.getGold()); // second reroll charged nothing
  }

  @Test
  void cannotRerollWithoutRoundOrFee() {
    InventoryComponent inventory = new InventoryComponent(12);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(1, 1, 4, 4));
    assertNull(game.reroll()); // no round yet
    game.roll(); // stake 10 taken, 2 Gold left
    assertFalse(game.canReroll());
    assertNull(game.reroll());
    assertEquals(2, inventory.getGold());
  }

  @Test
  void winningWithADoubleAddsBonus() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame game = new DiceDuelGame(inventory, rolls(4, 4, 1, 2));
    game.roll();
    DiceDuelGame.Outcome outcome = game.settle();
    assertEquals(DiceDuelGame.Result.WIN, outcome.result());
    assertEquals(5, outcome.bonus());
    assertEquals(65, inventory.getGold()); // 50 - 10 + 20 + 5 bonus
  }

  @Test
  void doublesGiveNoBonusOnLossOrTie() {
    InventoryComponent inventory = new InventoryComponent(50);
    DiceDuelGame loss = new DiceDuelGame(inventory, rolls(2, 2, 6, 6));
    loss.roll();
    assertEquals(0, loss.settle().bonus());
    assertEquals(40, inventory.getGold());
    DiceDuelGame tie = new DiceDuelGame(inventory, rolls(3, 3, 4, 2));
    tie.roll();
    assertEquals(0, tie.settle().bonus());
    assertEquals(40, inventory.getGold());
  }
}
