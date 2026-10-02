package com.csse3200.game.entities.configs;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class InteractableNpcConfigs {
  public static final String CONFIG_PATH = "configs/friendlyNpcs.json";

  public InteractableNpcConfig[] npcs = new InteractableNpcConfig[0];

  /**
   * @param npcId stable NPC id
   * @return the matching definition, or null if none exists
   */
  public InteractableNpcConfig get(String npcId) {
    for (InteractableNpcConfig npc : npcs) {
      if (Objects.equals(npc.id, npcId)) {
        return npc;
      }
    }
    return null;
  }

  /**
   * Rejects config definitions that would cause problems in game before the NPC is created
   *
   * @throws IllegalArgumentException describing the first problem found
   */
  public void validate() {
    Set<String> ids = new HashSet<>();
    for (InteractableNpcConfig npc : npcs) {
      require(npc != null, "Null friendly NPC definition");
      require(npc.id != null && !npc.id.isBlank(), "Friendly NPC is missing an id");
      require(ids.add(npc.id), "Duplicate friendly NPC id: " + npc.id);
      require(npc.name != null && !npc.name.isBlank(), "Friendly NPC has no name: " + npc.id);
      require(
          npc.hasDialogue() || npc.hasCutscene(),
          "Friendly NPC needs a dialogueId or a cutsceneId: " + npc.id);
      require(npc.interactionRange > 0f, "Friendly NPC range must be positive: " + npc.id);
      require(npc.width > 0f, "Friendly NPC width must be positive: " + npc.id);
      require(npc.cutsceneTiming != null, "Friendly NPC has no cutsceneTiming: " + npc.id);
      require(
          npc.atlas == null || (npc.animation != null && !npc.animation.isBlank()),
          "Friendly NPC atlas needs an animation: " + npc.id);
      require(
          npc.atlas != null || npc.texture != null,
          "Friendly NPC needs an atlas or a texture: " + npc.id);
    }
    for (InteractableNpcConfig npc : npcs) {
      for (String required : npc.requiresCompleted) {
        require(
            ids.contains(required),
            "Friendly NPC " + npc.id + " requires unknown NPC: " + required);
        require(!npc.id.equals(required), "Friendly NPC requires itself: " + npc.id);
      }
    }
  }

  private void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
