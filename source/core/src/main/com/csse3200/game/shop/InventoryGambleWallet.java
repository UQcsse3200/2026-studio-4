package com.csse3200.game.shop;

import com.csse3200.game.components.player.InventoryComponent;

public class InventoryGambleWallet implements GambleWallet {
  private final InventoryComponent inventory;

  public InventoryGambleWallet(InventoryComponent inventory) {
    this.inventory = inventory;
  }

  @Override
  public boolean hasGold(int amount) {
    return inventory.hasGold(amount);
  }

  @Override
  public void spendGold(int amount) {
    inventory.addGold(-amount);
  }

  @Override
  public void grantItem(String itemId, int quantity) {
    inventory.addConsumable(itemId, quantity);
  }
}
