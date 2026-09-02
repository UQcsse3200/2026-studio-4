package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.Charm;
import java.util.Objects;

/** Stores the item data represented by an item entity. */
public class ItemComponent extends Component {
  private final Charm charm;

  /**
   * Creates an item component for a charm.
   *
   * @param charm charm represented by the entity
   */
  public ItemComponent(Charm charm) {
    this.charm = Objects.requireNonNull(charm, "charm cannot be null");
  }

  /**
   * Returns the charm represented by the entity.
   *
   * @return charm data
   */
  public Charm getCharm() {
    return charm;
  }
}
