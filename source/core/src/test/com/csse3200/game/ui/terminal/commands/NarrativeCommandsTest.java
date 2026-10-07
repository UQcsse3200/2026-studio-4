package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.cutscene.CutsceneScript;
import com.csse3200.game.components.cutscene.DialogueScript;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NarrativeCommandsTest {
  private Entity player;
  private DialogueScript playedDialogue;
  private CutsceneScript playedCutscene;

  @BeforeEach
  void setUp() {
    playedDialogue = null;
    playedCutscene = null;
    player = new Entity();
    player
        .getEvents()
        .addListener(
            CutsceneEvents.START_DIALOGUE_SCRIPT, (DialogueScript s) -> playedDialogue = s);
    player
        .getEvents()
        .addListener(
            CutsceneEvents.START_CUTSCENE_SCRIPT, (CutsceneScript s) -> playedCutscene = s);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  @Test
  void dialogueCommandPlaysTheNamedFile() {
    assertTrue(new DialogueCommand(player).action(args("demo")));
    assertEquals("demo", playedDialogue.id);
  }

  @Test
  void dialogueCommandAcceptsAJsonExtension() {
    assertTrue(new DialogueCommand(player).action(args("demo.json")));
    assertEquals("demo", playedDialogue.id);
  }

  @Test
  void dialogueCommandRejectsMissingFilesAndBadArguments() {
    DialogueCommand command = new DialogueCommand(player);
    assertFalse(command.action(args("nope")));
    assertFalse(command.action(args()));
    assertFalse(command.action(args("demo", "extra")));
    assertFalse(new DialogueCommand(null).action(args("demo")));
    assertNull(playedDialogue);
  }

  @Test
  void cutsceneCommandPlaysTheNamedFile() {
    assertTrue(new CutsceneCommand(player).action(args("demovideo")));
    assertEquals("demovideo", playedCutscene.id);
    assertEquals("videos/demo.mp4", playedCutscene.video);
  }

  @Test
  void cutsceneCommandRejectsMissingFilesAndBadArguments() {
    CutsceneCommand command = new CutsceneCommand(player);
    assertFalse(command.action(args("nope")));
    assertFalse(command.action(args()));
    assertFalse(new CutsceneCommand(null).action(args("demovideo")));
    assertNull(playedCutscene);
  }
}
