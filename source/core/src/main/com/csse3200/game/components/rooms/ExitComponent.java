package com.csse3200.game.components.rooms;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.DreamlandTile;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Spawns the visible door and bookshelf forms of a room's configured exits. */
public class ExitComponent extends EntityManagerComponent {
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";
  private static final String FANTASY_DUNGEON_DOOR = "images/dungeons/fantasy_dreamland_door.png";
  private static final int TILE_SIZE = 16;
  private static final int DOOR_FRAME_SIZE = 32;

  private final ExitConfig[] exits;

  public ExitComponent(ExitConfig[] exits) {
    this.exits = exits;
  }

  @Override
  public void create() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    Texture texture = resourceService.getAsset(FANTASY_DUNGEON_TILESET, Texture.class);
    Texture doorTexture = resourceService.getAsset(FANTASY_DUNGEON_DOOR, Texture.class);
    doorTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TileSheet tileSheet = new TileSheet(texture, TILE_SIZE);
    for (ExitConfig exit : exits) {
      if ("BOOKSHELF".equals(exit.kind)) {
        spawnBookshelf(exit, tileSheet);
      } else if ("DOOR".equals(exit.kind)) {
        spawnDoor(exit, doorTexture);
      }
    }
  }

  private void spawnDoor(ExitConfig exit, Texture doorTexture) {
    Entity door =
        new Entity()
            .addComponent(
                new TextureRenderComponent(
                    new TextureRegion(doorTexture, 0, 0, DOOR_FRAME_SIZE, DOOR_FRAME_SIZE)));
    door.getComponent(TextureRenderComponent.class).scaleEntity();
    spawnEntityAt(door, new GridPoint2(exit.x, exit.y), false, false);
  }

  private void spawnBookshelf(ExitConfig exit, TileSheet tileSheet) {
    spawnFixture(DreamlandTile.BOOKSHELF_LEFT, exit.x, exit.y, tileSheet);
    spawnFixture(DreamlandTile.BOOKSHELF_MIDDLE, exit.x + 1, exit.y, tileSheet);
    spawnFixture(DreamlandTile.BOOKSHELF_RIGHT, exit.x + 2, exit.y, tileSheet);
  }

  private void spawnFixture(DreamlandTile tile, int x, int y, TileSheet tileSheet) {
    Entity fixture = new Entity().addComponent(new TextureRenderComponent(tile.region(tileSheet)));
    fixture.getComponent(TextureRenderComponent.class).scaleEntity();
    spawnEntityAt(fixture, new GridPoint2(x, y), false, false);
  }
}
