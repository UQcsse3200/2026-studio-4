package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.commands.Command;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomManager implements Command {
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
    ServiceLocator.getEntityService().register(currentRoom);
  }

  /** transitions to the next room in the list, if it exits */
  public void nextRoom() {
    for (Entity room : rooms) {
      if (!room.equals(currentRoom)) {
        transitionRoom(room);
      }
    }
  }

  /**
   * Disposes the current room then register and switches to the given room
   *
   * @param room
   */
  public void transitionRoom(Entity room) {
    if (!isRegistered(room)) {
      logger.error("Transition to unregistered room");
      return;
    }

    if (room.equals(currentRoom)) {
      logger.error("Transition to current room");
      return;
    }

    currentRoom.dispose();

    ServiceLocator.getEntityService().register(room);
    currentRoom = room;
    movePlayer();
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
  private void movePlayer() {
    Vector2 pos =
        currentRoom.getComponent(TerrainComponent.class).tileToWorldPosition(PLAYER_SPAWN);
    player.setPosition(pos);
  }

  public void addRoom(Entity room) {
    room.setEnabled(false);
    // ServiceLocator.getEntityService().register(room);
    rooms.add(room);
  }

  @Override
  public boolean action(ArrayList<String> args) {
    this.nextRoom();
    return true;
  }

  public Entity getPlayer() {
    return player;
  }
}
