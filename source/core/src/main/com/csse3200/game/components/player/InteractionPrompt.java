package com.csse3200.game.components.player;

import com.csse3200.game.components.rooms.configs.ExitConfig;

/**
 * Copy for in-range interaction prompts. The HUD displays these strings; it does not handle the E
 * key.
 */
public final class InteractionPrompt {
  public static final String NEXT_ROOM = "Press E for Next room";
  public static final String ENTER_DUNGEON = "Press E to Enter dungeon";
  public static final String CLEAR_REQUIRED = "Defeat all enemies first.";
  public static final String DUNGEON_UNAVAILABLE = "This dungeon is not available yet.";
  public static final String DUNGEON_COMPLETED = "Dungeon completed.";

  private InteractionPrompt() {}

  /**
   * Prompt while standing in range of a pick-upable item.
   *
   * @param itemName display name of the item
   * @return prompt text, or null if there is no item
   */
  public static String forItem(String itemName) {
    if (itemName == null || itemName.isBlank()) {
      return null;
    }
    return "Press E — Pick up " + itemName;
  }

  /**
   * Prompt while standing in range of a room exit. Blocked exits use the same channel.
   *
   * @param exit nearest exit, or null if none in range
   * @param roomCleared whether the current room has no remaining enemies
   * @param dungeonCompleted whether the destination dungeon is already completed
   * @return prompt text, or null if there is no exit in range
   */
  public static String forExit(ExitConfig exit, boolean roomCleared, boolean dungeonCompleted) {
    if (exit == null) {
      return null;
    }
    if (!exit.available) {
      return exit.message == null || exit.message.isBlank() ? DUNGEON_UNAVAILABLE : exit.message;
    }
    if (dungeonCompleted) {
      return DUNGEON_COMPLETED;
    }
    if (exit.requiresClear && !roomCleared) {
      return CLEAR_REQUIRED;
    }
    if ("BOOKSHELF".equals(exit.kind)) {
      return ENTER_DUNGEON;
    }
    return NEXT_ROOM;
  }

  /**
   * Prompt while standing in range of friendly NPC that can be interacted with.
   *
   * @param verb
   * @param npcName
   * @return
   */
  public static String forNpc(String verb, String npcName) {
    if (npcName == null || npcName.isBlank()) {
      return null;
    }
    String action = verb == null || verb.isBlank() ? "Talk to" : verb;
    return "Press E — " + action + " " + npcName;
  }

  /**
   * NPC prompts take priority over items and exits when handling interactions
   *
   * @param npcPrompt
   * @param itemPrompt
   * @param exitPrompt
   * @return
   */
  public static String resolve(String npcPrompt, String itemPrompt, String exitPrompt) {
    if (npcPrompt != null) {
      return npcPrompt;
    }
    return resolve(itemPrompt, exitPrompt);
  }

  /**
   * Item prompts take priority when the player is in range of both an item and an exit.
   *
   * @param itemPrompt prompt from a nearby item, or null
   * @param exitPrompt prompt from a nearby exit, or null
   * @return the text the HUD should show, or empty if nothing is in range
   */
  public static String resolve(String itemPrompt, String exitPrompt) {
    if (itemPrompt != null) {
      return itemPrompt;
    }
    if (exitPrompt != null) {
      return exitPrompt;
    }
    return "";
  }
}