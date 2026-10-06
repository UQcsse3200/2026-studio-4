package com.csse3200.game.components.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the real script files shipped in assets/configs load and reference real art. */
@ExtendWith(GameExtension.class)
class ScriptLoadingTest {
  @Test
  void demoDialogueLoadsWithSpeakersAndLines() {
    DialogueScript script = DialogueScript.load("demo");

    assertNotNull(script);
    assertEquals("demo", script.id);
    assertTrue(script.hasLines());
    for (DialogueScript.Line line : script.lines) {
      assertNotNull(script.findSpeaker(line.speaker), "unknown speaker " + line.speaker);
      assertTrue(line.text != null && !line.text.isBlank());
    }
    for (DialogueScript.Speaker speaker : script.speakers) {
      assertTrue(Gdx.files.internal(speaker.atlas).exists(), speaker.atlas);
    }
  }

  @Test
  void jsonExtensionIsAccepted() {
    assertNotNull(DialogueScript.load("demo.json"));
    assertNotNull(CutsceneScript.load("demovideo.json"));
  }

  @Test
  void demoCutsceneLoadsAndPointsAtARealVideo() {
    CutsceneScript video = CutsceneScript.load("demovideo");
    assertNotNull(video);
    assertEquals("demovideo", video.id);
    assertTrue(video.skippable);
    assertTrue(Gdx.files.internal(video.video).exists(), video.video);
  }

  @Test
  void missingOrUnsafeNamesReturnNull() {
    assertNull(DialogueScript.load("nope"));
    assertNull(DialogueScript.load(null));
    assertNull(DialogueScript.load("   "));
    assertNull(DialogueScript.load("../configs/rooms"));
    assertNull(CutsceneScript.load("sub/dir"));
  }

  @Test
  void everyDialogueAndCutsceneUsedByAFriendlyNpcHasAScript() {
    InteractableNpcConfigs configs =
        FileLoader.readClass(InteractableNpcConfigs.class, "configs/friendlyNpcs.json");
    assertNotNull(configs);
    for (InteractableNpcConfig npc : configs.npcs) {
      if (npc.hasDialogue()) {
        assertNotNull(DialogueScript.load(npc.dialogueId), npc.id + " dialogue " + npc.dialogueId);
      }
      if (npc.hasCutscene()) {
        assertNotNull(CutsceneScript.load(npc.cutsceneId), npc.id + " cutscene " + npc.cutsceneId);
      }
    }
  }

  @Test
  void builderMethodsAccumulate() {
    DialogueScript script = new DialogueScript("x").speaker("a", "A", null, null).line("a", "hi");
    assertEquals(1, script.speakers.length);
    assertEquals(1, script.lines.length);

    CutsceneScript cutscene = CutsceneScript.ofVideo("y", "videos/demo.mp4").at(1f, "boom", null);
    assertEquals(1, cutscene.events.length);
    assertEquals("boom", cutscene.events[0].event);
  }
}
