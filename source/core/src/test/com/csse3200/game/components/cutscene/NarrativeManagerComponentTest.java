package com.csse3200.game.components.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NarrativeManagerComponentTest {
  /** Records what the manager asks the screen to do, and lets the test drive the callbacks. */
  private static class FakeDialogueView implements DialogueView {
    DialogueScript shown;
    DialogueRunner.Listener listener;
    int advances;
    int closes;
    boolean active;

    @Override
    public void show(DialogueScript script, DialogueRunner.Listener listener) {
      this.shown = script;
      this.listener = listener;
      active = true;
    }

    @Override
    public void advance() {
      advances++;
    }

    @Override
    public void close() {
      closes++;
      active = false;
    }

    @Override
    public boolean isActive() {
      return active;
    }
  }

  private static class FakeCutsceneView implements CutsceneView {
    CutsceneScript played;
    Runnable onFinished;
    int stops;

    @Override
    public void play(CutsceneScript script, Runnable onFinished) {
      this.played = script;
      this.onFinished = onFinished;
    }

    @Override
    public void stop() {
      stops++;
      if (onFinished != null) {
        Runnable callback = onFinished;
        onFinished = null;
        callback.run();
      }
    }

    @Override
    public boolean isActive() {
      return onFinished != null;
    }
  }

  private Entity player;
  private PlayerActions actions;
  private EntityService entityService;
  private FakeDialogueView dialogueView;
  private FakeCutsceneView cutsceneView;
  private NarrativeManagerComponent manager;
  private final List<String> events = new ArrayList<>();

  @BeforeEach
  void setUp() {
    events.clear();
    entityService = new EntityService();
    ServiceLocator.registerEntityService(entityService);
    actions = new PlayerActions();
    player = new Entity().addComponent(actions);
    player.create();
    dialogueView = new FakeDialogueView();
    cutsceneView = new FakeCutsceneView();
    manager = new NarrativeManagerComponent(player, dialogueView, cutsceneView);
    new Entity().addComponent(manager).create();

    listen(CutsceneEvents.DIALOGUE_STARTED, "dialogueStarted:");
    listen(CutsceneEvents.DIALOGUE_FINISHED, "dialogueFinished:");
    listen(CutsceneEvents.CUTSCENE_STARTED, "cutsceneStarted:");
    listen(CutsceneEvents.CUTSCENE_FINISHED, "cutsceneFinished:");
    listen(CutsceneEvents.CUTSCENE_SKIPPED, "cutsceneSkipped:");
    listen(CutsceneEvents.DIALOGUE_CUTSCENE_CUE, "cue:");
    player
        .getEvents()
        .addListener(
            CutsceneEvents.DIALOGUE_LINE_SHOWN,
            (String id, Integer index) -> events.add("line:" + id + ":" + index));
  }

  private void listen(String event, String prefix) {
    player.getEvents().addListener(event, (String id) -> events.add(prefix + id));
  }

  private static DialogueScript dialogue(String id) {
    return new DialogueScript(id).speaker("a", "A", null, null).line("a", "one").line("a", "two");
  }

  // ------------------------------------------------------------- dialogue

  @Test
  void dialogueFreezesWorldLocksControlsAndReleasesWhenFinished() {
    assertTrue(manager.playDialogue(dialogue("d")));

    assertTrue(entityService.isFrozen());
    assertTrue(actions.areControlsLocked());
    assertTrue(manager.isNarrativeActive());
    assertEquals(List.of("dialogueStarted:d"), events);

    dialogueView.listener.onFinished();

    assertFalse(entityService.isFrozen());
    assertFalse(actions.areControlsLocked());
    assertFalse(manager.isNarrativeActive());
    assertEquals(1, dialogueView.closes);
    assertEquals(List.of("dialogueStarted:d", "dialogueFinished:d"), events);
  }

  @Test
  void lineShownEventsAndCuesAreForwarded() {
    DialogueScript script = dialogue("d");
    script.lines[1].cue = true;
    manager.playDialogue(script);

    dialogueView.listener.onLineShown(0, script.lines[0]);
    dialogueView.listener.onLineShown(1, script.lines[1]);

    assertEquals(List.of("dialogueStarted:d", "line:d:0", "line:d:1", "cue:d"), events);
  }

  @Test
  void startDialogueEventPlaysTheDialogueAndMissingFilesStillFinish() {
    player.getEvents().trigger(CutsceneEvents.START_DIALOGUE, "does_not_exist", (Entity) null);

    // nothing to show, but anything waiting on the dialogue (e.g. an NPC) is released
    assertEquals(List.of("dialogueFinished:does_not_exist"), events);
    assertFalse(entityService.isFrozen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void scriptEventPlaysAScriptBuiltInCode() {
    CutsceneEvents.playDialogue(player, dialogue("built"));
    assertNotNull(dialogueView.shown);
    assertEquals("built", dialogueView.shown.id);
  }

  @Test
  void secondDialogueIsIgnoredWhileOneIsOpen() {
    manager.playDialogue(dialogue("first"));
    events.clear();

    assertFalse(manager.playDialogue(dialogue("second")));

    assertEquals("first", dialogueView.shown.id);
    assertTrue(events.isEmpty());
  }

  @Test
  void emptyDialogueFinishesWithoutFreezing() {
    assertFalse(manager.playDialogue(new DialogueScript("empty")));
    assertEquals(List.of("dialogueFinished:empty"), events);
    assertFalse(entityService.isFrozen());
  }

  @Test
  void onceOnlyDialoguePlaysOncePerSession() {
    DialogueScript script = dialogue("once");
    script.once = true;
    assertTrue(manager.playDialogue(script));
    dialogueView.listener.onFinished();
    events.clear();

    assertFalse(manager.playDialogue(script));
    assertEquals(List.of("dialogueFinished:once"), events);
    assertFalse(entityService.isFrozen());

    manager.resetPlayed();
    assertTrue(manager.playDialogue(script));
  }

  @Test
  void aFollowUpCanStartFromInsideTheFinishedEvent() {
    manager.playDialogue(dialogue("first"));
    player
        .getEvents()
        .addListener(
            CutsceneEvents.DIALOGUE_FINISHED,
            (String id) ->
                manager.playCutscene(CutsceneScript.ofVideo("after", "videos/demo.mp4")));

    dialogueView.listener.onFinished();

    assertEquals("after", cutsceneView.played.id);
    assertTrue(entityService.isFrozen());
    assertTrue(actions.areControlsLocked());
  }

  @Test
  void advanceGoesToTheDialogue() {
    manager.playDialogue(dialogue("d"));
    assertTrue(manager.advance());
    assertEquals(1, dialogueView.advances);
  }

  // ------------------------------------------------------------- cutscene

  @Test
  void cutsceneFreezesWorldLocksControlsAndReleasesWhenFinished() {
    assertTrue(manager.playCutscene(CutsceneScript.ofVideo("c", "videos/demo.mp4")));

    assertTrue(entityService.isFrozen());
    assertTrue(actions.areControlsLocked());
    assertEquals(List.of("cutsceneStarted:c"), events);

    cutsceneView.onFinished.run();

    assertFalse(entityService.isFrozen());
    assertFalse(actions.areControlsLocked());
    assertEquals(List.of("cutsceneStarted:c", "cutsceneFinished:c"), events);
  }

  @Test
  void cutsceneThatFinishesImmediatelyDoesNotLeaveTheGameFrozen() {
    // e.g. a video that cannot be decoded: the view reports finished from inside play()
    CutsceneView instant =
        new CutsceneView() {
          @Override
          public void play(CutsceneScript script, Runnable onFinished) {
            onFinished.run();
          }

          @Override
          public void stop() {
            // not needed: this view finishes on its own from inside play()
          }

          @Override
          public boolean isActive() {
            return false;
          }
        };
    Entity other = new Entity().addComponent(new PlayerActions());
    other.create();
    NarrativeManagerComponent instantManager =
        new NarrativeManagerComponent(other, dialogueView, instant);
    new Entity().addComponent(instantManager).create();

    instantManager.playCutscene(CutsceneScript.ofVideo("v", "videos/nope.mp4"));

    assertFalse(entityService.isFrozen());
    assertFalse(other.getComponent(PlayerActions.class).areControlsLocked());
    assertFalse(instantManager.isNarrativeActive());
  }

  @Test
  void secondCutsceneIsIgnoredWhileOneIsPlaying() {
    manager.playCutscene(CutsceneScript.ofVideo("first", "videos/demo.mp4"));
    assertFalse(manager.playCutscene(CutsceneScript.ofVideo("second", "videos/demo.mp4")));
    assertEquals("first", cutsceneView.played.id);
  }

  @Test
  void onceOnlyCutscenePlaysOncePerSession() {
    CutsceneScript script = CutsceneScript.ofVideo("once", "videos/demo.mp4");
    script.once = true;
    manager.playCutscene(script);
    cutsceneView.onFinished.run();
    events.clear();

    assertFalse(manager.playCutscene(script));
    assertEquals(List.of("cutsceneFinished:once"), events);
  }

  @Test
  void skippableCutsceneCanBeSkipped() {
    manager.playCutscene(CutsceneScript.ofVideo("c", "videos/demo.mp4"));

    assertTrue(manager.advance());

    assertEquals(1, cutsceneView.stops);
    assertEquals(List.of("cutsceneStarted:c", "cutsceneSkipped:c", "cutsceneFinished:c"), events);
    assertFalse(entityService.isFrozen());
  }

  @Test
  void nonSkippableCutsceneSwallowsTheKeyButKeepsPlaying() {
    CutsceneScript script = CutsceneScript.ofVideo("c", "videos/demo.mp4");
    script.skippable = false;
    manager.playCutscene(script);

    assertTrue(manager.advance());

    assertEquals(0, cutsceneView.stops);
    assertTrue(manager.isCutsceneActive());
  }

  @Test
  void timedEventsFireInOrderAtTheirTimes() {
    CutsceneScript script =
        CutsceneScript.ofVideo("c", "videos/demo.mp4").at(4f, "later", "x").at(1f, "early", null);
    player.getEvents().addListener("early", () -> events.add("early"));
    player.getEvents().addListener("later", (String arg) -> events.add("later:" + arg));
    manager.playCutscene(script);
    events.clear();

    manager.update(0.5f);
    assertTrue(events.isEmpty());
    manager.update(0.6f);
    assertEquals(List.of("early"), events);
    manager.update(3f);
    assertEquals(List.of("early", "later:x"), events);
    manager.update(5f);
    assertEquals(2, events.size());
  }

  @Test
  void timedEventsStopFiringOnceTheCutsceneEnds() {
    CutsceneScript script = CutsceneScript.ofVideo("c", "videos/demo.mp4").at(2f, "late", null);
    player.getEvents().addListener("late", () -> events.add("late"));
    manager.playCutscene(script);
    cutsceneView.onFinished.run();
    events.clear();

    manager.update(5f);

    assertTrue(events.isEmpty());
  }

  // ------------------------------------------------------ both at once

  @Test
  void dialogueAndCutsceneReleaseTheirOwnFreezeIndependently() {
    manager.playDialogue(dialogue("d"));
    manager.playCutscene(CutsceneScript.ofVideo("c", "videos/demo.mp4"));

    cutsceneView.onFinished.run();
    assertTrue(entityService.isFrozen(), "dialogue is still open");
    assertTrue(actions.areControlsLocked());

    dialogueView.listener.onFinished();
    assertFalse(entityService.isFrozen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void disposeNeverLeavesTheGameFrozen() {
    manager.playDialogue(dialogue("d"));
    manager.playCutscene(CutsceneScript.ofVideo("c", "videos/demo.mp4"));

    manager.dispose();

    assertFalse(entityService.isFrozen());
    assertFalse(actions.areControlsLocked());
  }
}
