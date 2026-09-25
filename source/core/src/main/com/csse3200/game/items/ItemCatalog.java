package com.csse3200.game.items;

import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.items.consumables.FreezeBomb;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import com.csse3200.game.items.consumables.ShieldPotion;
import com.csse3200.game.items.consumables.SpeedPotion;
import com.csse3200.game.items.consumables.StrengthPotion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

/** Resolves a stable configuration ID to a fresh concrete item. Stores no item metadata. */
public final class ItemCatalog {
  private static final Map<String, IntFunction<? extends Item>> CREATORS =
      Map.ofEntries(
          Map.entry(ItemIds.STRENGTH_CHARM, quantity -> new StrengthCharm()),
          Map.entry(ItemIds.SPEED_CHARM, quantity -> new SpeedCharm()),
          Map.entry(ItemIds.ATTACK_SPEED_CHARM, quantity -> new AttackSpeedCharm()),
          Map.entry(ItemIds.HEALTH_POTION, InstantHealingPotion::small),
          Map.entry(ItemIds.MEDIUM_HEALTH_POTION, InstantHealingPotion::medium),
          Map.entry(ItemIds.LARGE_HEALTH_POTION, InstantHealingPotion::large),
          Map.entry(ItemIds.SHIELD, ShieldPotion::new),
          Map.entry(ItemIds.SPEED_POTION, SpeedPotion::new),
          Map.entry(ItemIds.STRENGTH_POTION, StrengthPotion::new),
          Map.entry(ItemIds.FREEZE_BOMB, FreezeBomb::new),
          Map.entry(ItemIds.GOLD_COIN, CurrencyItem::new));

  private ItemCatalog() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static boolean contains(String id) {
    return id != null && CREATORS.containsKey(id);
  }

  public static Set<String> ids() {
    return CREATORS.keySet();
  }

  public static Item create(String id, int quantity) {
    Objects.requireNonNull(id, "item id cannot be null");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    IntFunction<? extends Item> creator = CREATORS.get(id);
    if (creator == null) {
      throw new IllegalArgumentException("Unknown item ID: " + id);
    }
    Item item = creator.apply(quantity);
    if (!item.isStackable() && quantity != 1) {
      throw new IllegalArgumentException("Non-stackable item requires quantity one: " + id);
    }
    if (!id.equals(item.getId()) || item.getQuantity() != quantity) {
      throw new IllegalStateException("Item creator returned an inconsistent item for " + id);
    }
    return item;
  }

  /** Independent Charm instances, or one stack of a consumable or currency. */
  public static List<Item> createItems(String id, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    Item first = create(id, 1);
    if (first.isStackable()) {
      return List.of(quantity == 1 ? first : create(id, quantity));
    }
    List<Item> items = new ArrayList<>(quantity);
    items.add(first);
    for (int i = 1; i < quantity; i++) {
      items.add(create(id, 1));
    }
    return items;
  }
}
