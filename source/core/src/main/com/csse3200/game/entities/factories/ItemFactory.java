package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import java.util.Objects;

/** Factory for creating item entities. */
public final class ItemFactory {
  private static final String STRENGTH_CHARM_NAME = "Strength Charm";

  /**
   * Creates the requested item at a world position.
   *
   * <p>Sprint 1 currently supports only {@link ItemType#STRENGTH_CHARM}. The returned entity is not
   * registered; the requesting room owns that responsibility.
   *
   * @param itemType type of item to create
   * @param position world position assigned to the item entity
   * @return a non-null, positioned, unregistered item entity for the room to spawn
   */
  public static Entity createDrop(ItemType itemType, Vector2 position) {
    Objects.requireNonNull(itemType, "itemType cannot be null");
    Objects.requireNonNull(position, "position cannot be null");

    Entity item =
        switch (itemType) {
          case STRENGTH_CHARM -> createStrengthCharm();
        };
    item.setPosition(position);
    return item;
  }

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
    return new Entity()
        .addComponent(new PhysicsComponent())
        .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
        .addComponent(new ItemComponent(strengthCharm));
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
