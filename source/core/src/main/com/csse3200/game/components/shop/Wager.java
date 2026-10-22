package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;

/**
 * The stake amount a casino game bets with, clamped between a minimum and the player's current
 * Gold. Shared by {@link CoinFlipGame}, {@link HigherLowerGame} and {@link ShellGame}, with
 * idle-only adjustment and affordability checks for games with active rounds.
 */
final class Wager {
  private final InventoryComponent inventory;
  private final int minStake;
  private int stake;

  Wager(InventoryComponent inventory, int minStake) {
    this.inventory = Objects.requireNonNull(inventory);
    this.minStake = minStake;
    this.stake = minStake;
  }

  int stake() {
    return stake;
  }

  /** Keeps the committed stake unchanged until the active round has settled. */
  void adjustWhileIdle(int delta, boolean roundActive) {
    if (!roundActive) {
      adjust(delta);
    }
  }

  boolean canPlace(boolean roundActive) {
    int gold = inventory.getGold();
    return !roundActive && stake > 0 && gold >= stake;
  }

  void adjust(int delta) {
    int gold = inventory.getGold();
    stake = Math.clamp((long) stake + delta, Math.min(minStake, gold), gold);
  }
}
