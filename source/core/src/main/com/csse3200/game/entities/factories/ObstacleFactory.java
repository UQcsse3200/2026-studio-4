package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.areas.terrain.DreamlandTile;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create obstacle entities.
 *
 * <p>Each obstacle entity type should have a creation method that returns a corresponding entity.
 */
public class ObstacleFactory {
  private static final String DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";

  /**
   * Creates a tree entity.
   *
   * @return entity
   */
  public static Entity createTree() {
    Entity tree =
        new Entity()
            .addComponent(new TextureRenderComponent("images/tree.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    tree.getComponent(PhysicsComponent.class).setBodyType(BodyType.StaticBody);
    tree.getComponent(TextureRenderComponent.class).scaleEntity();
    tree.scaleHeight(2.5f);
    PhysicsUtils.setScaledCollider(tree, 0.5f, 0.2f);
    return tree;
  }

  /** Creates a rock obstacle. */
  public static Entity createRock() {
    Texture texture =
        ServiceLocator.getResourceService().getAsset("images/rock.png", Texture.class);
    return createRenderedObstacle(new TextureRegion(texture), 0.6f, 0.7f);
  }

  public static Entity createTile() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
        DreamlandTile.BLUE_STONE_WALL.region(new TileSheet(texture, 16)), 0.6f, 0.6f);
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
      boolean downLeft) {
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

    return createRenderedObstacle(
        tile.region(new TileSheet(texture, 16), 8, 0), 0.5f, colliderHeight);
  }

  /** Creates a barrel obstacle. */
  public static Entity createBarrel() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
        DreamlandTile.CHAIN.region(new TileSheet(texture, 16)), 0.7f, 0.9f);
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
