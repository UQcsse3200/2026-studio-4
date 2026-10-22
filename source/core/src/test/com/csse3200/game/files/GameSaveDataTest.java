package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameSaveDataTest {
  @Test
  void saveDataStoresProgress() {
    GameSaveData save = new GameSaveData();
    save.playTimeSeconds = 42f;
    save.resumePosition = new GameSaveData.ResumePosition();
    save.resumePosition.roomId = "final_dungeon";
    save.resumePosition.x = 12f;
    save.resumePosition.y = 8f;
    save.playerData.gold = 99;
    save.playerData.inventory.put("health_potion", 3);

    assertEquals(2, save.version);
    assertEquals(42f, save.playTimeSeconds);
    assertEquals("final_dungeon", save.resumePosition.roomId);
    assertEquals(12f, save.resumePosition.x);
    assertEquals(8f, save.resumePosition.y);
    assertEquals(99, save.playerData.gold);
    assertEquals(3, save.playerData.inventory.get("health_potion"));
  }
}
