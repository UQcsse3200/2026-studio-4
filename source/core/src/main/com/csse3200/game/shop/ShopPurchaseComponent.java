package com.csse3200.game.shop;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;

/**
 * Bridges the Shop display screen's purchase-request event (fired on the UI entity in
 * MainGameScreen) to the standalone ShopService transaction, and fires the result back so the UI
 * can react.
 *
 * <p>Event confirmed with Aarash: "shopPurchaseRequested", payload (String itemId, int price).
 */
public class ShopPurchaseComponent extends Component {
  public static final String PURCHASE_REQUEST_EVENT = "shopPurchaseRequested";
  public static final String PURCHASE_RESULT_EVENT =
      "shopPurchaseResult"; // TODO: confirm with Aarash

  private final ShopService shopService;

  public ShopPurchaseComponent(ShopCatalogue catalogue, InventoryComponent inventory) {
    this.shopService = new ShopService(catalogue, new InventoryShopWallet(inventory));
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PURCHASE_REQUEST_EVENT, this::onPurchaseRequest);
  }

  private void onPurchaseRequest(String itemId, int price) {
    // price is the UI's displayed price; ShopService looks the real price up from the
    // catalogue itself, so it isn't used for the transaction.
    PurchaseResult result = shopService.purchase(itemId);
    entity.getEvents().trigger(PURCHASE_RESULT_EVENT, result);
  }
}
