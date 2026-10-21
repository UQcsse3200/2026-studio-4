package com.csse3200.game.areas.attic;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.areas.terrain.TerrainTile;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;

/**
 * Builds the floor and wall terrain of a library-themed room from its {@code obstacles.spawns}
 * grid. Row 0 of the grid is the bottom of the room, as in every other room.
 *
 * <ul>
 *   <li>{@code 'w'} wooden planks, {@code 'p'} parquet, {@code 'l'} light planks, {@code 'd'} dark
 *       attic boards
 *   <li>{@code '#'} wall, {@code 'W'} wall with a window
 *   <li>{@code ' '} void (drawn as nothing)
 * </ul>
 *
 * Walls pick their edge piece from whichever neighbour is floor, so a wall grid only has to be a
 * closed outline around the floor.
 */
public final class AtticTerrain {
  public static final char WALL = '#';
  public static final char WINDOW = 'W';
  public static final char VOID = ' ';

  /** Wall piece order on walls.png; window variants sit one row below their plain piece. */
  public enum WallPiece {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CORNER_TOP_LEFT,
    CORNER_TOP_RIGHT,
    CORNER_BOTTOM_LEFT,
    CORNER_BOTTOM_RIGHT
  }

  private static final float TILE_WORLD_SIZE = 0.5f;

  private AtticTerrain() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** Whether a grid character is a wall (and so needs a collider). */
  public static boolean isWall(char c) {
    return c == WALL || c == WINDOW;
  }

  /** Whether a grid character is walkable floor. */
  public static boolean isFloor(char c) {
    return floorBlockOrigin(c) != null;
  }

  /**
   * The tile block on floors.png that a floor character repeats, as {left px, top px, columns,
   * rows}, or null if the character is not a floor.
   */
  static int[] floorBlockOrigin(char c) {
    return switch (c) {
      case 'w' -> new int[] {16, 80, 4, 3};
      case 'p' -> new int[] {80, 80, 4, 3};
      case 'l' -> new int[] {144, 80, 4, 3};
      case 'd' -> new int[] {96, 64, 2, 1};
      default -> null;
    };
  }

  /**
   * Chooses the wall piece for the wall at (x, y): the side that faces floor gets the outline.
   *
   * @return the piece, or null if the wall touches no floor at all
   */
  public static WallPiece wallPieceAt(List<String> grid, int x, int y) {
    if (floorAt(grid, x, y - 1)) return WallPiece.TOP;
    if (floorAt(grid, x, y + 1)) return WallPiece.BOTTOM;
    if (floorAt(grid, x + 1, y)) return WallPiece.LEFT;
    if (floorAt(grid, x - 1, y)) return WallPiece.RIGHT;
    if (floorAt(grid, x + 1, y - 1)) return WallPiece.CORNER_TOP_LEFT;
    if (floorAt(grid, x - 1, y - 1)) return WallPiece.CORNER_TOP_RIGHT;
    if (floorAt(grid, x + 1, y + 1)) return WallPiece.CORNER_BOTTOM_LEFT;
    if (floorAt(grid, x - 1, y + 1)) return WallPiece.CORNER_BOTTOM_RIGHT;
    return null;
  }

  private static boolean floorAt(List<String> grid, int x, int y) {
    if (y < 0 || y >= grid.size()) {
      return false;
    }
    String row = grid.get(y);
    return x >= 0 && x < row.length() && isFloor(row.charAt(x));
  }

  /**
   * Builds the terrain component for a library room.
   *
   * @param grid the room's spawn grid (row 0 = bottom)
   * @param width map width in tiles
   * @param height map height in tiles
   * @param cameraComponent camera the terrain renders to
   */
  public static TerrainComponent build(
      List<String> grid, int width, int height, CameraComponent cameraComponent) {
    int tilePixels = AtticSprites.TILE_PIXELS;
    TileSheet floors = new TileSheet(texture(AtticSprites.FLOORS), tilePixels);
    TileSheet walls = new TileSheet(texture(AtticSprites.WALLS), tilePixels);
    TiledMapTileLayer layer = new TiledMapTileLayer(width, height, tilePixels, tilePixels);

    for (int y = 0; y < grid.size(); y++) {
      String row = grid.get(y);
      for (int x = 0; x < row.length(); x++) {
        char c = row.charAt(x);
        TerrainTile tile = null;
        int[] origin = floorBlockOrigin(c);
        if (origin != null) {
          int columns = origin[2];
          int rows = origin[3];
          int column = origin[0] / tilePixels + Math.floorMod(x, columns);
          // Sheet rows run downwards while room rows run upwards; flip so the pattern stays
          // upright.
          int sheetRow = origin[1] / tilePixels + rows - 1 - Math.floorMod(y, rows);
          tile = new TerrainTile(floors.tile(column, sheetRow));
        } else if (isWall(c)) {
          WallPiece piece = wallPieceAt(grid, x, y);
          if (piece != null) {
            boolean window = c == WINDOW && piece.ordinal() <= WallPiece.RIGHT.ordinal();
            tile = new TerrainTile(walls.tile(piece.ordinal(), window ? 1 : 0));
          }
        }
        if (tile != null) {
          Cell cell = new Cell();
          cell.setTile(tile);
          layer.setCell(x, y, cell);
        }
      }
    }

    TiledMap map = new TiledMap();
    map.getLayers().add(layer);
    OrthogonalTiledMapRenderer renderer =
        new OrthogonalTiledMapRenderer(map, TILE_WORLD_SIZE / tilePixels);
    return new TerrainComponent(
        (OrthographicCamera) cameraComponent.getCamera(),
        map,
        renderer,
        TerrainOrientation.ORTHOGONAL,
        TILE_WORLD_SIZE);
  }

  private static Texture texture(String path) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return texture;
  }
}
