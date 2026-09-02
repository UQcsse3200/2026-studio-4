package com.csse3200.game.components.rooms.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WorldConfigTest {
  @Test
  void shouldLoadGameWorldDefinition() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");

    assertNotNull(world);
    assertDoesNotThrow(world::validate);
    assertEquals(EnemyType.CHASE, world.rooms[1].enemySpawns[1].type);
    assertEquals(EnemyType.FLOATING_DEMON, world.rooms[2].enemySpawns[3].type);
  }

  @Test
  void shouldRejectBrokenRoomReference() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    world.rooms[0].exits[0].destinationRoomId = "missing";

    assertThrows(IllegalArgumentException.class, world::validate);
  }
}
