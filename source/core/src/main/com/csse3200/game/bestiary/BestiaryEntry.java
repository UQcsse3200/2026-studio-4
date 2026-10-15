package com.csse3200.game.bestiary;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/**
 * One enemy's page in the bestiary.
 *
 * @param enemy the enemy type this entry describes, used as its unique key
 * @param name the name shown to the player
 * @param description a short description of the enemy
 * @param abilities what the enemy does in a fight
 * @param weakness a tip for beating it
 * @param texture path to the image shown once the entry is unlocked
 */
public record BestiaryEntry(
    EnemyType enemy,
    String name,
    String description,
    String abilities,
    String weakness,
    String texture) {

  /** Throws if any field is missing or blank. */
  public BestiaryEntry {
    if (enemy == null
        || isBlank(name)
        || isBlank(description)
        || isBlank(abilities)
        || isBlank(weakness)
        || isBlank(texture)) {
      throw new IllegalArgumentException("Bestiary entry is missing a field: " + enemy);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
