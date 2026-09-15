package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class KeyboardPlayerInputComponentTest {
  private Entity player;
  private KeyboardPlayerInputComponent input;
  private InventoryComponent inventory;
  private int walkCount;
  private int walkStopCount;
  private int attackCount;
  private int specialAttackCount;
  private int dashCount;
  private Vector2 lastWalkDirection;
  private Vector2 lastDashDirection;
  private final List<ItemType> selectedEvents = new ArrayList<>();
  private final List<ItemType> usedEvents = new ArrayList<>();

  @BeforeEach
  void setUp() {
    player = new Entity();
    input = new KeyboardPlayerInputComponent();
    inventory = new InventoryComponent(0);
    player.addComponent(input);
    player.addComponent(inventory);
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
    player.getEvents().addListener("specialAttack", () -> specialAttackCount++);
    player
        .getEvents()
        .addListener(
            "dash",
            (Vector2 direction) -> {
              dashCount++;
              lastDashDirection = direction.cpy();
            });
        player.getEvents().addListener("consumableSelected", (ItemType type) -> selectedEvents.add(type));
    player.getEvents().addListener("itemUsed", (ItemType type) -> usedEvents.add(type));
  }

  @Test
  void shouldKeepRoomAndItemInteractionsSeparateOnEKey() {
    int[] roomInteractions = {0};
    int[] itemPickups = {0};
    player.getEvents().addListener("interact", () -> roomInteractions[0]++);
    player.getEvents().addListener("itemPickup", () -> itemPickups[0]++);

    assertTrue(input.keyDown(Keys.E));

    assertEquals(1, roomInteractions[0]);
    assertEquals(1, itemPickups[0]);
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
  void shouldTriggerSpecialAttackOnK() {
    assertTrue(input.keyDown(Keys.K));
    assertEquals(1, specialAttackCount);
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
    assertFalse(input.keyDown(Keys.Q));
    assertFalse(input.keyUp(Keys.Q));
    assertEquals(0, walkCount);
    assertEquals(0, attackCount);
  }

  @Test
  void shouldHaveNoSelectionByDefault() {
    assertNull(input.getSelectedConsumable());
  }

  @Test
  void shouldSelectEachConsumableOnNumberKeys() {
    assertTrue(input.keyDown(Keys.NUM_1));
    assertEquals(ItemType.HEALTH_POTION, input.getSelectedConsumable());
    assertTrue(input.keyDown(Keys.NUM_2));
    assertEquals(ItemType.SHIELD, input.getSelectedConsumable());
    assertTrue(input.keyDown(Keys.NUM_3));
    assertEquals(ItemType.SPEED_POTION, input.getSelectedConsumable());
    assertTrue(input.keyDown(Keys.NUM_4));
    assertEquals(ItemType.STRENGTH_POTION, input.getSelectedConsumable());
    assertEquals(4, selectedEvents.size());
  }

  @Test
  void shouldDoNothingOnUseWhenNothingSelected() {
    assertTrue(input.keyDown(Keys.U));
    assertTrue(usedEvents.isEmpty());
  }

  @Test
  void shouldDoNothingOnUseWhenSelectedConsumableNotInInventory() {
    input.keyDown(Keys.NUM_1);
    assertTrue(input.keyDown(Keys.U));
    assertTrue(usedEvents.isEmpty());
    assertEquals(0, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void shouldFireItemUsedAndDecrementInventoryWhenAvailable() {
    inventory.addConsumable(ItemType.HEALTH_POTION, 2);
    input.keyDown(Keys.NUM_1);
    assertTrue(input.keyDown(Keys.U));
    assertEquals(1, usedEvents.size());
    assertEquals(ItemType.HEALTH_POTION, usedEvents.get(0));
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void shouldNotConsumeBelowZeroOnRepeatedUse() {
    inventory.addConsumable(ItemType.SHIELD, 1);
    input.keyDown(Keys.NUM_2);
    input.keyDown(Keys.U);
    input.keyDown(Keys.U);
    assertEquals(1, usedEvents.size());
    assertEquals(0, inventory.getConsumableCount(ItemType.SHIELD));
  }
}