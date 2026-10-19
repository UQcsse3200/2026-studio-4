package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfig.TeleportOffer;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rooms.RoomSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TeleportMenuTest {
  private final AtomicLong clock = new AtomicLong(1_000L);
  private final List<String> events = new ArrayList<>();
  private final List<String> requests = new ArrayList<>();
  private PlayerActions actions;
  private Entity player;
  private TeleportMenu menu;

  @BeforeEach
  void setUp() {
    actions = new PlayerActions();
    player =
        new Entity()
            .addComponent(physics())
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(actions);
    player.create();
    player
        .getEvents()
        .<String, String>addListener(
            RoomSelector.TELEPORT_REQUEST, (room, entry) -> requests.add(room + "/" + entry));

    menu = new TeleportMenu(player, clock::get);
    Entity ui = new Entity().addComponent(menu);
    ui.create();
    ui.getEvents().addListener(TeleportMenu.MENU_OPENED, () -> events.add("opened"));
    ui.getEvents().addListener(TeleportMenu.MENU_CLOSED, () -> events.add("closed"));
  }

  /** Walking stops through the physics body, so the player needs one to be locked. */
  private static PhysicsComponent physics() {
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    return physics;
  }

  private static TeleportOffer offer() {
    TeleportOffer offer = new TeleportOffer();
    offer.roomId = "tutorialRoom";
    offer.entryPointId = "fromSage";
    return offer;
  }

  private static Entity npcWith(TeleportOffer offer) {
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = "sage";
    config.name = "Old Sage";
    config.dialogueId = "sage_dialogue";
    config.teleportOffer = offer;
    Entity npc = new Entity().addComponent(new NpcInteractableComponent(config));
    npc.create();
    return npc;
  }

  private void finishInteractionWith(Entity npc) {
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "sage", npc);
  }

  private void waitOutTheGracePeriod() {
    clock.addAndGet(TeleportMenu.CONFIRM_GRACE_MS);
  }

  @Test
  void shouldStartClosed() {
    assertFalse(menu.isOpen());
    assertNull(menu.getOffer());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldOpenWhenAnNpcWithAnOfferFinishes() {
    finishInteractionWith(npcWith(offer()));

    assertTrue(menu.isOpen());
    assertEquals("tutorialRoom", menu.getOffer().roomId);
    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
    assertEquals(List.of("opened"), events);
  }

  @Test
  void shouldIgnoreNpcsWithoutAnOffer() {
    finishInteractionWith(npcWith(null));
    finishInteractionWith(null);
    finishInteractionWith(new Entity());

    assertFalse(menu.isOpen());
    assertTrue(events.isEmpty());
  }

  @Test
  void shouldLockControlsWhileOpenAndReleaseThemOnClose() {
    finishInteractionWith(npcWith(offer()));
    assertTrue(actions.areControlsLocked());

    menu.close();

    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked());
    assertEquals(List.of("opened", "closed"), events);
  }

  @Test
  void shouldNotOpenTwice() {
    assertTrue(menu.open(offer()));
    assertFalse(menu.open(offer()));
    assertEquals(List.of("opened"), events);
  }

  @Test
  void shouldRequestTheTeleportWhenConfirmed() {
    menu.open(offer());
    waitOutTheGracePeriod();

    assertTrue(menu.confirm());

    assertEquals(List.of("tutorialRoom/fromSage"), requests);
    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked(), "the player must arrive free to move");
  }

  @Test
  void shouldStayPutWhenTheDeclineRowIsConfirmed() {
    menu.open(offer());
    menu.moveSelection(1);
    waitOutTheGracePeriod();

    assertFalse(menu.confirm());

    assertTrue(requests.isEmpty());
    assertFalse(menu.isOpen());
  }

  @Test
  void shouldIgnoreAConfirmMashedStraightAfterOpening() {
    menu.open(offer());

    assertFalse(menu.confirm());
    assertTrue(menu.isOpen(), "an early press must not close the menu either");
    assertTrue(requests.isEmpty());

    waitOutTheGracePeriod();
    assertTrue(menu.confirm());
  }

  @Test
  void shouldWrapTheSelectionAtBothEnds() {
    menu.open(offer());

    menu.moveSelection(1);
    assertEquals(TeleportMenu.DECLINE, menu.getSelectedIndex());
    menu.moveSelection(1);
    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
    menu.moveSelection(-1);
    assertEquals(TeleportMenu.DECLINE, menu.getSelectedIndex());
  }

  @Test
  void shouldHighlightGoingAheadAgainOnEveryOpen() {
    menu.open(offer());
    menu.moveSelection(1);
    menu.close();

    menu.open(offer());

    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
  }

  @Test
  void shouldDoNothingWhileShut() {
    menu.moveSelection(1);
    assertFalse(menu.confirm());
    menu.close();

    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
    assertTrue(events.isEmpty());
    assertTrue(requests.isEmpty());
  }

  @Test
  void shouldCloseWhenThePlayerDies() {
    menu.open(offer());

    player.getEvents().trigger("entityDied");

    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked(), "dying must not strand the control lock");
  }
}
