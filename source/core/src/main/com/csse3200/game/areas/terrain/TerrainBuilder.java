package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Factory for creating game terrains. */
public class TerrainBuilder {
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";
  private static final int DUNGEON_TILE_SIZE = 16;
  private final OrthographicCamera camera;

  private final TiledMapTileLayer layer;

  private final TileSheet tileSheet;

  private TerrainTile defaultTile;

  /**
   * Create a terrain builder
   *
   * @param cameraComponent Camera to render terrains to. Must be orthographic.
   * @param mapSize The size of the map to render terrain to.
   */
  public TerrainBuilder(
      CameraComponent cameraComponent, GridPoint2 mapSize) {
    this.camera = (OrthographicCamera) cameraComponent.getCamera();
    this.layer = new TiledMapTileLayer(mapSize.x, mapSize.y, DUNGEON_TILE_SIZE, DUNGEON_TILE_SIZE);
    ResourceService resourceService = ServiceLocator.getResourceService();
    Texture texture = resourceService.getAsset(FANTASY_DUNGEON_TILESET, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    this.tileSheet = new TileSheet(texture, DUNGEON_TILE_SIZE);
    this.defaultTile = new TerrainTile(DreamlandTile.VOID.region(this.tileSheet));
  }

  /**
   * Create a cell and tile it with the given texture from C
   * @param x The x coordinate of the cell to spawn
   * @param y The y coordinate of the cell to spawn
   * @param c The character matching the texture of the tile wanted.
   */
  public void tile(int x, int y, Character c) {
    Cell cell = new Cell();
    randomOfId(c);
    cell.setTile(this.defaultTile);
    this.layer.setCell(x, y, cell);
  }

  /**
   * Sets the texture to give the tile to a random terrain texture matching the given character, as long as the character is a space or a digit (a terrain texture)
   * @param c The character matching the textures id in DreamlandTile
   */
  private void randomOfId(Character c) {
    if (Character.isDigit(c) || Character.isSpaceChar(c)) {
      setTile(new TerrainTile(DreamlandTile.randomOfId(c, 0.9f).region(this.tileSheet)));
    }
  }

  /**
   * Sets the texture to give the tile.
   * @param tile The texture to give the tile.
   */
  public void setTile(TerrainTile tile) {
    this.defaultTile = tile;
  }

  /**
   * Gets the terrain component created by this builder.
   * @return The terrain component.
   */
  public TerrainComponent getTerrain() {
    TiledMap tiledMap = new TiledMap();
    tiledMap.getLayers().add(layer);
    TiledMapRenderer renderer = new OrthogonalTiledMapRenderer(tiledMap, 0.5f / DUNGEON_TILE_SIZE);
    return new TerrainComponent(camera, tiledMap, renderer, TerrainOrientation.ORTHOGONAL, 0.5f);
  }
}
