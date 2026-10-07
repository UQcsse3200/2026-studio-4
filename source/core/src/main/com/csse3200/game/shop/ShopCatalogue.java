package com.csse3200.game.shop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The set of items currently purchasable in the shop. Kept as its own small class so adding a
 * second shop (e.g. a weapon-upgrade merchant later) is just a second ShopCatalogue instance.
 */
public final class ShopCatalogue {

  private final Map<String, ShopItem> itemsById = new LinkedHashMap<>();

  public ShopCatalogue(List<ShopItem> items) {
    for (ShopItem item : items) {
      if (itemsById.containsKey(item.itemId())) {
        throw new IllegalArgumentException(
            "Duplicate catalogue entry for itemId: " + item.itemId());
      }
      itemsById.put(item.itemId(), item);
    }
  }

  /** Looks up a catalogue entry by item id. Empty if the id isn't currently sold. */
  public Optional<ShopItem> find(String itemId) {
    return Optional.ofNullable(itemsById.get(itemId));
  }

  /** All items currently for sale, in catalogue order — for the shop UI to display. */
  public List<ShopItem> allItems() {
    return List.copyOf(itemsById.values());
  }
}
