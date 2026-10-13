package com.csse3200.game.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.files.FileLoader;

/** A facade for the WorldConfig, returns fully set up unintialised room entities */
public class RoomLoader {
  private String CONFIG_PATH = "configs/rooms.json";
  private static final int ARRIVAL_OFFSET_TILES = 3;
  private WorldConfig world = FileLoader.readClass(WorldConfig.class, CONFIG_PATH);
  private CameraComponent camera; // Storing camera not ideal

  public RoomLoader() {
    world.validate();
  }

  public RoomLoader(CameraComponent camera) {
    this.camera = camera;
    world.validate();
  }

  /** Returns an unintialised starting room as decalred in config */
  public Entity getStartingRoom() {
    RoomConfig room = world.getRoom(world.startRoomId);
    return RoomFactory.createRoom(room, camera, false);
  }

  public Entity getRoom(String roomId, boolean cleared) {
    return RoomFactory.createRoom(world.getRoom(roomId), camera, cleared);
  }

  public Entity getRoom(ExitConfig exit, boolean cleared) {
    return getRoom(exit.destinationRoomId, cleared);
  }

  public Entity getRoom(ExitConfig exit) {
    return getRoom(exit.destinationRoomId, RoomClearService.isRoomCleared(exit.destinationRoomId));
  }

  public GridPoint2 getStartingSpawn() {
    RoomConfig startRoom = world.getRoom(world.startRoomId);
    PositionConfig spawn = startRoom.getEntryPoint(world.startEntryPointId);
    return new GridPoint2(spawn.x, spawn.y);
  }

  public GridPoint2 getSpawnPoint(ExitConfig exit) {
    RoomConfig destination = world.getRoom(exit.destinationRoomId);
    PositionConfig position;

    if (exit.destinationExitId != null) {
      position = arrivalInsideDoor(destination.getExit(exit.destinationExitId));
    } else {
      position = destination.getEntryPoint(exit.destinationEntryPointId);
    }

    return new GridPoint2(position.x, position.y);
  }

  /** Copied from RoomManager */
  private static PositionConfig arrivalInsideDoor(ExitConfig door) {
    PositionConfig arrival = new PositionConfig();
    arrival.x = door.x;
    arrival.y = door.y;
    switch (door.side) {
      case "LEFT":
        arrival.x += ARRIVAL_OFFSET_TILES;
        break;
      case "RIGHT":
        arrival.x -= ARRIVAL_OFFSET_TILES;
        break;
      case "TOP":
        arrival.y -= ARRIVAL_OFFSET_TILES;
        break;
      case "BOTTOM":
        arrival.y += ARRIVAL_OFFSET_TILES;
        break;
      default:
        throw new IllegalStateException("Validated door has invalid side: " + door.side);
    }
    return arrival;
  }
}
