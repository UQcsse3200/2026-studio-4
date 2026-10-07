package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Gdx;

/** Small helpers for turning a script name typed by a developer into a safe asset path. */
final class ScriptNames {
  /**
   * @param name script name, e.g. "demo" or "demo.json"
   * @return the name without a ".json" suffix, or null for a null/blank name
   */
  static String stripExtension(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    String trimmed = name.trim();
    return trimmed.endsWith(".json") ? trimmed.substring(0, trimmed.length() - 5) : trimmed;
  }

  /**
   * Script names are plain file names: letters, digits, '_', '-' and '.', no path separators.
   *
   * @param id script name without extension
   * @return true if it cannot escape the scripts directory
   */
  static boolean isSafe(String id) {
    return id.matches("[A-Za-z0-9_.-]+") && !id.contains("..");
  }

  static boolean exists(String path) {
    return Gdx.files != null && Gdx.files.internal(path).exists();
  }

  private ScriptNames() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
