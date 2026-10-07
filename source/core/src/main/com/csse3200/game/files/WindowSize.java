package com.csse3200.game.files;

/** Window sizes offered in the settings menu, and the clamp used when applying one. */
public final class WindowSize {
  public static final int DEFAULT_WIDTH = 1280;
  public static final int DEFAULT_HEIGHT = 800;

  private static final int MIN = 640;
  private static final int MAX = 3840;

  private static final Preset[] PRESETS = {
    new Preset("960 x 540", 960, 540),
    new Preset("1280 x 720", 1280, 720),
    new Preset("1280 x 800", DEFAULT_WIDTH, DEFAULT_HEIGHT),
    new Preset("1600 x 900", 1600, 900),
    new Preset("1920 x 1080", 1920, 1080)
  };

  private WindowSize() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static Preset[] presets() {
    return PRESETS.clone();
  }

  /** Preset that matches a saved size, or the default window when none matches. */
  public static Preset matching(int width, int height) {
    for (Preset preset : PRESETS) {
      if (preset.width == width && preset.height == height) {
        return preset;
      }
    }
    return PRESETS[2];
  }

  public static int width(int stored) {
    return clamp(stored, DEFAULT_WIDTH);
  }

  public static int height(int stored) {
    return clamp(stored, DEFAULT_HEIGHT);
  }

  private static int clamp(int value, int fallback) {
    if (value <= 0) {
      return fallback;
    }
    return Math.min(MAX, Math.max(MIN, value));
  }

  /** One labelled window size. The menu prints {@link #toString()}. */
  public static final class Preset {
    public final String label;
    public final int width;
    public final int height;

    private Preset(String label, int width, int height) {
      this.label = label;
      this.width = width;
      this.height = height;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
