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
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig.TeleportOffer;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class KeyboardTeleportMenuInputComponentTest {
  private final AtomicLong clock = new AtomicLong(1_000L);
  private final List<String> requests = new ArrayList<>();
  private TeleportMenu menu;
  private KeyboardTeleportMenuInputComponent input;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerInputService(new InputService());
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    Entity player =
        new Entity()
            .addComponent(physics)
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerActions());
    player.create();
    player
        .getEvents()
        .<String, String>addListener(
            RoomSelector.TELEPORT_REQUEST, (room, entry) -> requests.add(room + "/" + entry));

    menu = new TeleportMenu(player, clock::get);
    input = new KeyboardTeleportMenuInputComponent();
    new Entity().addComponent(menu).addComponent(input).create();
  }

  private void openAndWaitOutTheGracePeriod() {
    TeleportOffer offer = new TeleportOffer();
    offer.roomId = "tutorialRoom";
    offer.entryPointId = "fromSage";
    menu.open(offer);
    clock.addAndGet(TeleportMenu.CONFIRM_GRACE_MS);
  }

  @Test
  void shouldSitAboveTheUiStageAndBelowDialogue() {
    assertTrue(input.getPriority() > new KeyboardTerminalInputComponent().getPriority());
    assertTrue(input.getPriority() < 20, "a dialogue must still win over the menu");
  }

  @Test
  void shouldIgnoreEverythingWhileClosed() {
    assertFalse(input.keyDown(Keys.W));
    assertFalse(input.keyDown(Keys.ENTER));
    assertFalse(input.keyUp(Keys.W));
    assertFalse(input.keyTyped('w'));
    assertFalse(input.touchDown(0, 0, 0, 0));
    assertFalse(input.scrolled(0f, 1f));
  }

  @Test
  void shouldSwallowEverythingWhileOpen() {
    openAndWaitOutTheGracePeriod();

    // Movement keys must not walk the player around behind the menu.
    assertTrue(input.keyDown(Keys.A));
    assertTrue(input.keyDown(Keys.D));
    assertTrue(input.keyUp(Keys.A));
    assertTrue(input.keyTyped('a'));
    assertTrue(input.touchDown(0, 0, 0, 0));
    assertTrue(input.touchUp(0, 0, 0, 0));
    assertTrue(input.touchDragged(0, 0, 0));
    assertTrue(input.scrolled(0f, 1f));
    assertTrue(menu.isOpen());
  }

  @Test
  void shouldMoveTheSelectionWithArrowsAndWasd() {
    openAndWaitOutTheGracePeriod();

    assertTrue(input.keyDown(Keys.DOWN));
    assertEquals(TeleportMenu.DECLINE, menu.getSelectedIndex());
    assertTrue(input.keyDown(Keys.UP));
    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
    assertTrue(input.keyDown(Keys.S));
    assertEquals(TeleportMenu.DECLINE, menu.getSelectedIndex());
    assertTrue(input.keyDown(Keys.W));
    assertEquals(TeleportMenu.CONFIRM, menu.getSelectedIndex());
  }

  @Test
  void shouldConfirmWithEnterSpaceOrE() {
    for (int key : new int[] {Keys.ENTER, Keys.NUMPAD_ENTER, Keys.SPACE, Keys.E}) {
      requests.clear();
      openAndWaitOutTheGracePeriod();

      assertTrue(input.keyDown(key));

      assertEquals(List.of("tutorialRoom/fromSage"), requests, "key " + key);
      assertFalse(menu.isOpen());
    }
  }

  @Test
  void shouldLeaveWithEscapeOrQ() {
    for (int key : new int[] {Keys.ESCAPE, Keys.Q}) {
      openAndWaitOutTheGracePeriod();

      assertTrue(input.keyDown(key));

      assertFalse(menu.isOpen(), "key " + key);
    }
    assertTrue(requests.isEmpty());
  }
}
