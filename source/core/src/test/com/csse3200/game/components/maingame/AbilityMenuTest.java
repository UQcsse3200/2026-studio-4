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
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
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
            .addComponent(actions);
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
