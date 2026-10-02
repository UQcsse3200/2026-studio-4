package com.csse3200.game.components.friendlynpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlaceholderNarrativeComponentTest {
  private Entity player;
  private NpcInteractorComponent interactor;
  private PlaceholderNarrativeComponent placeholder;
  private PlayerActions actions;

  @BeforeEach
  void setUp() {
    actions = new PlayerActions();
    interactor = new NpcInteractorComponent();
    placeholder = new PlaceholderNarrativeComponent(1f);
    player = new Entity().addComponent(actions).addComponent(interactor).addComponent(placeholder);
    player.create();
  }

  @Test
  void completesAFullInteractionEndToEnd() {
    interactor.beginInteraction(new Entity(), NpcInteractorComponentTest.config("sage", "a", "b"));
    assertTrue(actions.areControlsLocked());

    placeholder.advance(1f); // dialogue ends, cutscene begins
    assertTrue(interactor.isInteracting());
    assertTrue(actions.areControlsLocked());

    placeholder.advance(1f); // cutscene ends
    assertFalse(interactor.isInteracting());
    assertFalse(actions.areControlsLocked());
    assertTrue(interactor.hasCompleted("sage"));
  }

  @Test
  void cuesDuringDialogueCutsceneHalfway() {
    InteractableNpcConfig config = NpcInteractorComponentTest.config("sage", "a", "b");
    config.cutsceneTiming = InteractableNpcConfig.CutsceneTiming.DURING_DIALOGUE;
    int[] cutscenes = {0};
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_CUTSCENE, (String id, Entity npc) -> cutscenes[0]++);
    interactor.beginInteraction(new Entity(), config);

    placeholder.advance(0.4f);
    assertEquals(0, cutscenes[0]);
    placeholder.advance(0.2f);
    assertEquals(1, cutscenes[0]);
  }
}
