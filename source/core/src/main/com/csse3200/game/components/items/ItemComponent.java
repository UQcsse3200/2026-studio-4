package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.charms.Charm;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final Item item;

  /** Creates an item component for a single item. */
  public ItemComponent(Item item) {
    this.item = Objects.requireNonNull(item, "item cannot be null");
    requirePositiveQuantity(item.getQuantity());
  }

  /** Creates an item component for a single registered item type. */
  public ItemComponent(ItemType itemType) {
    this(ItemDropSpec.single(itemType));
  }

  /** Creates an item component from a caller-selected drop specification. */
  public ItemComponent(ItemDropSpec dropSpec) {
    this(
        Objects.requireNonNull(dropSpec, "dropSpec cannot be null")
            .itemType()
            .createItem(dropSpec.quantity()));
  }

  /** Returns the item represented by the entity. */
  public Item getItem() {
    return item;
  }

  /** Returns the stable item ID, or {@code null} for unrelated item implementations. */
  public ItemType getItemType() {
    return item.getItemType();
  }

  /** Returns the charm represented by the entity, or {@code null} for non-charm items. */
  public Charm getCharm() {
    return item instanceof Charm charm ? charm : null;
  }

  /** Returns the positive quantity represented by this world entity. */
  public int getQuantity() {
    return item.getQuantity();
  }

  private static int requirePositiveQuantity(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    return quantity;
  }
}
