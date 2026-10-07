package com.csse3200.game.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;

/** Settles gambling against the same real inventory used by shop purchases and Coin Flip. */
public final class InventoryGambleWallet implements GambleWallet {
  private final InventoryComponent inventory;

  public InventoryGambleWallet(InventoryComponent inventory) {
    this.inventory = Objects.requireNonNull(inventory);
  }

  @Override
  public boolean hasGold(int amount) {
    return amount > 0 && inventory.hasGold(amount);
  }

  @Override
  public GambleResult.Status settle(GambleEntry entry, int cost) {
    Objects.requireNonNull(entry);
    if (cost <= 0) throw new IllegalArgumentException("Draw cost must be positive");
    if (entry.isBust()) {
      if (!hasGold(cost)) return GambleResult.Status.INSUFFICIENT_FUNDS;
      inventory.addGold(-cost);
      return GambleResult.Status.BUST;
    }
    return switch (inventory.tryPurchaseConsumable(entry.itemId(), cost, entry.quantity())) {
      case SUCCESS -> GambleResult.Status.WON;
      case INSUFFICIENT_GOLD -> GambleResult.Status.INSUFFICIENT_FUNDS;
      case QUANTITY_LIMIT -> GambleResult.Status.QUANTITY_LIMIT;
      case INVALID_ITEM, INVALID_PRICE -> GambleResult.Status.INVALID_REWARD;
    };
  }
}
