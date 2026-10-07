package com.csse3200.game.components.shop;

import com.csse3200.game.components.Component;
import java.util.Objects;

/** Settles Aarash's display requests using the trusted catalogue and the active shop session. */
public class ShopPurchaseComponent extends Component {
  private final ShopSessionComponent session;
  private final ShopService service;
  private boolean disposed;

  public ShopPurchaseComponent(ShopSessionComponent session, ShopService service) {
    this.session = Objects.requireNonNull(session);
    this.service = Objects.requireNonNull(service);
  }

  @Override
  public void create() {
    entity.getEvents().addListener("shopPurchaseRequested", this::purchase);
  }

  private void purchase(String offerId, Integer displayedPrice) {
    if (disposed || !session.isOpen()) return;
    // The display's price is informative; only the validated catalogue determines the charge.
    ShopPurchaseResult result =
        displayedPrice == null || displayedPrice <= 0
            ? ShopPurchaseResult.INVALID_OFFER
            : service.purchase(offerId);
    entity.getEvents().trigger("shopPurchaseResult", offerId, result);
  }

  @Override
  public void dispose() {
    disposed = true;
    super.dispose();
  }
}
