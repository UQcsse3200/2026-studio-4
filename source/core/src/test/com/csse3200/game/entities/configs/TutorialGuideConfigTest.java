package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.cutscene.DialogueScript;
import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the Old Sage tutorial NPC is defined, placed and scripted the way the hub expects. */
@ExtendWith(GameExtension.class)
class TutorialGuideConfigTest {
  private static final String NPC_ID = "tutorialGuide";
  private static final String DIALOGUE_ID = "tutorial_guide";

  @Test
  void guideIsDefinedAsADialogueOnlyNpc() {
    InteractableNpcConfig guide = loadGuide();

    assertTrue(guide.hasDialogue());
    assertEquals(DIALOGUE_ID, guide.dialogueId);
    assertFalse(guide.hasCutscene(), "the tutorial is talk only, no cutscene");
    assertTrue(guide.lockMovementDuringDialogue, "the player should stand still while learning");
  }

  @Test
  void guideCanBeRevisitedAnyTime() {
    InteractableNpcConfig guide = loadGuide();

    // Players who forget a control should be able to walk back and ask again, so no gating.
    assertFalse(guide.once, "the tutorial must stay repeatable");
    assertFalse(guide.requiresRoomCleared);
    assertEquals(0, guide.requiresCompleted.length);
  }

  @Test
  void guideStandsInTheStartingRoom() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    RoomConfig start = world.getRoom(world.startRoomId);
    assertNotNull(start);

    boolean spawned = false;
    for (NpcSpawnConfig spawn : start.npcSpawns) {
      spawned |= NPC_ID.equals(spawn.npcId);
    }
    assertTrue(spawned, "the guide has to be the first person a new player can talk to");
  }

  @Test
  void tutorialDialogueLoadsAndOpensWithTheSage() {
    DialogueScript script = DialogueScript.load(DIALOGUE_ID);

    assertNotNull(script, "configs/dialogues/tutorial_guide.json failed to load");
    assertTrue(script.hasLines());
    assertEquals("sage", script.lines[0].speaker);
    for (DialogueScript.Line line : script.lines) {
      assertNotNull(script.findSpeaker(line.speaker), "unknown speaker " + line.speaker);
      assertFalse(line.text == null || line.text.isBlank(), "empty tutorial line");
    }
  }

  private static InteractableNpcConfig loadGuide() {
    InteractableNpcConfigs configs =
        FileLoader.readClass(InteractableNpcConfigs.class, InteractableNpcConfigs.CONFIG_PATH);
    assertNotNull(configs, "friendlyNpcs.json failed to parse");
    InteractableNpcConfig guide = configs.get(NPC_ID);
    assertNotNull(guide, "friendlyNpcs.json must define " + NPC_ID);
    return guide;
  }
}
