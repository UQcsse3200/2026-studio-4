package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.areas.terrain.DreamlandTile;
import com.csse3200.game.areas.terrain.TerrainTile;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory to create obstacle entities.
 *
 * <p>Each obstacle entity type should have a creation method that returns a corresponding entity.
 */
public class ObstacleFactory {
  private static final String DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";
  // Wider than tall so holding a diagonal along a horizontal wall also slides past tile corners.
  private static final float TILE_BEVEL_X = 0.04f;
  private static final float TILE_BEVEL_Y = 0.02f;

  /** Creates a rock obstacle. */
  public static Entity createRock() {
    Texture texture =
        ServiceLocator.getResourceService().getAsset("images/rock.png", Texture.class);
    return createRenderedObstacle(new TextureRegion(texture), 0.6f, 0.7f);
  }

  public static Entity createTile() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    Entity tile =
            createRenderedObstacle(DreamlandTile.BLUE_STONE_WALL.region(new TileSheet(texture, 16)));
    setBevelledCollider(tile, 0.5f, 0.5f);
    return tile;
  }

  // ObstacleFactory
  /** True if this wall draws the top face (spawned one row lower, at y - 1). */
  public static Entity createWallFor(
      boolean up,
      boolean right,
      boolean down,
      boolean left,
      boolean upRight,
      boolean upLeft,
      boolean downRight,
      boolean downLeft,
      boolean shift) {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);

    float colliderHeight = 0.5f;
    DreamlandTile tile;
    if (down && left || down && right) {
      tile = DreamlandTile.VOID;
    } else if (left) {
      tile = DreamlandTile.WALL_TYPE1_LEFT;
    } else if (right) {
      tile = DreamlandTile.WALL_TYPE1_RIGHT;
    } else if (up) {
      tile = DreamlandTile.WALL_TYPE1_UP;
      colliderHeight = 1f;
    } else if (down) {
      tile = DreamlandTile.WALL_TYPE1_DOWN;
    }
    // diagonal-only (inner corners)
    else if (upLeft) {
      tile = DreamlandTile.WALL_TYPE1_UP_LEFT;
      colliderHeight = 1f;
    } else if (upRight) {
      tile = DreamlandTile.WALL_TYPE1_UP_RIGHT;
      colliderHeight = 1f;
    } else if (downLeft) {
      tile = DreamlandTile.WALL_TYPE1_DOWN_LEFT;
    } else if (downRight) {
      tile = DreamlandTile.WALL_TYPE1_DOWN_RIGHT;
    } else {
      return null;
    }
    int x_shift = 0;
    if (shift) {
      x_shift = 8;
    }
    Entity wall = createRenderedObstacle(
            tile.region(new TileSheet(texture, 16), x_shift, 0), 0.5f, colliderHeight);
    setBevelledCollider(wall,0.5f, colliderHeight);
    return wall;
  }

  /** Creates a barrel obstacle. */
  public static Entity createBarrel() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
        DreamlandTile.OPEN_BARREL.region(new TileSheet(texture, 16)), 0.7f, 0.9f);
  }

  public static Entity createSword() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
            DreamlandTile.randomOfId('s', 0.1f).region(new TileSheet(texture, 16)), 0.7f, 0.9f);
  }

  public static Entity createBox() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
            DreamlandTile.randomOfId('B', 0.1f).region(new TileSheet(texture, 16)), 0.5f, 0.5f);
  }

  private static Entity createRenderedObstacle(
      TextureRegion region, float colliderWidth, float colliderHeight) {
    Entity obstacle =
            new Entity()
                    .addComponent(new TextureRenderComponent(region))
                    .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
                    .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    obstacle.getComponent(TextureRenderComponent.class).scaleEntity();
    obstacle.scaleHeight(colliderHeight);
    PhysicsUtils.setScaledCollider(obstacle, colliderWidth, colliderHeight);
    return obstacle;
  }

  private static Entity createRenderedObstacle(TextureRegion region) {
    return createRenderedObstacle(region, 0.5f, 0.5f);
  }

  /**
   * Same box as {@link PhysicsUtils#setScaledCollider} but with bevelled corners. With square
   * corners, a player moving along a row of wall tiles catches on the corner of the next tile
   * (Box2D "ghost" collisions) and gets stuck in an open corridor. This fixes snagging on flat
   * walls when moving alongside the wall but not when moving diagonally into a vertical wall.
   */
  private static void setBevelledCollider(Entity obstacle, float scaleX, float scaleY) {
    Vector2 size = obstacle.getScale().scl(scaleX, scaleY);
    float left = obstacle.getCenterPosition().x - size.x / 2;
    float right = left + size.x;
    float top = size.y;
    PolygonShape shape = new PolygonShape();
    shape.set(
        new Vector2[] {
          new Vector2(left + TILE_BEVEL_X, 0f),
          new Vector2(right - TILE_BEVEL_X, 0f),
          new Vector2(right, TILE_BEVEL_Y),
          new Vector2(right, top - TILE_BEVEL_Y),
          new Vector2(right - TILE_BEVEL_X, top),
          new Vector2(left + TILE_BEVEL_X, top),
          new Vector2(left, top - TILE_BEVEL_Y),
          new Vector2(left, TILE_BEVEL_Y)
        });
    obstacle.getComponent(ColliderComponent.class).setShape(shape);
  }

  /**
   * Creates an invisible physics wall.
   *
   * @param width Wall width in world units
   * @param height Wall height in world units
   * @return Wall entity of given width and height
   */
  public static Entity createWall(float width, float height) {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    wall.setScale(width, height);
    return wall;
  }

  private ObstacleFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
