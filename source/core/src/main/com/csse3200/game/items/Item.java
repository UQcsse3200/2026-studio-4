package com.csse3200.game.items;

public abstract class Item implements Pickupable {
  private final String name;
  private final String description;
  private final String texture; // used when for inventory ui
  
  protected Item(String name, String description, String texture) {
    this.name = name;
    this.description = description;
    this.texture = texture;
  }

  public String getName() {
	return name;
  }

  public String getDescription() {
	return description;
  }

  public String getTexture() {
	return texture;
  }
}
