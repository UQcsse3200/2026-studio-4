package com.csse3200.game.entities.factories;

import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;

/** Factory for creating item entities. */
public final class ItemFactory {
  private static final String STRENGTH_CHARM_NAME = "Strength Charm";

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createStrengthCharm() {
    Charm strengthCharm = new Charm(STRENGTH_CHARM_NAME);
    return new Entity().addComponent(new ItemComponent(strengthCharm));
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
