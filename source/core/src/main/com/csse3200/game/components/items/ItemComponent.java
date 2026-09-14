package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Item;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final Item item;

  /**
   * Creates an item component for a charm.
   *
   * @param item represented by the entity
   */
  public ItemComponent(Item item) {
    this.item = Objects.requireNonNull(item, "charm cannot be null");
  }

  /**
   * Returns the charm represented by the entity.
   *
   * @return charm data
   */
  public Item getItem() {
    return item;
  }
}
