package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.charms.Charm;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final Item item;
  private boolean collected;

  /** Creates an item component for a single item. */
  public ItemComponent(Item item) {
    this.item = Objects.requireNonNull(item, "item cannot be null");
    requirePositiveQuantity(item.getQuantity());
  }

  /** Creates an item component for a single registered item ID. */
  public ItemComponent(String itemId) {
    this(ItemDropSpec.single(itemId));
  }

  /** Creates an item component from a caller-selected drop specification. */
  public ItemComponent(ItemDropSpec dropSpec) {
    this(
        ItemCatalog.create(
            Objects.requireNonNull(dropSpec, "dropSpec cannot be null").itemId(),
            dropSpec.quantity()));
  }

  /** Returns the item represented by the entity. */
  public Item getItem() {
    return item;
  }

  /**
   * Applies the item's normal pickup effect at most once across all collection methods.
   *
   * @param collector entity receiving the item
   * @return true if this call collected the item; false if it was collected or disposed earlier
   */
  public boolean tryPickUp(Entity collector) {
    if (collected) {
      return false;
    }
    collected = true;
    item.pickUp(collector);
    return true;
  }

  /** Returns whether this world item has already been collected or disposed. */
  public boolean isCollected() {
    return collected;
  }

  @Override
  public void dispose() {
    collected = true;
  }

  /** Returns the stable item ID, or {@code null} for unrelated item implementations. */
  public String getItemId() {
    return item.getId();
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
