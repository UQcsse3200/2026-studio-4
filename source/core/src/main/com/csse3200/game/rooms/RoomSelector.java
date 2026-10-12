package com.csse3200.game.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for storing the current room to be rendered and coordination between player and room.
 */
public class RoomSelector {
  private Entity room; // only one room entity should exist at atime
  private Entity player;
  private CameraComponent camera;
  private RoomLoader roomLoader;

  // TODO Cleared tracking should be somewhere else
  private final Set<String> clearedRoomIds = new HashSet<>();
  private final Set<String> completedDungeonIds = new HashSet<>();

  private final EventHandler eventHandler = new EventHandler();
  public final String ROOM_SWITCHED_EVENT = "RoomSwitched";

  RoomSelector(Entity room, Entity player, CameraComponent camera, RoomLoader roomLoader) {
    this.room = room;
    this.player = player;
    this.camera = camera;
    this.roomLoader = roomLoader;
  }

  RoomSelector(Entity room, Entity player, CameraComponent camera) {
    this(room, player, camera, new RoomLoader(camera));
  }

  public RoomSelector(Entity player, CameraComponent camera) {
    this(null, player, camera, new RoomLoader(camera));
  }

  /* Gives to the CameraComponent to the room
   */
  private void resetCamera() {
    // I want the camera to stay with the player and the room can give bounds to the following
    // camera instead.
    var followingCamera = room.getComponent(FollowingCameraComponent.class);
    followingCamera.setCamera(camera);
    followingCamera.setTarget(player);
  }

  /* */
  public void start() {
    this.room = roomLoader.getStartingRoom();
    spawnPlayer(roomLoader.getStartingSpawn());
    resetCamera();

    ServiceLocator.getEntityService().register(player);
    ServiceLocator.getEntityService().register(room);
  }

  /* disposes the old room, then registers new room and sets the player to the given spawn.*/
  private void switchRoom(Entity newRoom, GridPoint2 spawnPoint) {
    // TODO not called by anyone yet
    ServiceLocator.getEntityService().scheduleDisposal(this.room);

    ServiceLocator.getEntityService().register(newRoom);
    spawnPlayer(spawnPoint);

    resetCamera();
  }

  /** Switches the currently rendered room to the destination room declared in exit */
  public void switchRoom(ExitConfig exit) {
    boolean cleared = clearedRoomIds.contains(exit.destinationRoomId);
    switchRoom(roomLoader.getRoom(exit, cleared), roomLoader.getSpawnPoint(exit));

    eventHandler.trigger(ROOM_SWITCHED_EVENT);
  }

  /**
   * Converts the spawn from tile coord to position the set the player to it
   *
   * @require room to be set
   */
  private void spawnPlayer(GridPoint2 spawn) {
    Vector2 pos = room.getComponent(TerrainComponent.class).tileToWorldPosition(spawn);
    player.setPosition(pos);
  }
}
