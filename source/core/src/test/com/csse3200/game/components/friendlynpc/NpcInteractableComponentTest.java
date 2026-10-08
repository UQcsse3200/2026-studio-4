package com.csse3200.game.components.friendlynpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.friendlynpc.NpcInteractableComponent.Availability;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NpcInteractableComponentTest {
  private Entity player;
  private NpcInteractorComponent interactor;
  private InteractableNpcConfig config;

  @BeforeEach
  void setUp() {
    interactor = new NpcInteractorComponent();
    player = new Entity().addComponent(interactor);
    player.create();
    config = NpcInteractorComponentTest.config("sage", "hello", null);
    config.name = "Old Sage";
  }

  @Test
  void detectsRangeFromCentres() {
    config.interactionRange = 1.5f;
    NpcInteractableComponent npc = npc(config);

    player.setPosition(1.4f, 0f);
    assertTrue(npc.isInRange(player));
    player.setPosition(1.6f, 0f);
    assertFalse(npc.isInRange(player));
  }

  @Test
  void promptsWithNameAndVerb() {
    config.promptVerb = "Greet";
    assertEquals("Press E — Greet Old Sage", npc(config).getPrompt(player));
  }

  @Test
  void interactingStartsDialogue() {
    String[] started = {null};
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_DIALOGUE, (String id, Entity source) -> started[0] = id);
    NpcInteractableComponent npc = npc(config);

    assertTrue(npc.interact(player));
    assertEquals("hello", started[0]);
    assertEquals(Availability.BUSY, npc.getAvailability(player));
    assertNull(npc.getPrompt(player), "No prompt while the interaction is running");
  }

  @Test
  void onceOnlyNpcCannotBeRepeated() {
    config.once = true;
    config.completedMessage = "Nothing more to say.";
    NpcInteractableComponent npc = npc(config);

    npc.interact(player);
    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hello");

    assertEquals(Availability.ALREADY_COMPLETED, npc.getAvailability(player));
    assertEquals("Nothing more to say.", npc.getPrompt(player));
    assertFalse(npc.interact(player));
  }

  @Test
  void repeatableNpcCanBeTalkedToAgain() {
    NpcInteractableComponent npc = npc(config);

    npc.interact(player);
    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hello");

    assertEquals(Availability.AVAILABLE, npc.getAvailability(player));
    assertTrue(npc.interact(player));
  }

  @Test
  void waitsForRoomToBeCleared() {
    config.requiresRoomCleared = true;
    config.unavailableMessage = "Defeat the enemies first.";
    NpcInteractableComponent npc = npc(config);
    boolean[] cleared = {false};
    npc.setRoomClearedSupplier(() -> cleared[0]);

    assertEquals(Availability.CONDITIONS_UNMET, npc.getAvailability(player));
    assertEquals("Defeat the enemies first.", npc.getPrompt(player));
    assertFalse(npc.interact(player));

    cleared[0] = true;
    assertEquals(Availability.AVAILABLE, npc.getAvailability(player));
  }

  @Test
  void waitsForPrerequisiteNpc() {
    config.requiresCompleted = new String[] {"spirit"};
    NpcInteractableComponent npc = npc(config);

    assertEquals(Availability.CONDITIONS_UNMET, npc.getAvailability(player));
    assertNull(npc.getPrompt(player), "No unavailableMessage configured");

    interactor.markCompleted("spirit");
    assertEquals(Availability.AVAILABLE, npc.getAvailability(player));
  }

  @Test
  void playerWithoutInteractorCannotInteract() {
    Entity bystander = new Entity();
    NpcInteractableComponent npc = npc(config);
    assertEquals(Availability.BUSY, npc.getAvailability(bystander));
    assertFalse(npc.interact(bystander));
  }

  @Test
  void indicatorEventsFireOnlyOnChange() {
    NpcInteractableComponent npc = npc(config);
    int[] shown = {0};
    int[] hidden = {0};
    npc.getEntity().getEvents().addListener(NpcInteractionEvents.INDICATOR_SHOWN, () -> shown[0]++);
    npc.getEntity()
        .getEvents()
        .addListener(NpcInteractionEvents.INDICATOR_HIDDEN, () -> hidden[0]++);

    npc.setIndicatorVisible(true);
    npc.setIndicatorVisible(true);
    npc.setIndicatorVisible(false);

    assertEquals(1, shown[0]);
    assertEquals(1, hidden[0]);
  }

  @Test
  void interactingHidesIndicator() {
    NpcInteractableComponent npc = npc(config);
    npc.setIndicatorVisible(true);
    npc.interact(player);
    assertFalse(npc.isIndicatorVisible());
  }

  private static NpcInteractableComponent npc(InteractableNpcConfig config) {
    NpcInteractableComponent component = new NpcInteractableComponent(config);
    Entity npc = new Entity().addComponent(component);
    npc.create();
    return component;
  }
}
