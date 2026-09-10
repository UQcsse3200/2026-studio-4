package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.items.ItemType;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final ItemType itemType;
  private final Charm charm;

  /**
   * Creates an item component for a charm.
   *
   * @param charm charm represented by the entity
   */
  public ItemComponent(Charm charm) {
    this.charm = Objects.requireNonNull(charm, "charm cannot be null");
    this.itemType = ItemType.STRENGTH_CHARM;
  }

  /**
   * Creates an item component for a non-charm item type.
   *
   * @param itemType consumable or currency represented by the entity
   * @throws IllegalArgumentException when a charm type is supplied without its charm data
   */
  public ItemComponent(ItemType itemType) {
    this.itemType = Objects.requireNonNull(itemType, "itemType cannot be null");
    if (itemType.getCategory() == ItemCategory.CHARM) {
      throw new IllegalArgumentException("Charm items require charm data");
    }
    this.charm = null;
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
}
