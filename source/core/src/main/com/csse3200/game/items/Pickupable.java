package com.csse3200.game.items;

import com.csse3200.game.entities.Entity;

public interface Pickupable {
  public void pickUp(Entity player);
  public void drop(Entity player);
}
