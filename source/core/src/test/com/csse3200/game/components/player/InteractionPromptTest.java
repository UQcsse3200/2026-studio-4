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

  private static ExitConfig door(boolean available, boolean requiresClear) {
    ExitConfig exit = new ExitConfig();
    exit.kind = "DOOR";
    exit.available = available;
    exit.requiresClear = requiresClear;
    return exit;
  }
}
