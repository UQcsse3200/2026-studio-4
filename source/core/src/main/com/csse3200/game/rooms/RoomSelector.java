package com.csse3200.game.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for storing the current room to be rendered and coordination between player and room.
 */
public class RoomSelector {
  private static final Logger logger = LoggerFactory.getLogger(RoomSelector.class);

  private Entity room; // only one room entity should exist at atime
  private Entity player;
  private CameraComponent camera;
  private RoomLoader roomLoader;

  private final EventHandler eventHandler = new EventHandler();
  public final String ROOM_SWITCHED_EVENT = "RoomSwitched";
  public final String SWITCH_ROOM = "SwitchRoom";

  RoomSelector(Entity room, Entity player, CameraComponent camera, RoomLoader roomLoader) {
    this.room = room;
    this.player = player;
    this.camera = camera;
    this.roomLoader = roomLoader;
  }

  public RoomSelector(Entity player, CameraComponent camera) {
    this(null, player, camera, new RoomLoader(camera));
    player.getEvents().addListener("interact", this::onPlayerInteract);
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
    setRoom(roomLoader.getStartingRoom());
    spawnPlayer(roomLoader.getStartingSpawn());

    ServiceLocator.getEntityService().register(player);
  }

  /** Switches the currently rendered room to the destination room declared in exit */
  public void switchRoom(ExitConfig exit) {
    logger.debug("Switching to {}, cleared: {}", exit.destinationRoomId, cleared);

    setRoom(roomLoader.getRoom(exit, cleared));
    spawnPlayer(roomLoader.getSpawnPoint(exit));

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

  /** Disposes the current room then sets then performs setup on the new room. */
  private void setRoom(Entity newRoom) {
    if (this.room != null) {
      ServiceLocator.getEntityService().scheduleDisposal(this.room);
    }
    this.room = newRoom;

    ServiceLocator.getEntityService().register(newRoom);
    room.getEvents().trigger("RoomCreated", this.player);
    resetCamera();
    room.getEvents().addListener(SWITCH_ROOM, this::switchRoom);
  }

  /**
   * Callback function for the interact event when player presses E. If the room is cleared, will
   * request the room entity for any potential exits.
   */
  private void onPlayerInteract() {
    // TODO Handle room switch via hitbox events
    if (room.getComponent(EnemyManagerComponent.class).isCleared()) {
      room.getEvents().trigger("requestExit", player.getCenterPosition());
    }
  }
}
