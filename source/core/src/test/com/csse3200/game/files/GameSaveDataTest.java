package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameSaveDataTest {
  @Test
  void saveDataStoresProgress() {
    GameSaveData save = new GameSaveData();
    save.playTimeSeconds = 42f;
    save.checkpoint.roomId = "final_dungeon";
    save.playerData.gold = 99;
    save.playerData.inventory.put("health_potion", 3);

    assertEquals(1, save.version);
    assertEquals(42f, save.playTimeSeconds);
    assertEquals("final_dungeon", save.checkpoint.roomId);
    assertEquals(99, save.playerData.gold);
    assertEquals(3, save.playerData.inventory.get("health_potion"));
  }
}
