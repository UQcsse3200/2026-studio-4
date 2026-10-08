package com.csse3200.game.components.shop;

import com.csse3200.game.files.FileLoader;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads and validates shop offers independently of their rendering and purchase effects. */
public class ShopCatalog {
  private final String id;
  private final List<ShopOffer> offers;
  private final Map<String, ShopOffer> byId;

  public ShopCatalog(String id, List<ShopOffer> offers) {
    if (isBlank(id) || offers == null || offers.isEmpty()) {
      throw new IllegalArgumentException("Shop requires an ID and at least one offer");
    }
    Map<String, ShopOffer> indexed = new LinkedHashMap<>();
    for (ShopOffer offer : offers) {
      if (offer == null
          || isBlank(offer.offerId())
          || isBlank(offer.productId())
          || offer.kind() == null
          || offer.goldPrice() <= 0) {
        throw new IllegalArgumentException(
            "Shop offers require IDs, a category and positive price");
      }
      if (offer.kind() == ShopProductKind.CONSUMABLE
          && (!ItemCatalog.contains(offer.productId())
              || !(ItemCatalog.create(offer.productId(), 1) instanceof ConsumableItem))) {
        throw new IllegalArgumentException("Unknown consumable: " + offer.productId());
      }
      if (indexed.putIfAbsent(offer.offerId(), offer) != null) {
        throw new IllegalArgumentException("Duplicate shop offer: " + offer.offerId());
      }
    }
    this.id = id;
    this.offers = List.copyOf(offers);
    byId = Collections.unmodifiableMap(indexed);
  }

  public String id() {
    return id;
  }

  public List<ShopOffer> offers() {
    return offers;
  }

  public ShopOffer find(String offerId) {
    return byId.get(offerId);
  }

  public static ShopCatalog load(String assetPath) {
    CatalogConfig config = FileLoader.readClass(CatalogConfig.class, assetPath);
    if (config == null || config.offers == null) {
      throw new IllegalArgumentException("Invalid shop configuration: " + assetPath);
    }
    List<ShopOffer> loaded = new ArrayList<>();
    for (OfferConfig offer : config.offers) {
      if (offer == null) {
        throw new IllegalArgumentException("Null offer in shop configuration: " + assetPath);
      }
      loaded.add(new ShopOffer(offer.offerId, offer.kind, offer.productId, offer.goldPrice));
    }
    return new ShopCatalog(config.id, loaded);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /** JSON transport only; validated immutable offers are used at runtime. */
  public static class CatalogConfig {
    public String id;
    public OfferConfig[] offers;
  }

  /** JSON transport for one offer. */
  public static class OfferConfig {
    public String offerId;
    public ShopProductKind kind;
    public String productId;
    public int goldPrice;
  }
}
