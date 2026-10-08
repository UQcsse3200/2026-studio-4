package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumableCommandTest {
  private InventoryComponent inventory;
  private Entity player;
  private ConsumableCommand command;

  @BeforeEach
  void setUp() {
    inventory = new InventoryComponent(0);
    player = new Entity().addComponent(inventory);
    command = new ConsumableCommand(player);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  @Test
  void defaultsToOneForEveryItem() {
    String[][] items = {
      {"small", ItemIds.HEALTH_POTION},
      {"medium", ItemIds.MEDIUM_HEALTH_POTION},
      {"large", ItemIds.LARGE_HEALTH_POTION},
      {"shield", ItemIds.SHIELD},
      {"speed", ItemIds.SPEED_POTION},
      {"strength", ItemIds.STRENGTH_POTION},
      {"freeze", ItemIds.FREEZE_BOMB},
      {"burn", ItemIds.BURN_VIAL}
    };
    for (String[] item : items) {
      assertTrue(command.action(args(item[0])));
      assertEquals(1, inventory.getConsumableCount(item[1]));
    }
    assertEquals(ItemIds.HEALTH_POTION, inventory.getConsumableSlot(0));
    assertEquals(ItemIds.SHIELD, inventory.getConsumableSlot(3));
  }

  @Test
  void genericCommandGrantsMagnetWithTheSameQuantityCapWithoutUsingIt() {
    AtomicInteger uses = new AtomicInteger();
    player.getEvents().addListener("useConsumable", (String id) -> uses.incrementAndGet());
    assertTrue(command.action(args("magnet", "999999999999999999999")));
    assertEquals(25, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    assertEquals(ItemIds.MAGNET_POTION, inventory.getConsumableSlot(0));
    assertEquals(0, uses.get());
  }

  @Test
  void magnetAliasSharesCappedGrantAndRetainsDefaultQuantity() {
    MagnetCommand alias = new MagnetCommand(player);
    assertTrue(alias.action(args()));
    assertEquals(1, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    assertTrue(alias.action(args("100")));
    assertEquals(26, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    assertFalse(alias.action(args("0")));
    assertFalse(alias.action(args("bad")));
    assertFalse(alias.action(args("1", "extra")));
    assertEquals(26, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
  }

  @Test
  void grantsRequestedQuantityWithPerCommandCap() {
    String[] inputs = {"1", "10", "25", "26", "100", "999999999999999999999999", "00010"};
    int[] amounts = {1, 10, 25, 25, 25, 25, 10};
    inventory.addConsumable(ItemIds.SPEED_POTION, 3);
    AtomicInteger reportedCount = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "consumableInventoryChanged", (String id, Integer count) -> reportedCount.set(count));
    int expected = 3;
    for (int i = 0; i < inputs.length; i++) {
      assertTrue(command.action(args("speed", inputs[i])));
      expected += amounts[i];
      assertEquals(expected, inventory.getConsumableCount(ItemIds.SPEED_POTION));
      assertEquals(expected, reportedCount.get());
    }
  }

  @Test
  void rejectsInvalidQuantityWithoutAddingAnything() {
    for (String value : new String[] {"0", "-1", "1.5", "abc", "+2", ""}) {
      assertFalse(command.action(args("speed", value)));
      assertEquals(0, inventory.getConsumableCount(ItemIds.SPEED_POTION));
    }
  }

  @Test
  void acceptsHealingTierSyntax() {
    assertTrue(command.action(args("heal", "medium", "7")));
    assertEquals(7, inventory.getConsumableCount(ItemIds.MEDIUM_HEALTH_POTION));
  }

  @Test
  void rejectsMissingUnknownAndExtraArguments() {
    assertFalse(command.action(args()));
    assertFalse(command.action(args("unknown")));
    assertFalse(command.action(args("heal")));
    assertFalse(command.action(args("heal", "speed")));
    assertFalse(command.action(args("speed", "2", "extra")));
    assertTrue(inventory.getConsumableIds().isEmpty());
  }

  @Test
  void preservesOtherTerminalCommandsAndDoesNotRequestUse() {
    Terminal terminal = new Terminal();
    AtomicInteger oldCommandCalls = new AtomicInteger();
    AtomicInteger useRequests = new AtomicInteger();
    player.getEvents().addListener("useConsumable", (String id) -> useRequests.incrementAndGet());
    terminal.addCommand(
        "burnvial",
        values -> {
          oldCommandCalls.incrementAndGet();
          return true;
        });
    terminal.addCommand("con", command);
    terminal.setEnteredMessage("con burn 26");
    assertTrue(terminal.processMessage());
    assertEquals(25, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    assertEquals(0, useRequests.get());
    terminal.setEnteredMessage("burnvial give");
    assertTrue(terminal.processMessage());
    assertEquals(1, oldCommandCalls.get());
  }

  @Test
  void failsWithoutInventory() {
    assertFalse(new ConsumableCommand(new Entity()).action(args("speed")));
  }
}
