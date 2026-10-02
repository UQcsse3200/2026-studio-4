package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InteractableNpcConfigsTest {
  @Test
  void shippedDefinitionsLoadAndValidate() {
    InteractableNpcConfigs configs = load();
    assertTrue(configs.npcs.length > 0);
    assertDoesNotThrow(configs::validate);
  }

  @Test
  void shippedDefinitionsCoverDialogueOnlyCutsceneOnlyAndBoth() {
    InteractableNpcConfigs configs = load();
    boolean dialogueAndCutscene = false;
    boolean cutsceneOnly = false;
    for (InteractableNpcConfig npc : configs.npcs) {
      dialogueAndCutscene |= npc.hasDialogue() && npc.hasCutscene();
      cutsceneOnly |= !npc.hasDialogue() && npc.hasCutscene();
    }
    assertTrue(dialogueAndCutscene);
    assertTrue(cutsceneOnly);
  }

  @Test
  void everyRoomSpawnNamesADefinedNpc() {
    InteractableNpcConfigs configs = load();
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    for (RoomConfig room : world.rooms) {
      for (NpcSpawnConfig spawn : room.npcSpawns) {
        assertNotNull(configs.get(spawn.npcId), room.id + " spawns unknown NPC " + spawn.npcId);
      }
    }
  }

  @Test
  void rejectsNpcWithNeitherDialogueNorCutscene() {
    InteractableNpcConfig npc = valid("a");
    npc.dialogueId = null;
    assertThrows(IllegalArgumentException.class, () -> of(npc).validate());
  }

  @Test
  void rejectsDuplicateIds() {
    assertThrows(IllegalArgumentException.class, () -> of(valid("a"), valid("a")).validate());
  }

  @Test
  void rejectsUnknownPrerequisite() {
    InteractableNpcConfig npc = valid("a");
    npc.requiresCompleted = new String[] {"ghost"};
    assertThrows(IllegalArgumentException.class, () -> of(npc).validate());
  }

  @Test
  void rejectsMissingSprite() {
    InteractableNpcConfig npc = valid("a");
    npc.atlas = null;
    assertThrows(IllegalArgumentException.class, () -> of(npc).validate());
  }

  @Test
  void acceptsCutsceneOnlyNpcWithTexture() {
    InteractableNpcConfig npc = valid("a");
    npc.dialogueId = null;
    npc.cutsceneId = "scene";
    npc.atlas = null;
    npc.texture = "images/rock.png";
    assertDoesNotThrow(() -> of(npc).validate());
  }

  private static InteractableNpcConfigs load() {
    InteractableNpcConfigs configs =
        FileLoader.readClass(InteractableNpcConfigs.class, InteractableNpcConfigs.CONFIG_PATH);
    assertNotNull(configs, "friendlyNpcs.json failed to parse");
    return configs;
  }

  private static InteractableNpcConfigs of(InteractableNpcConfig... npcs) {
    InteractableNpcConfigs configs = new InteractableNpcConfigs();
    configs.npcs = npcs;
    return configs;
  }

  private static InteractableNpcConfig valid(String id) {
    InteractableNpcConfig npc = new InteractableNpcConfig();
    npc.id = id;
    npc.name = id;
    npc.dialogueId = "talk";
    npc.atlas = "images/ghost.atlas";
    npc.animation = "float";
    return npc;
  }
}
