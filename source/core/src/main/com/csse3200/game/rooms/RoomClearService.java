package com.csse3200.game.rooms;

import com.csse3200.game.entities.Entity;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Singleton class to track the players cleared rooms.
 *
 * <p>Write access should only be given to room entities via attaching events to it.
 */
public class RoomClearService {
  private static final Logger logger = LoggerFactory.getLogger(RoomClearService.class);
  private static final Set<String> clearedRoomIds = new HashSet<>();
  private static final Set<String> clearedDungeonIds = new HashSet<>();

  private RoomClearService() {}

  /**
   * Provides listeners to room events to track the cleared rooms
   *
   * <p>Currently only room entities should have write access
   *
   * <p>If a feature wishes run logic on a room clear added a new listener somewhere within the room
   * entities and not to modify this function
   *
   * @param entity The room entity to attach lisners to
   */
  public static void attachListeners(Entity entity) {
    entity
        .getEvents()
        .addListener(
            "RoomCleared",
            (String roomId) -> {
              clearedRoomIds.add(roomId);
              logger.debug("Added {} to cleared rooms", roomId);
            });
    entity
        .getEvents()
        .addListener(
            "DungeonCleared",
            (String dungeonId) -> {
              clearedDungeonIds.add(dungeonId);
              logger.debug("Added {} to cleared dungeons", dungeonId);
            });
  }

  public static boolean isRoomCleared(String RoomId) {
    return clearedRoomIds.contains(RoomId);
  }

  public static boolean isDungeonCleared(String dungeonId) {
    return clearedDungeonIds.contains(dungeonId);
  }
}
