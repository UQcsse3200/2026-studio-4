package com.csse3200.game.items;

import com.csse3200.game.entities.Entity;

/**
 * Implementations should define a behaviour that should happen to the player
 *
 * <p>Example: pickUp will add _ to player inventory
 */
public interface Pickupable {
  public void pickUp(Entity player);

  public void drop(Entity player);
}
