package com.csse3200.game.components.rooms;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.attic.AtticProp;
import com.csse3200.game.areas.attic.AtticSprites;
import com.csse3200.game.areas.terrain.DreamlandTile;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TileSheet;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.GlowRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spawns the visible door and bookshelf forms of a room's configured exits, and keeps the
 * interaction prompt in step with the nearest one.
 */
public class ExitComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(ExitComponent.class);
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/tileSet0.png";
  private static final String FANTASY_DUNGEON_DOOR = "images/dungeons/fantasy_dreamland_door.png";
  private static final int TILE_SIZE = 16;
  private static final int DOOR_FRAME_SIZE = 32;
  private static final float INTERACTION_RANGE = 1f;
  private static final String FINAL_DUNGEON_EXIT = "dungeonFiveShelf";
  private static final int DUNGEONS_BEFORE_FINAL = 4;
  private static final float GLOW_RADIUS = 0.45f;
  // Centre of the book on the 16px tome sprite, in sprite pixels from its bottom-left.
  private static final float TOME_CENTRE_X = 8.5f;
  private static final float TOME_CENTRE_Y = 5f;

  private final ExitConfig[] exits;
  private Entity player;

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
      if (exit.sprite != null) {
        spawnSprite(exit);
      } else if ("BOOKSHELF".equals(exit.kind)) {
        spawnBookshelf(exit, tileSheet);
      } else if ("DOOR".equals(exit.kind)) {
        spawnDoor(exit, doorTexture);
      }
    }

    entity.getEvents().addListener("requestExit", this::onExitRequest);
    entity.getEvents().addListener("RoomCreated", this::rememberPlayer);
  }

  /**
   * Shows the exit prompt while the player is near a door, and hides it after they walk away.
   *
   * <p>Pressing E is handled separately. The prompt has to be refreshed every frame because it
   * depends on where the player is standing. A drop already in pickup range keeps "Press E — Pick
   * up …" instead of the door sentence. The boss shelf uses the same sentence as the E key.
   */
  @Override
  public void update() {
    if (player == null) {
      return;
    }
    InteractionPromptDisplay display = player.getComponent(InteractionPromptDisplay.class);
    if (display == null) {
      return;
    }
    PlayerActions actions = player.getComponent(PlayerActions.class);
    if (actions != null && actions.areControlsLocked()) {
      display.clearPrompt();
      return;
    }
    ItemPickupComponent pickup = player.getComponent(ItemPickupComponent.class);
    String itemPrompt = pickup == null ? null : pickup.getPickupPrompt();
    if (itemPrompt != null) {
      display.setPrompt(itemPrompt);
      return;
    }
    EnemyManagerComponent enemies = entity.getComponent(EnemyManagerComponent.class);
    boolean roomCleared = enemies != null && enemies.isCleared();
    display.setPrompt(promptAt(player.getCenterPosition(), roomCleared));
  }

  /**
   * Remembers the player announced by {@code RoomCreated}, so later frames can read their position.
   *
   * @param player the player who entered this room
   */
  private void rememberPlayer(Entity player) {
    this.player = player;
  }

  /**
   * Text for the exit the player is close enough to use.
   *
   * @param playerPosition centre of the player, in world units
   * @param roomCleared whether this room has no enemies left
   * @return the prompt to show, or an empty string when no exit is in range
   */
  String promptAt(Vector2 playerPosition, boolean roomCleared) {
    ExitConfig nearest = nearestExit(playerPosition);
    if (nearest == null) {
      return "";
    }
    return InteractionPrompt.forExit(nearest, roomCleared, false);
  }

  /**
   * The closest configured exit within {@link #INTERACTION_RANGE}, or null when the player is
   * farther away than that.
   *
   * @param playerPosition centre of the player, in world units
   * @return the nearest exit in range, or null
   */
  private ExitConfig nearestExit(Vector2 playerPosition) {
    ExitConfig nearest = null;
    float nearestDistance = INTERACTION_RANGE;
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    for (ExitConfig exit : exits) {
      Vector2 exitPos = terrain.tileToWorldPosition(exit.x, exit.y);
      float distance = playerPosition.dst(exitPos);
      if (distance <= nearestDistance) {
        nearest = exit;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  // This is kind of an anti pattern since it is kinda forcing a "return value" for the listener
  private void onExitRequest(Vector2 playerPosition) {
    ExitConfig nearest = nearestExit(playerPosition);

    if (nearest == null) {
      logger.debug("No exits found in range");
      // can trigger an event notification for hud things.
      return;
    }

    if (!entity.getComponent(EnemyManagerComponent.class).isCleared()) {
      // room is not yet cleared
      entity.getEvents().trigger(GameAreaDisplay.DISPLAY, "Defeat all enemies to progress");
      return;
    } else if (Objects.equals(nearest.id, FINAL_DUNGEON_EXIT) && !finalDungeonUnlocked()) {
      // Access boss room before all other dungeons are cleared
      entity
          .getEvents()
          .trigger(GameAreaDisplay.DISPLAY, InteractionPrompt.CLEAR_ALL_DUNGEONS_REQUIRED);
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

  /**
   * Draws an exit as a attic prop (e.g. a dungeon tome on the reading table). Magical props get a
   * pulsing halo in their colour; the final tome's halo only lights once the other dungeons are
   * cleared, mirroring the lock in {@link #onExitRequest}.
   */
  private void spawnSprite(ExitConfig exit) {
    AtticProp prop = AtticProp.fromName(exit.sprite);
    GridPoint2 tile = new GridPoint2(exit.x, exit.y);
    spawnEntityAt(AtticFurnitureComponent.createProp(prop, 0, exit.elevation), tile, false, false);

    Color glow = prop.getGlow();
    if (glow != null) {
      BooleanSupplier lit =
          FINAL_DUNGEON_EXIT.equals(exit.id) ? ExitComponent::finalDungeonUnlocked : () -> true;
      Vector2 centre =
          new Vector2(AtticSprites.toWorld(TOME_CENTRE_X), AtticSprites.toWorld(TOME_CENTRE_Y));
      Entity halo =
          new Entity()
              .addComponent(
                  new GlowRenderComponent(
                      glow, GLOW_RADIUS, centre, AtticSprites.toWorld(exit.elevation), lit));
      spawnEntityAt(halo, tile, false, false);
    }
  }

  private static boolean finalDungeonUnlocked() {
    return ServiceLocator.getRoomClearService() != null
        && ServiceLocator.getRoomClearService().getDungeonClearedCount() >= DUNGEONS_BEFORE_FINAL;
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
