package com.csse3200.game.shop;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;

/**
 * Bridges UI purchase-request events (fired by the Shop display screen) to the standalone
 * ShopService transaction, and fires the result back so the UI can show success/"not enough Gold"
 * feedback.
 *
 * <p>TODO: confirm real event name + payload with Aarash once he replies, then update
 * PURCHASE_REQUEST_EVENT below (currently a placeholder guess).
 */
public class ShopPurchaseComponent extends Component {
  public static final String PURCHASE_REQUEST_EVENT = "purchaseRequest"; // placeholder
  public static final String PURCHASE_RESULT_EVENT = "purchaseResult"; // placeholder

  private final ShopService shopService;

  public ShopPurchaseComponent(ShopCatalogue catalogue, InventoryComponent inventory) {
    this.shopService = new ShopService(catalogue, new InventoryShopWallet(inventory));
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PURCHASE_REQUEST_EVENT, this::onPurchaseRequest);
  }

  private void onPurchaseRequest(String itemId) {
    PurchaseResult result = shopService.purchase(itemId);
    entity.getEvents().trigger(PURCHASE_RESULT_EVENT, result);
  }
}
