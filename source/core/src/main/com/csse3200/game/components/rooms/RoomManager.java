package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomManager {
  private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);
  private List<Entity> rooms;
  private Entity currentRoom;
  private Entity player;
  private CameraComponent camera;
  private static final GridPoint2 PLAYER_SPAWN = new GridPoint2(10, 10);

  /**
   * @requires unregistered player
   * @param player
   * @param camera
   */
  public RoomManager(Entity player, CameraComponent camera) {
    this.player = player;
    this.camera = camera;
    this.rooms = new ArrayList<Entity>();
  }

  /** Create a new RoomManager with a new player */
  public RoomManager(CameraComponent camera) {
    this(PlayerFactory.createPlayer(), camera);
  }

  public void create() {
    ServiceLocator.getEntityService().register(player);
    currentRoom = RoomFactory.createRoom("First Room", camera);
    rooms.add(currentRoom);
    ServiceLocator.getEntityService().register(currentRoom);

    currentRoom.getEvents().trigger("RoomCreated", player);
  }

  /**
   * Switches the player to the given room.
   *
   * <p>if the room has been added before, will not register to entity service.
   *
   * @param room must be unregistered to entity service
   */
  public void switchRoom(Entity room) {
    currentRoom.setEnabled(false);

    if (isRegistered(room)) {
      currentRoom = room;
      currentRoom.setEnabled(true);
    } else {
      currentRoom = room;
      rooms.add(currentRoom);
      ServiceLocator.getEntityService().register(room);
      logger.info("switched room: {}", currentRoom.toString());

      currentRoom.getEvents().trigger("RoomCreated", player);
    }

    resetPlayerPos();
  }

  /**
   * Returns true if the room is stored in rooms
   *
   * @param room
   * @return
   */
  private boolean isRegistered(Entity room) {
    for (Entity registeredRoom : rooms) {
      if (registeredRoom.equals(room)) {
        return true;
      }
    }
    return false;
  }

  /** sets the player position to PLAYER_SPAWN */
  private void resetPlayerPos() {
    Vector2 pos =
        currentRoom.getComponent(TerrainComponent.class).tileToWorldPosition(PLAYER_SPAWN);
    player.setPosition(pos);
  }

  public Entity getPlayer() {
    return player;
  }
}
