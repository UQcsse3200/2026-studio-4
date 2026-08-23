package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.math.GridPoint2;

public class TerrainConfig {
  public String groundTexture;

  public String gAltTexture1;
  public String gAltTexture2;
  public int gAltTexture1Count;
  public int gAltTexture2Count;

  public float tileSize;
  public GridPoint2 MAP_SIZE = new GridPoint2(50, 50);

  public void setTextures(String groundTexture, String gAltTexture1, String gAltTexture2) {
    this.groundTexture = groundTexture;
    this.gAltTexture1 = gAltTexture1;
    this.gAltTexture2 = gAltTexture2;
  }
}
