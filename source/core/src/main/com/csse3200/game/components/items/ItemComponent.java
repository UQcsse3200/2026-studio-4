package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.TypedItem;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.StrengthCharm;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final Item item;
  private final ItemType itemType;
  private final int quantity;

  /** Creates an item component for a single item. */
  public ItemComponent(Item item) {
    this(item, item instanceof TypedItem typedItem ? typedItem.getQuantity() : 1);
  }

  /** Creates an item component for a caller-selected quantity of an item. */
  public ItemComponent(Item item, int quantity) {
    this.item = Objects.requireNonNull(item, "item cannot be null");
    this.itemType = resolveItemType(item);
    this.quantity = requirePositiveQuantity(quantity);
  }

  /** Creates an item component for a single non-charm Team 5 item type. */
  public ItemComponent(ItemType itemType) {
    this(ItemDropSpec.single(itemType));
  }

  /** Creates an item component from a caller-selected Team 5 drop specification. */
  public ItemComponent(ItemDropSpec dropSpec) {
    Objects.requireNonNull(dropSpec, "dropSpec cannot be null");
    if (dropSpec.itemType() == ItemType.STRENGTH_CHARM) {
      throw new IllegalArgumentException("Charm items require charm data");
    }
    this.item = new TypedItem(dropSpec);
    this.itemType = dropSpec.itemType();
    this.quantity = dropSpec.quantity();
  }

  /** Returns the item represented by the entity. */
  public Item getItem() {
    return item;
  }

  /** Returns the stable Team 5 item type, or {@code null} for unrelated item implementations. */
  public ItemType getItemType() {
    return itemType;
  }

  /** Returns the charm represented by the entity, or {@code null} for non-charm items. */
  public Charm getCharm() {
    return item instanceof Charm charm ? charm : null;
  }

  /** Returns the positive quantity represented by this world entity. */
  public int getQuantity() {
    return quantity;
  }

  private static ItemType resolveItemType(Item item) {
    if (item instanceof TypedItem typedItem) {
      return typedItem.getItemType();
    }
    if (item instanceof StrengthCharm) {
      return ItemType.STRENGTH_CHARM;
    }
    return null;
  }

  private static int requirePositiveQuantity(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    return quantity;
  }
}
