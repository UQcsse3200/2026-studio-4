package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomManager {
  private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);
  private static final GridPoint2 PLAYER_SPAWN = new GridPoint2(10, 10);

  private Entity currentRoom;
  private Entity player;

  /** Creates a RoomManager with a single room */
  public RoomManager(Entity currentRoom, Entity player) {
    this.currentRoom = currentRoom;
    this.player = player;
  }

  public void create() {
    EntityService entityService = ServiceLocator.getEntityService();

    entityService.register(currentRoom);
    entityService.register(player);
    start();
  }

  /** Starts the room and sets the player to its spawn. */
  public void start() {
    if (currentRoom == null) {
      throw new RuntimeException("Started the game without setting currentRoom");
    }

    currentRoom.getEvents().trigger("RoomCreated", player);
    resetPlayerPos();
  }

  public void switchRoom(Entity room) {
    currentRoom.dispose();

    ServiceLocator.getEntityService().register(room);
    currentRoom = room;

    start();
  }

  /** sets the player position to PLAYER_SPAWN */
  private void resetPlayerPos() {
    Vector2 pos =
        currentRoom.getComponent(TerrainComponent.class).tileToWorldPosition(PLAYER_SPAWN);
    player.setPosition(pos);
  }
}
