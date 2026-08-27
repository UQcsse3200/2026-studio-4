package com.csse3200.game.entities.factories;

import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Factory for creating item entities. */
public final class ItemFactory {
  private static final String STRENGTH_CHARM_NAME = "Strength Charm";

  /**
   * Selects and creates the item dropped after an enemy is defeated.
   *
   * <p>Sprint 1 uses a deterministic drop: every request returns a Strength Charm. The returned
   * entity is not positioned or registered; the requesting room owns those responsibilities.
   *
   * @return a non-null, unregistered item entity for the room to spawn
   */
  public static Entity createDrop() {
    return createStrengthCharm();
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
