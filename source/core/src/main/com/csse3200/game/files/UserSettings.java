package com.csse3200.game.files;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.csse3200.game.files.FileLoader.Location;
import java.io.File;

/** Reading, Writing, and applying user settings in the game. */
public class UserSettings {
  private static final String ROOT_DIR = "DECO2800Game";
  private static final String SETTINGS_FILE = "settings.json";

  /**
   * Get the stored user settings
   *
   * @return Copy of the current settings
   */
  public static Settings get() {
    String path = ROOT_DIR + File.separator + SETTINGS_FILE;
    Settings fileSettings = FileLoader.readClass(Settings.class, path, Location.EXTERNAL);
    // Use default values if file doesn't exist
    return fileSettings != null ? fileSettings : new Settings();
  }

  /**
   * Set the stored user settings
   *
   * @param settings New settings to store
   * @param applyImmediate true to immediately apply new settings.
   */
  public static void set(Settings settings, boolean applyImmediate) {
    String path = ROOT_DIR + File.separator + SETTINGS_FILE;
    FileLoader.writeClass(settings, path, Location.EXTERNAL);

    if (applyImmediate) {
      applySettings(settings);
    }
  }

  /**
   * Apply the given settings without storing them.
   *
   * @param settings Settings to apply
   */
  public static void applySettings(Settings settings) {
    Gdx.graphics.setForegroundFPS(settings.fps);
    Gdx.graphics.setVSync(settings.vsync);

    if (settings.fullscreen) {
      DisplayMode displayMode = findMatching(settings.displayMode);
      if (displayMode == null) {
        displayMode = Gdx.graphics.getDisplayMode();
      }
      Gdx.graphics.setFullscreenMode(displayMode);
    } else {
      Gdx.graphics.setWindowedMode(
          WindowSize.width(settings.windowWidth), WindowSize.height(settings.windowHeight));
    }
  }

  private static DisplayMode findMatching(DisplaySettings desiredSettings) {
    DisplayMode match = matchSize(desiredSettings, Gdx.graphics.getDisplayModes());
    if (match != null) {
      return match;
    }
    Monitor monitor = Gdx.graphics.getMonitor();
    if (monitor == null) {
      return null;
    }
    return matchSize(desiredSettings, Gdx.graphics.getDisplayModes(monitor));
  }

  /**
   * A display mode of the requested size. An exact refresh rate wins. Otherwise the first mode of
   * that size is used, so a 59 Hz panel still accepts a 60 Hz choice.
   *
   * @param desired width, height, and refresh rate chosen in the menu
   * @param modes modes to search; null is treated as none
   * @return a matching mode, or null when the size is absent
   */
  static DisplayMode matchSize(DisplaySettings desired, DisplayMode[] modes) {
    if (desired == null || modes == null) {
      return null;
    }
    DisplayMode sameSize = null;
    for (DisplayMode mode : modes) {
      if (mode == null || mode.width != desired.width || mode.height != desired.height) {
        continue;
      }
      if (mode.refreshRate == desired.refreshRate) {
        return mode;
      }
      if (sameSize == null) {
        sameSize = mode;
      }
    }
    return sameSize;
  }

  /** Stores game settings, can be serialised/deserialised. */
  public static class Settings {
    /** FPS cap of the game. Independant of screen FPS. */
    public int fps = 60;

    public boolean fullscreen = true;
    public boolean vsync = true;

    /** Scales the settings page immediately and the in-game HUD after Apply. */
    public float uiScale = 1f;

    /** When true, the in-run HUD shows the game timer. */
    public boolean showTimer = true;

    /** When true, a small FPS counter is drawn during a run. */
    public boolean showFps = false;

    /** Background music volume, from 0 to 1. */
    public float musicVolume = 0.3f;

    /** Effect volume, from 0 to 1. */
    public float soundVolume = 1f;

    /** Window size used when fullscreen is off. Zero means the default 1280 x 800. */
    public int windowWidth = WindowSize.DEFAULT_WIDTH;

    public int windowHeight = WindowSize.DEFAULT_HEIGHT;

    /** Saved preference. The game does not open a network connection from this flag. */
    public boolean onlinePlay = false;

    /** Name shown next to the online preference. */
    public String displayName = PlayMode.DEFAULT_NAME;

    /** When true, music and effects are silent while the window is in the background. */
    public boolean muteUnfocused = false;

    public DisplaySettings displayMode = null;
  }

  /** Stores chosen display settings. Can be serialised/deserialised. */
  public static class DisplaySettings {
    public int width;
    public int height;
    public int refreshRate;

    public DisplaySettings() {}

    public DisplaySettings(DisplayMode displayMode) {
      this.width = displayMode.width;
      this.height = displayMode.height;
      this.refreshRate = displayMode.refreshRate;
    }
  }

  private UserSettings() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
