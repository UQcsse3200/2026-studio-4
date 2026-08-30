package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.ServiceLocator;

/** Registers one active room and positions the player at that room's spawn point. */
public class RoomManager {
  private static final GridPoint2 PLAYER_SPAWN = new GridPoint2(10, 10);

  private Entity currentRoom;
  private final Entity player;

  public RoomManager(Entity currentRoom, Entity player) {
    this.currentRoom = currentRoom;
    this.player = player;
  }

  /** Registers the room and player, then starts the room. */
  public void create() {
    EntityService entityService = ServiceLocator.getEntityService();
    entityService.register(currentRoom);
    entityService.register(player);
    start();
  }

  /** Starts the current room and places the player at its spawn tile. */
  public void start() {
    if (currentRoom == null) {
      throw new IllegalStateException("Started the game without setting currentRoom");
    }
    currentRoom.getEvents().trigger("RoomCreated", player);
    TerrainComponent terrain = currentRoom.getComponent(TerrainComponent.class);
    Vector2 playerPosition = terrain.tileToWorldPosition(PLAYER_SPAWN);
    player.setPosition(playerPosition);
  }

  public Entity getCurrentRoom() {
    return currentRoom;
  }
}
