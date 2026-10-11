package com.csse3200.game.files;

/** Display name and online preference saved from the settings menu. */
public final class PlayMode {
  public static final int NAME_LIMIT = 16;
  public static final String DEFAULT_NAME = "Player";

  private PlayMode() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** Blank names become Player. Extra spaces are collapsed, then the name is cut to 16 letters. */
  public static String cleanName(String raw) {
    if (raw == null) {
      return DEFAULT_NAME;
    }
    String trimmed = raw.trim().replaceAll("\\s+", " ");
    if (trimmed.isEmpty()) {
      return DEFAULT_NAME;
    }
    if (trimmed.length() > NAME_LIMIT) {
      return trimmed.substring(0, NAME_LIMIT);
    }
    return trimmed;
  }

  /** Status line for the settings page. Online is a saved preference, not a live match. */
  public static String summary(boolean online, String name) {
    if (!online) {
      return "Offline. This game stays on this computer.";
    }
    return "Online preference saved for " + cleanName(name) + ".";
  }
}
