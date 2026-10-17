package com.csse3200.game.components.rooms;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.DreamlandTile;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spawns the visible door and bookshelf forms of a room's configured exits. */
public class ExitComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(ExitComponent.class);
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/tileSet0.png";
  private static final String FANTASY_DUNGEON_DOOR = "images/dungeons/fantasy_dreamland_door.png";
  private static final int TILE_SIZE = 16;
  private static final int DOOR_FRAME_SIZE = 32;
  private static final float INTERACTION_RANGE = 1f;

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

    entity.getEvents().addListener("requestExit", this::onExitRequest);
  }

  // This is kind of an anti pattern since it is kinda forcing a "return value" for the listener
  private void onExitRequest(Vector2 playerPosition) {
    ExitConfig nearest = null;
    float nearestDistance = INTERACTION_RANGE;

    // Finds the nearest exit
    for (ExitConfig exit : exits) {
      Vector2 exitPos =
          entity.getComponent(TerrainComponent.class).tileToWorldPosition(exit.x, exit.y);
      float distance = playerPosition.dst(exitPos);
      if (distance <= nearestDistance) {
        nearest = exit;
        nearestDistance = distance;
      }
    }

    if (nearest == null) {
      logger.debug("No exits found in range");
      // can trigger an event notification for hud things.
      return;
    }

    if (!entity.getComponent(EnemyManagerComponent.class).isCleared()) {
      // room is not yet cleared
      entity.getEvents().trigger(GameAreaDisplay.DISPLAY, "Defeat all enemies to progress");
      return;
    } else if (Objects.equals(nearest.id, "dungeonFiveShelf")
        && ServiceLocator.getRoomClearService().getDungeonClearedCount() < 4) {
      // Access boss room before all other dungeons are cleared
      entity
          .getEvents()
          .trigger(GameAreaDisplay.DISPLAY, "Clear all other dungeons before attempting.");
      return;
    }

    logger.debug("Found exit: {}", nearest.id);
    entity.getEvents().trigger("SwitchRoom", nearest);
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
    spawnFixture(DreamlandTile.randomOfId('b', 0.5f), exit.x, exit.y, tileSheet);
    spawnFixture(DreamlandTile.randomOfId('b', 0.5f), exit.x + 2, exit.y, tileSheet);
    spawnFixture(DreamlandTile.randomOfId('b', 0.5f), exit.x + 4, exit.y, tileSheet);
  }

  // Spawn exits with a component that can be queried via an interact component on the player
  // New component should exist on the player
  // The (player) interact component should trigger a function call on the (door) component
  //      to obtain the ExitConfig linked to that door
  // ExitConfig can then be given to RoomSelector to request a room switch
  private void spawnFixture(DreamlandTile tile, int x, int y, TileSheet tileSheet) {
    Entity fixture = new Entity().addComponent(new TextureRenderComponent(tile.region(tileSheet)));
    fixture.scaleHeight(1f);
    spawnEntityAt(fixture, new GridPoint2(x, y), false, false);
  }
}
