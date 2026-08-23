package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
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
  private static final GridPoint2 PLAYER_SPAWN = new GridPoint2(10, 10);

  public RoomManager(Entity player, RoomFactory roomFactory) {
    this.player = player;
    this.rooms = new ArrayList<Entity>();

    currentRoom = roomFactory.createRoom("First Room");
    ServiceLocator.getEntityService().register(currentRoom);
    movePlayer();
    rooms.add(currentRoom);
  }

  public void nextRoom() {
    for (Entity room : rooms) {
      if (!room.equals(currentRoom)) {
        transitionRoom(room);
      }
    }
  }

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

  private boolean isRegistered(Entity room) {
    for (Entity registeredRoom : rooms) {
      if (registeredRoom.equals(room)) {
        return true;
      }
    }
    return false;
  }

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
}
