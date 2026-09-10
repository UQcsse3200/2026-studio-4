package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final ItemType itemType;
  private final Charm charm;
  private final int quantity;

  /**
   * Creates an item component for a charm.
   *
   * @param charm charm represented by the entity
   */
  public ItemComponent(Charm charm) {
    this(charm, 1);
  }

  /** Creates a charm item component with a caller-selected quantity. */
  public ItemComponent(Charm charm, int quantity) {
    this.charm = Objects.requireNonNull(charm, "charm cannot be null");
    this.itemType = ItemType.STRENGTH_CHARM;
    this.quantity = requirePositiveQuantity(quantity);
  }

  /**
   * Creates an item component for a non-charm item type.
   *
   * @param itemType consumable or currency represented by the entity
   * @throws IllegalArgumentException when a charm type is supplied without its charm data
   */
  public ItemComponent(ItemType itemType) {
    this(ItemDropSpec.single(itemType));
  }

  /** Creates a non-charm item component from a caller-selected drop specification. */
  public ItemComponent(ItemDropSpec dropSpec) {
    Objects.requireNonNull(dropSpec, "dropSpec cannot be null");
    this.itemType = dropSpec.itemType();
    if (itemType.getCategory() == ItemCategory.CHARM) {
      throw new IllegalArgumentException("Charm items require charm data");
    }
    this.charm = null;
    this.quantity = dropSpec.quantity();
  }

  /** Returns the stable type used by item, inventory, and UI integrations. */
  public ItemType getItemType() {
    return itemType;
  }

  /**
   * Returns the charm represented by the entity.
   *
   * @return charm data, or {@code null} when this is not a charm item
   */
  public Charm getCharm() {
    return charm;
  }

  /** Returns the positive quantity represented by this world entity. */
  public int getQuantity() {
    return quantity;
  }

  private static int requirePositiveQuantity(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    return quantity;
  }
}
