package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import org.junit.jupiter.api.Test;

class CoinFlipGameTest {
  @Test
  void winAndLossUseTheSameInventoryAndExactStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    CoinFlipGame win = new CoinFlipGame(inventory, () -> true);
    assertEquals(new CoinFlipGame.Outcome(true, 10), win.flip());
    assertEquals(60, inventory.getGold());
    CoinFlipGame loss = new CoinFlipGame(inventory, () -> false);
    loss.adjustStake(10);
    assertEquals(new CoinFlipGame.Outcome(false, 20), loss.flip());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void stakeClampsToBalanceAndAllowsLastFewCoins() {
    InventoryComponent inventory = new InventoryComponent(25);
    CoinFlipGame game = new CoinFlipGame(inventory, () -> false);
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.adjustStake(Integer.MIN_VALUE);
    assertEquals(10, game.getStake());
    inventory.setGold(5);
    game.refresh();
    assertEquals(5, game.getStake());
    game.flip();
    assertEquals(0, inventory.getGold());
    assertFalse(game.canFlip());
    assertNull(game.flip());
  }

  @Test
  void changedBalanceAndPayoutOverflowRejectWithoutTossingOrChangingGold() {
    InventoryComponent inventory = new InventoryComponent(50);
    int[] tosses = {0};
    CoinFlipGame game =
        new CoinFlipGame(
            inventory,
            () -> {
              tosses[0]++;
              return true;
            });
    inventory.setGold(0);
    assertNull(game.flip());
    inventory.setGold(Integer.MAX_VALUE);
    game.refresh();
    assertFalse(game.canFlip());
    assertNull(game.flip());
    assertEquals(Integer.MAX_VALUE, inventory.getGold());
    assertEquals(0, tosses[0]);
  }
}
