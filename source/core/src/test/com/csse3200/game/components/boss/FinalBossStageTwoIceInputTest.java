package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.KeyboardPlayerInputComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoIceInputTest {
  private boolean active;
  private boolean energy;
  private boolean physicalKeyDown;
  private int ordinaryAttacks;
  private int heavyAttacks;
  private int walks;
  private InputService service;
  private FinalBossStageTwoIceInput input;

  @BeforeEach
  void setUp() {
    active = true;
    energy = true;
    physicalKeyDown = true;
    service = new InputService();
    ServiceLocator.registerInputService(service);
    Entity player = new Entity();
    KeyboardPlayerInputComponent keyboard = new KeyboardPlayerInputComponent();
    player.addComponent(keyboard);
    player.getEvents().addListener("attack", () -> ordinaryAttacks++);
    player.getEvents().addListener("heavyAttack", () -> heavyAttacks++);
    player.getEvents().addListener("walk", (Vector2 direction) -> walks++);
    service.register(keyboard);
    input = new FinalBossStageTwoIceInput(() -> active, () -> energy, () -> physicalKeyDown);
    input.create();
  }

  @Test
  void energyInterceptsJBeforeOrdinaryAttack() {
    assertEquals(6, input.getPriority());
    assertTrue(service.keyDown(Keys.J));
    assertTrue(input.isHeld());
    assertEquals(0, ordinaryAttacks);
    assertTrue(service.keyUp(Keys.J));
    assertFalse(input.isHeld());
  }

  @Test
  void emptyEnergyPassesOrdinaryAttackAndRemembersHoldAcrossPickup() {
    energy = false;
    assertTrue(service.keyDown(Keys.J));
    assertEquals(1, ordinaryAttacks);
    assertTrue(input.isHeld());

    energy = true;
    assertTrue(input.isHeld());
    assertEquals(1, ordinaryAttacks);
    assertTrue(service.keyUp(Keys.J));
    assertFalse(input.isHeld());
  }

  @Test
  void interceptedReleaseIsConsumedEvenWhenEnergyRanOut() {
    assertTrue(input.keyDown(Keys.J));
    energy = false;
    assertTrue(input.keyUp(Keys.J));
    assertFalse(input.isHeld());
    assertFalse(input.keyUp(Keys.J));
  }

  @Test
  void emptyEnergyDoesNotConsumeAnOrdinaryRelease() {
    energy = false;
    assertFalse(input.keyDown(Keys.J));
    assertFalse(input.keyUp(Keys.J));
    assertFalse(input.isHeld());
  }

  @Test
  void unrelatedKeysKeepMovementAndHeavyAttackHandlers() {
    assertTrue(service.keyDown(Keys.D));
    assertTrue(service.keyDown(Keys.K));
    assertEquals(1, walks);
    assertEquals(1, heavyAttacks);
    assertFalse(input.isHeld());
    input.keyDown(Keys.J);
    assertFalse(input.keyUp(Keys.K));
    assertTrue(input.isHeld());
  }

  @Test
  void terminalConsumesPressBeforeIceOverrideAndPollingCannotAcquireIt() {
    Terminal terminal = new Terminal();
    service.register(new KeyboardTerminalInputComponent(terminal));
    terminal.setOpen();
    assertTrue(service.keyDown(Keys.J));
    assertFalse(input.isHeld());
    assertEquals(0, ordinaryAttacks);

    terminal.setClosed();
    assertFalse(input.isHeld());
    assertTrue(service.keyDown(Keys.J));
    assertTrue(input.isHeld());
  }

  @Test
  void physicalPollingClearsAReleaseConsumedByTheTerminal() {
    Terminal terminal = new Terminal();
    service.register(new KeyboardTerminalInputComponent(terminal));
    service.keyDown(Keys.J);
    terminal.setOpen();
    physicalKeyDown = false;
    assertTrue(service.keyUp(Keys.J));
    assertFalse(input.isHeld());

    terminal.setClosed();
    physicalKeyDown = true;
    assertFalse(input.isHeld());
  }

  @Test
  void openingARegisteredTerminalClearsAnAlreadyHeldPress() {
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    Terminal terminal = new Terminal();
    entities.register(new Entity().addComponent(terminal));
    service.register(new KeyboardTerminalInputComponent(terminal));
    service.keyDown(Keys.J);
    assertTrue(input.isHeld());

    service.keyDown(Keys.F1);
    assertTrue(terminal.isOpen());
    assertFalse(input.isHeld());
    service.keyDown(Keys.F1);
    assertFalse(terminal.isOpen());
    assertFalse(input.isHeld());
    service.keyDown(Keys.J);
    assertTrue(input.isHeld());
  }

  @Test
  void firstEntityServiceUpdateWithoutJOrEnergyKeepsUpdatingOtherEntities() {
    energy = false;
    physicalKeyDown = false;
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    boolean[] sampledHeld = {true};
    int[] otherUpdates = {0};
    registerUpdatingEntity(entities, () -> sampledHeld[0] = input.isHeld());
    entities.register(new Entity().addComponent(new Terminal()));
    registerUpdatingEntity(entities, () -> otherUpdates[0]++);

    // Use the real outer entities iteration: direct isHeld() calls cannot reproduce this failure.
    assertDoesNotThrow(entities::update);
    assertFalse(sampledHeld[0]);
    assertEquals(1, otherUpdates[0]);
    assertEquals(0, ordinaryAttacks);
  }

  @Test
  void terminalOpeningAndClosingDuringEntityUpdatesDoesNotInterruptOtherEntities() {
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    boolean[] sampledHeld = {false};
    int[] otherUpdates = {0};
    registerUpdatingEntity(entities, () -> sampledHeld[0] = input.isHeld());
    Terminal terminal = new Terminal();
    entities.register(new Entity().addComponent(terminal));
    service.register(new KeyboardTerminalInputComponent(terminal));
    registerUpdatingEntity(entities, () -> otherUpdates[0]++);

    service.keyDown(Keys.J);
    assertDoesNotThrow(entities::update);
    assertTrue(sampledHeld[0]);
    assertEquals(1, otherUpdates[0]);

    service.keyDown(Keys.F1);
    assertTrue(terminal.isOpen());
    assertDoesNotThrow(entities::update);
    assertFalse(sampledHeld[0]);
    assertEquals(2, otherUpdates[0]);

    service.keyDown(Keys.F1);
    assertFalse(terminal.isOpen());
    assertDoesNotThrow(entities::update);
    assertFalse(sampledHeld[0]);
    assertEquals(3, otherUpdates[0]);

    service.keyDown(Keys.J);
    assertDoesNotThrow(entities::update);
    assertTrue(sampledHeld[0]);
    assertEquals(4, otherUpdates[0]);
  }

  @Test
  void textFieldFocusClearsHeldInputUntilANewObservedPress() {
    RenderService rendering = new RenderService();
    Stage stage = mock(Stage.class);
    rendering.setStage(stage);
    ServiceLocator.registerRenderService(rendering);
    input.keyDown(Keys.J);
    assertTrue(input.isHeld());

    TextField textField = mock(TextField.class);
    when(stage.getKeyboardFocus()).thenReturn(textField);
    assertFalse(input.isHeld());
    assertFalse(input.keyDown(Keys.J));
    when(stage.getKeyboardFocus()).thenReturn(null);
    assertFalse(input.isHeld());
    assertTrue(input.keyDown(Keys.J));
    assertTrue(input.isHeld());
  }

  @Test
  void inactiveEncounterClearsHoldAndPassesJThrough() {
    service.keyDown(Keys.J);
    active = false;
    assertFalse(input.isHeld());
    assertTrue(service.keyDown(Keys.J));
    assertEquals(1, ordinaryAttacks);
    active = true;
    assertFalse(input.isHeld());
  }

  @Test
  void resetRequiresANewObservedPressEvenIfPhysicalJIsStillDown() {
    input.keyDown(Keys.J);
    input.reset();
    assertFalse(input.isHeld());
    input.keyDown(Keys.J);
    assertTrue(input.isHeld());
  }

  @Test
  void disabledInputPassesJAndDoesNotReviveAnOldPressWhenEnabled() {
    input.keyDown(Keys.J);
    input.setEnabled(false);
    assertFalse(input.keyDown(Keys.J));
    assertFalse(input.isHeld());
    input.setEnabled(true);
    assertFalse(input.isHeld());
  }

  @Test
  void registrationIsIdempotentAndDisposalUsesTheOriginalService() {
    input.dispose();
    InputService first = spy(new InputService());
    ServiceLocator.registerInputService(first);
    input = new FinalBossStageTwoIceInput(() -> active, () -> energy, () -> physicalKeyDown);
    input.create();
    input.create();
    verify(first, times(1)).register(input);
    input.keyDown(Keys.J);

    InputService replacement = mock(InputService.class);
    ServiceLocator.registerInputService(replacement);
    input.dispose();
    input.dispose();
    input.create();
    verify(first, times(1)).unregister(input);
    verify(replacement, never()).unregister(input);
    verify(replacement, never()).register(input);
    assertFalse(input.isHeld());
    assertFalse(input.keyDown(Keys.J));
    assertFalse(input.keyUp(Keys.J));
  }

  @Test
  void createCanRetryAfterTheInputServiceBecomesAvailable() {
    input.dispose();
    ServiceLocator.registerInputService(null);
    input = new FinalBossStageTwoIceInput(() -> active, () -> energy, () -> physicalKeyDown);
    input.create();

    ServiceLocator.registerInputService(service);
    input.create();
    service.keyDown(Keys.J);
    assertEquals(0, ordinaryAttacks);
    assertTrue(input.isHeld());
    input.dispose();
    service.keyDown(Keys.J);
    assertEquals(1, ordinaryAttacks);
  }

  @Test
  void defaultPhysicalPollingUsesGdxAndToleratesMissingInput() {
    input.dispose();
    Input previous = Gdx.input;
    Input physical = mock(Input.class);
    try {
      Gdx.input = physical;
      input = new FinalBossStageTwoIceInput(() -> active, () -> energy);
      when(physical.isKeyPressed(Keys.J)).thenReturn(true);
      input.keyDown(Keys.J);
      assertTrue(input.isHeld());
      when(physical.isKeyPressed(Keys.J)).thenReturn(false);
      assertFalse(input.isHeld());
      when(physical.isKeyPressed(Keys.J)).thenReturn(true);
      assertFalse(input.isHeld());

      input.keyDown(Keys.J);
      Gdx.input = null;
      assertFalse(input.isHeld());
    } finally {
      Gdx.input = previous;
    }
  }

  private void registerUpdatingEntity(EntityService entities, Runnable update) {
    entities.register(
        new Entity()
            .addComponent(
                new Component() {
                  @Override
                  public void update() {
                    update.run();
                  }
                }));
  }
}
