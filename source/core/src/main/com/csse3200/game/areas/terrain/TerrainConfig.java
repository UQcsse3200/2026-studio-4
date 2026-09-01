package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.math.GridPoint2;

/** Configuration for a simple orthogonal terrain layer. */
public class TerrainConfig {
  public String groundTexture;
  public String alternateTextureOne;
  public String alternateTextureTwo;
  public int alternateTextureOneCount;
  public int alternateTextureTwoCount;
  public GridPoint2 mapSize = new GridPoint2(50, 50);

  /** Sets the floor textures used by this terrain. */
  public void setTextures(String ground, String alternateOne, String alternateTwo) {
    groundTexture = ground;
    alternateTextureOne = alternateOne;
    alternateTextureTwo = alternateTwo;
  }
}
