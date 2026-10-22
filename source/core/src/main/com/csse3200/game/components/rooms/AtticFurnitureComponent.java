package com.csse3200.game.components.rooms;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.areas.attic.AtticProp;
import com.csse3200.game.areas.attic.AtticSprites;
import com.csse3200.game.areas.attic.AtticTerrain;
import com.csse3200.game.components.rooms.configs.PropConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.PropRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;

/**
 * Spawns a room's furniture props. In attic-themed rooms it also gives every wall tile of the spawn
 * grid an invisible collider (in tileset rooms {@link ObstacleComponent} already does that).
 */
public class AtticFurnitureComponent extends EntityManagerComponent {
  private static final float TILE_WORLD_SIZE = 0.5f;

  private final RoomConfig room;
  private final AtticProp[] resolved;
  private final boolean wallColliders;

  /**
   * @param room an attic-themed room
   * @throws IllegalArgumentException if a prop names an unknown {@link AtticProp}
   */
  public AtticFurnitureComponent(RoomConfig room) {
    this(room, true);
  }

  /**
   * @param room the room whose props to spawn
   * @param wallColliders whether to add colliders for attic wall characters ('#', 'W')
   * @throws IllegalArgumentException if a prop names an unknown {@link AtticProp}
   */
  public AtticFurnitureComponent(RoomConfig room, boolean wallColliders) {
    this.room = room;
    this.wallColliders = wallColliders;
    resolved = new AtticProp[room.props.length];
    for (int i = 0; i < room.props.length; i++) {
      resolved[i] = AtticProp.fromName(room.props[i].prop);
    }
  }

  @Override
  public void create() {
    if (wallColliders) {
      spawnWallColliders(room.obstacles.spawns);
    }
    for (int i = 0; i < resolved.length; i++) {
      PropConfig config = room.props[i];
      spawnEntityAt(
          createProp(resolved[i], config.length, config.elevation),
          new GridPoint2(config.x, config.y),
          false,
          false);
    }
  }

  /**
   * Creates an unplaced prop entity: renderer, plus a static collider if the prop is solid.
   *
   * @param prop which prop
   * @param lengthTiles length for stretchable props, otherwise ignored
   * @param elevationPixels how far to lift the sprite, in sprite pixels
   */
  public static Entity createProp(AtticProp prop, int lengthTiles, float elevationPixels) {
    Texture texture = ServiceLocator.getResourceService().getAsset(prop.getSheet(), Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegion region =
        new TextureRegion(texture, prop.getX(), prop.getY(), prop.getWidth(), prop.getHeight());
    float elevation = AtticSprites.toWorld(elevationPixels);

    PropRenderComponent renderer =
        prop.isStretchable()
            ? new PropRenderComponent(region, prop.getSliceWidth(), elevation)
            : new PropRenderComponent(region, elevation, prop.isFloorCovering());

    Entity entity = new Entity().addComponent(renderer);
    float width = AtticSprites.toWorld(prop.drawnWidth(lengthTiles) * 1f);
    entity.setScale(width, AtticSprites.toWorld(prop.getHeight() * 1f));

    if (prop.isSolid()) {
      // Box along the bottom of the sprite, in entity-local coordinates.
      Vector2 size =
          new Vector2(
              width * prop.getColliderWidth(), AtticSprites.toWorld(prop.getColliderHeight() * 1f));
      entity
          .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
          .addComponent(
              new ColliderComponent()
                  .setLayer(PhysicsLayer.OBSTACLE)
                  .setAsBox(size, new Vector2(width / 2f, size.y / 2f)));
    }
    return entity;
  }

  /** One full-tile collider per wall cell, merged into horizontal runs to keep the count low. */
  private void spawnWallColliders(List<String> grid) {
    for (int y = 0; y < grid.size(); y++) {
      String row = grid.get(y);
      int x = 0;
      while (x < row.length()) {
        if (!AtticTerrain.isWall(row.charAt(x))) {
          x++;
          continue;
        }
        int start = x;
        while (x < row.length() && AtticTerrain.isWall(row.charAt(x))) {
          x++;
        }
        Entity wall = ObstacleFactory.createWall((x - start) * TILE_WORLD_SIZE, TILE_WORLD_SIZE);
        spawnEntityAt(wall, new GridPoint2(start, y), false, false);
      }
    }
  }
}
