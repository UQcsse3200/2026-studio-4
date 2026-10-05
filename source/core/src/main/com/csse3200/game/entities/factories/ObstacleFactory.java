package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
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
  // Wider than tall so holding a diagonal along a horizontal wall also slides past tile corners.
  private static final float TILE_BEVEL_X = 0.04f;
  private static final float TILE_BEVEL_Y = 0.02f;

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
    Entity tile =
        createRenderedObstacle(DreamlandTile.BLUE_STONE_WALL.region(new TileSheet(texture, 16)));
    setBevelledCollider(tile, 0.5f, 0.5f);
    return tile;
  }

  /** Creates a barrel obstacle. */
  public static Entity createBarrel() {
    Texture texture = ServiceLocator.getResourceService().getAsset(DUNGEON_TILESET, Texture.class);
    return createRenderedObstacle(
        DreamlandTile.OPEN_BARREL.region(new TileSheet(texture, 16)), 0.7f, 0.9f);
  }

  private static Entity createRenderedObstacle(
      TextureRegion region, float colliderWidth, float colliderHeight) {
    Entity obstacle = createRenderedObstacle(region);
    PhysicsUtils.setScaledCollider(obstacle, colliderWidth, colliderHeight);
    return obstacle;
  }

  private static Entity createRenderedObstacle(TextureRegion region) {
    Entity obstacle =
        new Entity()
            .addComponent(new TextureRenderComponent(region))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    obstacle.getComponent(TextureRenderComponent.class).scaleEntity();
    obstacle.scaleHeight(0.5f);
    return obstacle;
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
