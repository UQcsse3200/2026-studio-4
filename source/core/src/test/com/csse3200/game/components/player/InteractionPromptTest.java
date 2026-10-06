package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InteractionPromptTest {
  @Test
  void shouldPromptNextRoomForAvailableDoor() {
    ExitConfig exit = door(true, false);
    assertEquals(InteractionPrompt.NEXT_ROOM, InteractionPrompt.forExit(exit, true, false));
  }

  @Test
  void shouldPromptEnterDungeonForBookshelf() {
    ExitConfig exit = new ExitConfig();
    exit.kind = "BOOKSHELF";
    exit.available = true;
    assertEquals(InteractionPrompt.ENTER_DUNGEON, InteractionPrompt.forExit(exit, true, false));
  }

  @Test
  void shouldShowClearRequiredWhenEnemiesRemain() {
    ExitConfig exit = door(true, true);
    assertEquals(InteractionPrompt.CLEAR_REQUIRED, InteractionPrompt.forExit(exit, false, false));
  }

  @Test
  void shouldShowUnavailableMessageForBlockedDungeon() {
    ExitConfig exit = new ExitConfig();
    exit.available = false;
    exit.message = "This dungeon is not available yet.";
    assertEquals(
        "This dungeon is not available yet.", InteractionPrompt.forExit(exit, false, false));
  }

  @Test
  void shouldDefaultUnavailableCopyWhenMessageMissing() {
    ExitConfig exit = new ExitConfig();
    exit.available = false;
    assertEquals(
        InteractionPrompt.DUNGEON_UNAVAILABLE, InteractionPrompt.forExit(exit, false, false));
  }

  @Test
  void shouldShowCompletedDungeonMessage() {
    ExitConfig exit = door(true, false);
    assertEquals(InteractionPrompt.DUNGEON_COMPLETED, InteractionPrompt.forExit(exit, true, true));
  }

  @Test
  void shouldReturnNullWhenNoExitInRange() {
    assertNull(InteractionPrompt.forExit(null, false, false));
  }

  @Test
  void shouldBuildItemPickupPrompt() {
    assertEquals("Press E — Pick up Strength Charm", InteractionPrompt.forItem("Strength Charm"));
    assertNull(InteractionPrompt.forItem(null));
    assertNull(InteractionPrompt.forItem("  "));
  }

  @Test
  void shouldPreferItemPromptOverExit() {
    assertEquals(
        "Press E — Pick up Charm",
        InteractionPrompt.resolve("Press E — Pick up Charm", InteractionPrompt.NEXT_ROOM));
    assertEquals(
        InteractionPrompt.NEXT_ROOM, InteractionPrompt.resolve(null, InteractionPrompt.NEXT_ROOM));
    assertEquals("", InteractionPrompt.resolve(null, null));
  }

  @Test
  void shouldPromptForNpcWithVerbAndName() {
    assertEquals("Press E — Talk to Old Sage", InteractionPrompt.forNpc("Talk to", "Old Sage"));
    assertEquals("Press E — Talk to Old Sage", InteractionPrompt.forNpc(null, "Old Sage"));
    assertEquals("Press E — Wake Guardian", InteractionPrompt.forNpc("Wake", "Guardian"));
    assertNull(InteractionPrompt.forNpc("Talk to", " "));
  }

  @Test
  void shouldPreferNpcPromptOverItemAndExit() {
    assertEquals(
        "Press E — Talk to Sage",
        InteractionPrompt.resolve(
            "Press E — Talk to Sage", "Press E — Pick up Charm", InteractionPrompt.NEXT_ROOM));
    assertEquals(
        "Press E — Pick up Charm",
        InteractionPrompt.resolve(null, "Press E — Pick up Charm", InteractionPrompt.NEXT_ROOM));
    assertEquals("", InteractionPrompt.resolve(null, null, null));
  }

  private static ExitConfig door(boolean available, boolean requiresClear) {
    ExitConfig exit = new ExitConfig();
    exit.kind = "DOOR";
    exit.available = available;
    exit.requiresClear = requiresClear;
    return exit;
  }
}
