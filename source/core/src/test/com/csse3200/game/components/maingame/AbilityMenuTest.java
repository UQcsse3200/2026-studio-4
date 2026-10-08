package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbilityMenuTest {
  private GameTime time;
  private CombatStatsComponent stats;
  private PlayerAbilitiesComponent abilities;
  private AbilityAttunementComponent attunement;
  private PlayerActions actions;
  private Entity player;
  private AbilityMenu menu;
  private Entity ui;
  private final List<String> events = new ArrayList<>();

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(1_000L);
    ServiceLocator.registerTimeSource(time);

    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());

    stats = new CombatStatsComponent(100, 10, 2f, 4f);
    abilities = new PlayerAbilitiesComponent(time);
    attunement = new AbilityAttunementComponent();
    actions = new PlayerActions();
    player =
        new Entity()
            .addComponent(physics)
            .addComponent(stats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(attunement)
            .addComponent(actions)
            .addComponent(new NpcInteractorComponent());
    player.create();
    attunement.update();

    menu = new AbilityMenu(player);
    ui = new Entity().addComponent(menu);
    ui.create();
    ui.getEvents().addListener(AbilityMenu.MENU_OPENED, () -> events.add("opened"));
    ui.getEvents().addListener(AbilityMenu.MENU_CLOSED, () -> events.add("closed"));
  }

  private void finishInteractionWith(String npcId) {
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, npcId, new Entity());
  }

  /** An NPC built the way FriendlyNpcFactory builds one, minus the art. */
  private NpcInteractableComponent npcNamed(String id) {
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = id;
    config.name = id;
    config.dialogueId = id + "_dialogue";
    NpcInteractableComponent npc = new NpcInteractableComponent(config);
    new Entity().addComponent(npc).create();
    return npc;
  }

  @Test
  void shouldOpenOnlyOnceHecateHasFinishedSpeaking() {
    NpcInteractableComponent hecate = npcNamed(AbilityMenu.NPC_ID);

    assertTrue(hecate.interact(player));
    assertFalse(menu.isOpen(), "the menu must not share the screen with her dialogue");

    // What the dialogue system sends when the last line is done.
    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "hecate_dialogue");

    assertTrue(menu.isOpen(), "the menu opens once the interaction completes");
    assertEquals(List.of("opened"), events);
  }

  @Test
  void shouldStayShutForAnotherNpcsWholeInteraction() {
    NpcInteractableComponent merchant = npcNamed("merchant");

    assertTrue(merchant.interact(player));
    player.getEvents().trigger(NpcInteractionEvents.DIALOGUE_FINISHED, "merchant_dialogue");

    assertFalse(menu.isOpen());
    assertTrue(events.isEmpty());
  }

  @Test
  void shouldStartClosedAndEmpty() {
    assertFalse(menu.isOpen());
    assertTrue(menu.getOptions().isEmpty());
    assertNull(menu.getSelected());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldOpenWhenHecatesInteractionFinishes() {
    finishInteractionWith(AbilityMenu.NPC_ID);

    assertTrue(menu.isOpen());
    assertEquals(2, menu.getOptions().size());
    assertEquals(List.of("opened"), events);
  }

  @Test
  void shouldIgnoreOtherNpcs() {
    finishInteractionWith("merchant");
    finishInteractionWith("wanderingSpirit");

    assertFalse(menu.isOpen());
    assertTrue(events.isEmpty());
  }

  @Test
  void shouldHoldItsOwnControlLockWhileOpen() {
    assertTrue(menu.open());
    assertTrue(actions.areControlsLocked(), "The player must not walk away mid-choice");

    menu.close();
    assertFalse(actions.areControlsLocked());
    assertEquals(List.of("opened", "closed"), events);
  }

  @Test
  void shouldStopThePlayerWalkingWhenItOpens() {
    List<String> movement = new ArrayList<>();
    player.getEvents().addListener("walkStop", () -> movement.add("walkStop"));
    player.getEvents().addListener("resetMovementInput", () -> movement.add("resetMovementInput"));
    player.getEvents().trigger("walk", new Vector2(0f, 1f));

    assertTrue(menu.open());

    assertEquals(List.of("walkStop", "resetMovementInput"), movement);
  }

  @Test
  void shouldNotResetMovementWhenAlreadyOpen() {
    assertTrue(menu.open());
    List<String> movement = new ArrayList<>();
    player.getEvents().addListener("walkStop", () -> movement.add("walkStop"));

    assertFalse(menu.open());

    assertTrue(movement.isEmpty(), "a refused open must not touch the player's movement");
  }

  @Test
  void shouldNotKeepWalkingAfterClosingIfTheKeyWasReleasedBehindIt() {
    // Issue #266: hold W, open the menu, let go of W while it is open, then close it.
    Body body = player.getComponent(PhysicsComponent.class).getBody();
    player.getEvents().trigger("walk", new Vector2(0f, 1f));
    actions.update();
    verify(body).applyLinearImpulse(any(), any(), anyBoolean());

    assertTrue(menu.open());
    // The menu swallows the release of W, so no walkStop arrives from the keyboard here.
    menu.close();
    clearInvocations(body);
    actions.update();

    verify(body, never()).applyLinearImpulse(any(), any(), anyBoolean());
  }

  @Test
  void shouldKeepTheLockWhenTheInteractionsOwnLockIsReleased() {
    assertTrue(menu.open());
    // The NPC interaction releases its lock when it finishes; ours is a separate owner.
    actions.setControlsLocked(new Object(), true);
    actions.setControlsLocked(new Object(), false);

    assertTrue(actions.areControlsLocked());
  }

  @Test
  void shouldOfferEveryAbilityInRegistrationOrder() {
    assertTrue(menu.open());

    assertEquals(
        List.of(Invisibility.NAME, LastStand.NAME),
        menu.getOptions().stream().map(a -> a.getName()).toList());
  }

  @Test
  void shouldWrapTheSelectionAtBothEnds() {
    assertTrue(menu.open());
    assertEquals(0, menu.getSelectedIndex());

    menu.moveSelection(1);
    assertEquals(1, menu.getSelectedIndex());
    menu.moveSelection(1);
    assertEquals(0, menu.getSelectedIndex(), "Past the end wraps to the top");
    menu.moveSelection(-1);
    assertEquals(1, menu.getSelectedIndex(), "Before the start wraps to the bottom");
  }

  @Test
  void shouldIgnoreMovementWhileClosed() {
    menu.moveSelection(1);
    assertEquals(0, menu.getSelectedIndex());
    assertFalse(menu.isOpen());
  }

  @Test
  void shouldAttuneTheHighlightedAbilityAndClose() {
    assertTrue(menu.open());
    menu.moveSelection(1);

    assertTrue(menu.confirm());

    assertEquals(LastStand.class, attunement.getAttuned());
    assertTrue(abilities.isUnlocked(LastStand.class));
    assertFalse(abilities.isUnlocked(Invisibility.class));
    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldOpenOnWhateverIsAlreadyAttuned() {
    assertTrue(attunement.attune(LastStand.class));

    assertTrue(menu.open());

    assertEquals(1, menu.getSelectedIndex());
    assertTrue(menu.isAttuned(menu.getSelected()));
  }

  @Test
  void shouldDoNothingOnConfirmWhileClosed() {
    assertFalse(menu.confirm());
    assertNull(attunement.getAttuned());
  }

  @Test
  void shouldCloseEvenWhenAttuningIsRefused() {
    assertTrue(menu.open());
    stats.setHealth(0);

    assertFalse(menu.confirm(), "A dead player cannot be attuned");
    assertFalse(menu.isOpen(), "but must never be left trapped behind the menu");
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldCloseWhenThePlayerDies() {
    assertTrue(menu.open());

    player.getEvents().trigger("entityDied");

    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldNotOpenTwice() {
    assertTrue(menu.open());
    assertFalse(menu.open());
    assertEquals(List.of("opened"), events);
  }

  @Test
  void shouldReleaseTheLockWhenDisposed() {
    assertTrue(menu.open());

    menu.dispose();

    assertFalse(menu.isOpen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldNotOpenForAPlayerWithoutAbilities() {
    Entity bare = new Entity();
    bare.create();
    AbilityMenu orphan = new AbilityMenu(bare);
    Entity orphanUi = new Entity().addComponent(orphan);
    orphanUi.create();

    assertFalse(orphan.open());
    assertFalse(orphan.isOpen());
  }
}
