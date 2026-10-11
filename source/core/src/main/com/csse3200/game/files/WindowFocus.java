package com.csse3200.game.files;

/** Whether the desktop window currently has focus. Defaults to focused. */
public final class WindowFocus {
  private static boolean focused = true;

  private WindowFocus() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static boolean focused() {
    return focused;
  }

  public static void setFocused(boolean value) {
    focused = value;
  }
}
