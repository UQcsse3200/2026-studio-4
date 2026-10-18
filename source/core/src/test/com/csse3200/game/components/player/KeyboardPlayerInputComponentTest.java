package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class KeyboardPlayerInputComponentTest {
  private Entity player;
  private KeyboardPlayerInputComponent input;
  private GameTime time;
  private int roomInteractions;
  private int itemPickups;
  private int magneticPickups;
  private int walkCount;
  private int walkStopCount;
  private int attackCount;
  private int heavyAttackCount;
  private int specialAttackCount;
  private int dashCount;
  private Vector2 lastWalkDirection;
  private Vector2 lastDashDirection;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerInputService(mock(InputService.class));
    player = new Entity();
    input = new KeyboardPlayerInputComponent();
    player.addComponent(input);
    input.create();
    player.getEvents().addListener("interact", () -> roomInteractions++);
    player.getEvents().addListener("itemPickup", () -> itemPickups++);
    player
        .getEvents()
        .addListener(KeyboardPlayerInputComponent.ROOM_MAGNETIC_PICKUP, () -> magneticPickups++);
    player
        .getEvents()
        .addListener(
            "walk",
            (Vector2 direction) -> {
              walkCount++;
              lastWalkDirection = direction.cpy();
            });
    player.getEvents().addListener("walkStop", () -> walkStopCount++);
    player.getEvents().addListener("attack", () -> attackCount++);
    player.getEvents().addListener("heavyAttack", () -> heavyAttackCount++);
    player.getEvents().addListener("specialAttack", () -> specialAttackCount++);
    player
        .getEvents()
        .addListener(
            "dash",
            (Vector2 direction) -> {
              dashCount++;
              lastDashDirection = direction.cpy();
            });
  }

  @Test
  void tabCyclesConsumablesOnceOnKeyDown() {
    int[] cycles = {0};
    player.getEvents().addListener("cycleConsumable", () -> cycles[0]++);
    assertTrue(input.keyDown(Keys.TAB));
    assertFalse(input.keyUp(Keys.TAB));
    assertEquals(1, cycles[0]);
  }

  @Test
  void qRequestsSelectedConsumableWithoutUsingWeaponInput() {
    int[] uses = {0};
    player.getEvents().addListener("useSelectedConsumable", () -> uses[0]++);
    assertTrue(input.keyDown(Keys.Q));
    assertFalse(input.keyUp(Keys.Q));
    assertEquals(1, uses[0]);
    assertEquals(0, attackCount);
  }

  @Test
  void numberKeysSelectWeaponsOnlyOnKeyDown() {
    List<WeaponType> selected = new ArrayList<>();
    player.getEvents().addListener("equipWeapon", (EventListener1<WeaponType>) selected::add);
    for (int key : new int[] {Keys.NUM_1, Keys.NUM_2, Keys.NUM_3}) {
      assertTrue(input.keyDown(key));
      assertFalse(input.keyUp(key));
    }
    assertFalse(input.keyDown(Keys.NUM_4));
    assertEquals(List.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.BOW), selected);
  }

  @Test
  void formerConsumableKeysAreUnbound() {
    for (int key : new int[] {Keys.NUM_7, Keys.NUM_8, Keys.NUM_9, Keys.NUM_0}) {
      assertFalse(input.keyDown(key));
      assertFalse(input.keyUp(key));
    }
  }

  @Test
  void openTerminalConsumesNumberKeysBeforePlayerInput() {
    List<WeaponType> selected = new ArrayList<>();
    player.getEvents().addListener("equipWeapon", (EventListener1<WeaponType>) selected::add);
    InputService service = new InputService();
    Terminal terminal = new Terminal();
    service.register(input);
    service.register(new KeyboardTerminalInputComponent(terminal));
    terminal.setOpen();
    assertTrue(service.keyDown(Keys.NUM_2));
    assertTrue(selected.isEmpty());
    terminal.setClosed();
    assertTrue(service.keyDown(Keys.NUM_2));
    assertEquals(List.of(WeaponType.DAGGER), selected);
  }

  @Test
  void shouldKeepRoomAndItemInteractionsSeparateOnEKey() {
    assertTrue(input.keyDown(Keys.E));
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);

    when(time.getTime()).thenReturn(1999L);
    assertTrue(input.keyUp(Keys.E));

    assertEquals(1, roomInteractions);
    assertEquals(1, itemPickups);
    assertEquals(0, magneticPickups);
  }

  @Test
  void shouldIgnoreEWhileNpcInteractionIsRunning() {
    NpcInteractorComponent interactor = new NpcInteractorComponent();
    player.addComponent(interactor);
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = "sage";
    config.dialogueId = "hello";
    interactor.beginInteraction(new Entity(), config);
    assertTrue(input.keyDown(Keys.E));
    when(time.getTime()).thenReturn(2000L);
    input.update();
    assertTrue(input.keyUp(Keys.E));

    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
    assertEquals(0, magneticPickups);
  }

  @Test
  void shouldNotPickUpItemWhenEStartsNpcInteraction() {
    NpcInteractorComponent interactor = new NpcInteractorComponent();
    player.addComponent(interactor);
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = "sage";
    config.dialogueId = "hello";
    player
        .getEvents()
        .addListener("interact", () -> interactor.beginInteraction(new Entity(), config));

    assertTrue(input.keyDown(Keys.E));
    assertTrue(input.keyUp(Keys.E));

    assertTrue(interactor.isInteracting());
    assertEquals(0, itemPickups);
  }

  @Test
  void holdingETriggersOnlyAtTwoSecondsWithoutNormalInteractions() {
    when(time.getTime()).thenReturn(500L);
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(2499L);
    input.update();
    assertEquals(0, magneticPickups);

    when(time.getTime()).thenReturn(2500L);
    input.update();
    assertEquals(1, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);

    when(time.getTime()).thenReturn(7000L);
    input.update();
    input.keyUp(Keys.E);
    assertEquals(1, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void eKeyRepeatsDoNotRestartTheTimerOrTriggerAnotherBatch() {
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(1000L);
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(2000L);
    input.update();
    assertEquals(1, magneticPickups);

    when(time.getTime()).thenReturn(5000L);
    input.keyDown(Keys.E);
    input.update();
    input.keyUp(Keys.E);
    assertEquals(1, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void releaseAtTwoSecondsTriggersTheBatchBeforeTheNextUpdate() {
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(2000L);

    input.keyUp(Keys.E);
    input.update();

    assertEquals(1, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void aNewPressMustCompleteItsOwnTwoSecondHold() {
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(2000L);
    input.update();
    input.keyUp(Keys.E);

    when(time.getTime()).thenReturn(2500L);
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(4499L);
    input.update();
    assertEquals(1, magneticPickups);
    when(time.getTime()).thenReturn(4500L);
    input.update();
    assertEquals(2, magneticPickups);
  }

  @Test
  void unmatchedEReleaseDoesNotInteract() {
    assertTrue(input.keyUp(Keys.E));
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
    assertEquals(0, magneticPickups);
  }

  @Test
  void movementResetCancelsPendingHoldAndIgnoresTheStaleRelease() {
    input.keyDown(Keys.W);
    input.keyDown(Keys.E);
    when(time.getTime()).thenReturn(1000L);
    player.getEvents().trigger("resetMovementInput");
    input.keyDown(Keys.E); // A repeat from the cancelled press must not start a new hold.
    when(time.getTime()).thenReturn(5000L);
    input.keyUp(Keys.E);
    input.keyUp(Keys.W);
    input.update();

    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
    assertEquals(0, magneticPickups);
    assertEquals(1, walkCount);
    assertEquals(0, walkStopCount);

    input.keyDown(Keys.E);
    input.keyUp(Keys.E);
    assertEquals(1, roomInteractions);
  }

  @Test
  void roomChangeCancelsPendingHold() {
    input.keyDown(Keys.E);
    player.getEvents().trigger("roomPickupReset");
    when(time.getTime()).thenReturn(2000L);
    input.update();
    input.keyUp(Keys.E);

    assertEquals(0, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void dialogueAndCutsceneStartCancelPendingHolds() {
    for (String event : List.of(CutsceneEvents.DIALOGUE_STARTED, CutsceneEvents.CUTSCENE_STARTED)) {
      when(time.getTime()).thenReturn(0L);
      input.keyDown(Keys.E);
      player.getEvents().trigger(event, "test-scene");
      when(time.getTime()).thenReturn(2000L);
      input.update();
      input.keyUp(Keys.E);
    }

    assertEquals(0, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void inputCannotStartOrFinishAnInteractionWhileTheWorldIsFrozen() {
    EntityService entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    input.keyDown(Keys.E);
    when(entities.isFrozen()).thenReturn(true);
    when(time.getTime()).thenReturn(2000L);
    input.keyUp(Keys.E);

    input.keyDown(Keys.E);
    input.keyUp(Keys.E);
    assertEquals(0, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void openingInventoryCancelsPendingHold() {
    InventoryComponent inventory = mock(InventoryComponent.class);
    player.addComponent(inventory);
    input.keyDown(Keys.E);
    input.keyDown(Keys.I);
    when(time.getTime()).thenReturn(2000L);
    input.update();
    input.keyUp(Keys.E);

    assertEquals(0, magneticPickups);
    assertEquals(0, roomInteractions);
    assertEquals(0, itemPickups);
  }

  @Test
  void explicitCancellationPreventsStaleReleaseAndAllowsAReleasedKeyToBeUsedAgain() {
    input.keyDown(Keys.E);
    input.cancelPickupHold();
    when(time.getTime()).thenReturn(5000L);
    // Headless input reports E released, simulating a release swallowed by a modal interface.
    input.update();
    input.keyUp(Keys.E);
    assertEquals(0, magneticPickups);
    assertEquals(0, roomInteractions);

    input.keyDown(Keys.E);
    input.keyUp(Keys.E);
    assertEquals(1, roomInteractions);
    assertEquals(1, itemPickups);
  }

  @Test
  void shouldWalkOnWasdAndStopWhenReleased() {
    assertTrue(input.keyDown(Keys.D));
    assertEquals(1, walkCount);
    assertEquals(1f, lastWalkDirection.x, 0.001f);
    assertEquals(0f, lastWalkDirection.y, 0.001f);

    assertTrue(input.keyUp(Keys.D));
    assertEquals(1, walkStopCount);
  }

  @Test
  void shouldWalkUpOnW() {
    assertTrue(input.keyDown(Keys.W));
    assertEquals(0f, lastWalkDirection.x, 0.001f);
    assertEquals(1f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldWalkLeftOnA() {
    assertTrue(input.keyDown(Keys.A));
    assertEquals(-1f, lastWalkDirection.x, 0.001f);
    assertEquals(0f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldWalkDownOnS() {
    assertTrue(input.keyDown(Keys.S));
    assertEquals(0f, lastWalkDirection.x, 0.001f);
    assertEquals(-1f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldWalkDiagonallyWhenTwoKeysHeld() {
    input.keyDown(Keys.W);
    input.keyDown(Keys.D);
    assertEquals(1f, lastWalkDirection.x, 0.001f);
    assertEquals(1f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldTriggerAttackOnJ() {
    assertTrue(input.keyDown(Keys.J));
    assertEquals(1, attackCount);
  }

  @Test
  void shouldTriggerHeavyAttackNotSpecialAttackOnK() {
    assertTrue(input.keyDown(Keys.K));
    assertEquals(1, heavyAttackCount);
    assertEquals(0, specialAttackCount);
  }

  @Test
  void shouldDashOnSpace() {
    input.keyDown(Keys.A);
    assertTrue(input.keyDown(Keys.SPACE));
    assertEquals(1, dashCount);
    assertEquals(-1f, lastDashDirection.x, 0.001f);
  }

  @Test
  void shouldIgnoreUnboundKeys() {
    assertFalse(input.keyDown(Keys.R));
    assertFalse(input.keyUp(Keys.R));
    assertEquals(0, walkCount);
    assertEquals(0, attackCount);
  }

  @Test
  void tabCyclesConsumablesAndQUsesOnlyTheSelectedSlot() {
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    InventoryComponent inventory = new InventoryComponent(0);
    player.addComponent(inventory).addComponent(selection);
    for (String id :
        List.of(
            ItemIds.HEALTH_POTION,
            ItemIds.SHIELD,
            ItemIds.SPEED_POTION,
            ItemIds.STRENGTH_POTION,
            ItemIds.FREEZE_BOMB)) inventory.addConsumable(id);
    inventory.equipConsumable(ItemIds.FREEZE_BOMB, 3);
    selection.create();
    List<String> requested = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            ConsumableEffectComponent.USE_REQUEST, (EventListener1<String>) requested::add);

    assertTrue(input.keyDown(Keys.Q));
    assertTrue(input.keyDown(Keys.TAB));
    assertTrue(input.keyDown(Keys.Q));
    assertEquals(List.of(ItemIds.HEALTH_POTION, ItemIds.SHIELD), requested);
    assertEquals(1, selection.getSelectedIndex());
    for (int i = 0; i < 2; i++) {
      assertTrue(input.keyDown(Keys.TAB));
    }
    assertEquals(ItemIds.FREEZE_BOMB, selection.getSelectedType());
    assertTrue(input.keyDown(Keys.TAB));
    assertEquals(0, selection.getSelectedIndex());
  }

  @Test
  void lCastsTheAttunedAbility() {
    List<String> requests = new ArrayList<>();
    player
        .getEvents()
        .addListener(AbilityAttunementComponent.USE_ATTUNED_REQUEST, () -> requests.add("cast"));

    assertTrue(input.keyDown(Keys.L));
    assertTrue(input.keyDown(Keys.L));

    assertEquals(2, requests.size(), "L asks to cast every press");
  }

  @Test
  void lIsNotBoundToAnythingElse() {
    // L sits beside the J and K attacks; nothing else may claim it.
    assertFalse(input.keyUp(Keys.L));
  }
}
