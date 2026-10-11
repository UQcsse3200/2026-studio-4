package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.HashSet;
import java.util.Set;
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
  void hecateIsRepeatableAndUnconditional() {
    InteractableNpcConfig hecate = load().get("hecate");
    assertNotNull(hecate, "friendlyNpcs.json must define hecate");
    assertTrue(hecate.hasDialogue());
    // Swapping abilities is the whole point of her, so none of the gating flags may be set: a
    // once-only or room-gated Hecate would let the player pick an ability exactly once per run.
    assertFalse(hecate.once, "Hecate must stay usable");
    assertFalse(hecate.requiresRoomCleared);
    assertEquals(0, hecate.requiresCompleted.length);
  }

  @Test
  void hecateStandsInTheStartingRoom() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    RoomConfig start = world.getRoom(world.startRoomId);
    assertNotNull(start);

    boolean spawned = false;
    for (NpcSpawnConfig spawn : start.npcSpawns) {
      spawned |= "hecate".equals(spawn.npcId);
    }
    assertTrue(spawned, "Hecate belongs in the hub, where abilities are swapped between runs");
  }

  @Test
  void noTwoNpcsShareATile() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    for (RoomConfig room : world.rooms) {
      Set<String> tiles = new HashSet<>();
      for (NpcSpawnConfig spawn : room.npcSpawns) {
        assertTrue(
            tiles.add(spawn.x + "," + spawn.y),
            room.id + " stacks " + spawn.npcId + " on another NPC at " + spawn.x + "," + spawn.y);
      }
    }
  }

  @Test
  void rejectsNpcWithNeitherDialogueNorCutscene() {
    InteractableNpcConfig npc = valid("a");
    npc.dialogueId = null;
    InteractableNpcConfigs configs = of(npc);
    assertThrows(IllegalArgumentException.class, configs::validate);
  }

  @Test
  void rejectsDuplicateIds() {
    InteractableNpcConfigs configs = of(valid("a"), valid("a"));
    assertThrows(IllegalArgumentException.class, configs::validate);
  }

  @Test
  void rejectsUnknownPrerequisite() {
    InteractableNpcConfig npc = valid("a");
    npc.requiresCompleted = new String[] {"ghost"};
    InteractableNpcConfigs configs = of(npc);
    assertThrows(IllegalArgumentException.class, configs::validate);
  }

  @Test
  void rejectsMissingSprite() {
    InteractableNpcConfig npc = valid("a");
    npc.atlas = null;
    InteractableNpcConfigs configs = of(npc);
    assertThrows(IllegalArgumentException.class, configs::validate);
  }

  @Test
  void acceptsCutsceneOnlyNpcWithTexture() {
    InteractableNpcConfig npc = valid("a");
    npc.dialogueId = null;
    npc.cutsceneId = "scene";
    npc.atlas = null;
    npc.texture = "images/rock.png";
    InteractableNpcConfigs configs = of(npc);
    assertDoesNotThrow(configs::validate);
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
