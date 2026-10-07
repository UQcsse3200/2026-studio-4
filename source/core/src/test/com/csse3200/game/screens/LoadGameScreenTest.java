package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.files.GameSaveData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LoadGameScreenTest {
  @Test
  void savedSlotCanBeLoaded() {
    int slot = 3;
    GameSaveData save = new GameSaveData();
    save.playTimeSeconds = 42f;
    save.checkpoint.roomId = "final_dungeon";

    try {
      FileLoader.save(save, slot);
      GameSaveData loaded = FileLoader.load(slot);

      assertNotNull(loaded);
      assertEquals(42f, loaded.playTimeSeconds);
      assertEquals("final_dungeon", loaded.checkpoint.roomId);
    } finally {
      FileLoader.deleteSaveSlot(slot);
    }
  }
}
