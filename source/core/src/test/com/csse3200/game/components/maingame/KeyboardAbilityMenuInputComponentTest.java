package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class KeyboardAbilityMenuInputComponentTest {
  private AbilityAttunementComponent attunement;
  private AbilityMenu menu;
  private KeyboardAbilityMenuInputComponent input;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(1_000L);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerInputService(new InputService());

    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());

    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    attunement = new AbilityAttunementComponent();
    Entity player =
        new Entity()
            .addComponent(physics)
            .addComponent(new CombatStatsComponent(100, 10, 2f, 4f))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(attunement)
            .addComponent(new PlayerActions());
    player.create();
    attunement.update();

    menu = new AbilityMenu(player);
    input = new KeyboardAbilityMenuInputComponent();
    Entity ui = new Entity().addComponent(menu).addComponent(input);
    ui.create();
  }

  @Test
  void shouldSitAboveTheUiStageAndBelowDialogue() {
    // The terminal and the scene2d stage are both registered at 10; narrative input is at 20.
    assertTrue(
        input.getPriority() > new KeyboardTerminalInputComponent().getPriority(),
        "The menu must be modal over the terminal and the UI stage");
    assertTrue(input.getPriority() < 20, "A dialogue must still win over the menu");
  }

  @Test
  void shouldIgnoreEverythingWhileClosed() {
    assertFalse(menu.isOpen());

    assertFalse(input.keyDown(Keys.W));
    assertFalse(input.keyDown(Keys.ENTER));
    assertFalse(input.keyUp(Keys.W));
    assertFalse(input.keyTyped('w'));
    assertFalse(input.touchDown(0, 0, 0, 0));
    assertFalse(input.scrolled(0f, 1f));
  }

  @Test
  void shouldSwallowMovementKeysWhileOpen() {
    assertTrue(menu.open());

    // A, D and the walk keys mean nothing here, but must not reach the player underneath.
    assertTrue(input.keyDown(Keys.A));
    assertTrue(input.keyDown(Keys.D));
    assertTrue(input.keyUp(Keys.A));
    assertTrue(input.keyTyped('a'));
    assertTrue(input.touchDown(0, 0, 0, 0));
    assertTrue(input.touchUp(0, 0, 0, 0));
    assertTrue(input.touchDragged(0, 0, 0));
    assertTrue(input.scrolled(0f, 1f));
    assertTrue(menu.isOpen(), "None of that should have closed the menu");
  }

  @Test
  void shouldMoveTheSelectionWithArrowsAndWasd() {
    assertTrue(menu.open());
    assertEquals(0, menu.getSelectedIndex());

    assertTrue(input.keyDown(Keys.DOWN));
    assertEquals(1, menu.getSelectedIndex());
    assertTrue(input.keyDown(Keys.UP));
    assertEquals(0, menu.getSelectedIndex());

    assertTrue(input.keyDown(Keys.S));
    assertEquals(1, menu.getSelectedIndex());
    assertTrue(input.keyDown(Keys.W));
    assertEquals(0, menu.getSelectedIndex());
  }

  @Test
  void shouldConfirmWithEnter() {
    assertTrue(menu.open());
    input.keyDown(Keys.DOWN);

    assertTrue(input.keyDown(Keys.ENTER));

    assertEquals(LastStand.class, attunement.getAttuned());
    assertFalse(menu.isOpen());
  }

  @Test
  void shouldConfirmWithTheSameKeyThatOpenedTheInteraction() {
    assertTrue(menu.open());

    assertTrue(input.keyDown(Keys.E));

    assertEquals(Invisibility.class, attunement.getAttuned());
    assertFalse(menu.isOpen());
  }

  @Test
  void shouldCloseWithEscapeWithoutChangingAnything() {
    assertTrue(menu.open());
    input.keyDown(Keys.DOWN);

    assertTrue(input.keyDown(Keys.ESCAPE));

    assertFalse(menu.isOpen());
    assertEquals(null, attunement.getAttuned(), "Backing out must not attune anything");
  }

  @Test
  void shouldStopSwallowingOnceClosed() {
    assertTrue(menu.open());
    assertTrue(input.keyDown(Keys.ESCAPE));

    assertFalse(input.keyDown(Keys.W), "The player gets their keys back");
    assertFalse(input.keyUp(Keys.W));
  }

  @Test
  void shouldFindItsMenuOnTheSameEntity() {
    AbilityMenu other = new AbilityMenu(new Entity());
    KeyboardAbilityMenuInputComponent wired = new KeyboardAbilityMenuInputComponent();
    Entity ui = new Entity().addComponent(other).addComponent(wired);
    ui.create();

    assertFalse(wired.keyDown(Keys.W), "Closed menu, nothing swallowed");
  }
}
