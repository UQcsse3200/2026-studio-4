package com.csse3200.game.components.friendlynpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NpcInteractorComponentTest {
  private Entity player;
  private Entity npc;
  private NpcInteractorComponent interactor;
  private PlayerActions actions;
  private final List<String> events = new ArrayList<>();

  @BeforeEach
  void setUp() {
    events.clear();
    actions = new PlayerActions();
    interactor = new NpcInteractorComponent();
    player = new Entity().addComponent(actions).addComponent(interactor);
    player.create();
    npc = new Entity();

    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_DIALOGUE,
            (String id, Entity source) -> {
              assertSame(npc, source);
              events.add("dialogue:" + id);
            });
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_CUTSCENE,
            (String id, Entity source) -> events.add("cutscene:" + id));
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_STARTED,
            (String id, Entity source) -> events.add("started:" + id));
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_FINISHED,
            (String id, Entity source) -> events.add("finished:" + id));
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_CANCELLED,
            (String id, Entity source) -> events.add("cancelled:" + id));
  }

  @Test
  void runsDialogueThenCutsceneAndRestoresControls() {
    assertTrue(interactor.beginInteraction(npc, config("sage", "hello", "scene")));

    assertTrue(interactor.isInteracting());
    assertEquals("sage", interactor.getActiveNpcId());
    assertTrue(actions.areControlsLocked());
    assertEquals(List.of("started:sage", "dialogue:hello"), events);

    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hello");
    assertTrue(actions.areControlsLocked());
    assertEquals(List.of("started:sage", "dialogue:hello", "cutscene:scene"), events);

    player.getEvents().trigger(NpcInteractionEvents.CUTSCENE_FINISHED, "scene");
    assertFalse(interactor.isInteracting());
    assertFalse(actions.areControlsLocked());
    assertTrue(interactor.hasCompleted("sage"));
    assertEquals(
        List.of("started:sage", "dialogue:hello", "cutscene:scene", "finished:sage"), events);
  }

  @Test
  void refusesASecondInteractionWhileBusy() {
    interactor.beginInteraction(npc, config("sage", "hello", null));

    assertFalse(interactor.beginInteraction(new Entity(), config("other", "hi", null)));
    assertEquals("sage", interactor.getActiveNpcId());
  }

  @Test
  void cueStartsDuringDialogueCutscene() {
    InteractableNpcConfig config = config("sage", "hello", "scene");
    config.cutsceneTiming = InteractableNpcConfig.CutsceneTiming.DURING_DIALOGUE;
    interactor.beginInteraction(npc, config);

    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_CUTSCENE_CUE, "hello");

    assertEquals(List.of("started:sage", "dialogue:hello", "cutscene:scene"), events);
  }

  @Test
  void dyingCancelsAndUnlocksWithoutCompleting() {
    interactor.beginInteraction(npc, config("sage", "hello", null));

    player.getEvents().trigger("entityDied");

    assertFalse(interactor.isInteracting());
    assertFalse(actions.areControlsLocked());
    assertFalse(interactor.hasCompleted("sage"));
    assertTrue(events.contains("cancelled:sage"));
  }

  @Test
  void disposeReleasesLock() {
    interactor.beginInteraction(npc, config("sage", "hello", null));
    interactor.dispose();
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void worksWithoutPlayerActions() {
    Entity bare = new Entity().addComponent(new NpcInteractorComponent());
    bare.create();
    NpcInteractorComponent bareInteractor = bare.getComponent(NpcInteractorComponent.class);

    assertTrue(bareInteractor.beginInteraction(npc, config("sage", "hello", null)));
    bare.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hello");
    assertTrue(bareInteractor.hasCompleted("sage"));
  }

  @Test
  void finishEventsWithNoActiveInteractionAreIgnored() {
    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hello");
    player.getEvents().trigger(NpcInteractionEvents.CUTSCENE_FINISHED, "scene");
    assertFalse(interactor.isInteracting());
    assertNull(interactor.getActiveNpcId());
    assertTrue(events.isEmpty());
  }

  @Test
  void markCompletedIsRemembered() {
    interactor.markCompleted("sage");
    assertTrue(interactor.hasCompleted("sage"));
    assertTrue(interactor.getCompletedNpcIds().contains("sage"));
  }

  static InteractableNpcConfig config(String id, String dialogueId, String cutsceneId) {
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = id;
    config.name = id;
    config.dialogueId = dialogueId;
    config.cutsceneId = cutsceneId;
    return config;
  }
}
