package com.csse3200.game.shop;

/** Outcome of a single ShopService.purchase() call, for the UI/HUD to react to. */
public record PurchaseResult(Status status, ShopItem item) {

  public enum Status {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    INVALID_ITEM,
    INVALID_QUANTITY
  }

  public static PurchaseResult success(ShopItem item) {
    return new PurchaseResult(Status.SUCCESS, item);
  }

  public static PurchaseResult insufficientFunds() {
    return new PurchaseResult(Status.INSUFFICIENT_FUNDS, null);
  }

  public static PurchaseResult invalidItem() {
    return new PurchaseResult(Status.INVALID_ITEM, null);
  }

  public static PurchaseResult invalidQuantity() {
    return new PurchaseResult(Status.INVALID_QUANTITY, null);
  }

  public boolean succeeded() {
    return status == Status.SUCCESS;
  }
}
