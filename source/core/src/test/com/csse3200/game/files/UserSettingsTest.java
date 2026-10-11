package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.files.UserSettings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class UserSettingsTest {
  @Test
  void shouldApplySettings() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode displayMode = mock(DisplayMode.class);
    when(Gdx.graphics.getDisplayMode()).thenReturn(displayMode);

    Settings settings = new Settings();
    settings.vsync = true;
    settings.displayMode = null;
    settings.fullscreen = true;
    settings.fps = 40;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setForegroundFPS(settings.fps);
    verify(Gdx.graphics).setFullscreenMode(displayMode);
    verify(Gdx.graphics).setVSync(settings.vsync);
  }

  @Test
  void usesSavedWindowSizeWhenNotFullscreen() {
    Gdx.graphics = mock(Graphics.class);
    Settings settings = new Settings();
    settings.fullscreen = false;
    settings.windowWidth = 1600;
    settings.windowHeight = 900;

    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setWindowedMode(1600, 900);
  }

  @Test
  void keepsDefaultWindowWhenSizeIsMissing() {
    Gdx.graphics = mock(Graphics.class);
    Settings settings = new Settings();
    settings.fullscreen = false;
    settings.windowWidth = 0;
    settings.windowHeight = 0;

    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setWindowedMode(WindowSize.DEFAULT_WIDTH, WindowSize.DEFAULT_HEIGHT);
  }

  @Test
  void shouldFindMatchingDisplay() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode correctMode = new CustomDisplayMode(200, 100, 60, 0);
    DisplayMode[] displayModes = {
      new CustomDisplayMode(100, 200, 30, 0), new CustomDisplayMode(100, 200, 60, 0), correctMode
    };
    when(Gdx.graphics.getDisplayModes()).thenReturn(displayModes);

    Settings settings = new Settings();
    settings.displayMode = new DisplaySettings();
    settings.displayMode.height = 100;
    settings.displayMode.width = 200;
    settings.displayMode.refreshRate = 60;
    settings.fullscreen = true;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setFullscreenMode(correctMode);
  }

  @Test
  void gameplayOptionsDefaultToVisible() {
    Settings settings = new Settings();
    assertTrue(settings.showTimer);
    assertFalse(settings.showFps);
    assertEquals(0.3f, settings.musicVolume);
    assertEquals(1f, settings.soundVolume);
    assertEquals(WindowSize.DEFAULT_WIDTH, settings.windowWidth);
    assertEquals(WindowSize.DEFAULT_HEIGHT, settings.windowHeight);
    assertFalse(settings.onlinePlay);
    assertEquals(PlayMode.DEFAULT_NAME, settings.displayName);
    assertFalse(settings.muteUnfocused);
  }

  @Test
  void shouldPersistGameplayOptionsWithoutApplyingGraphics() {
    Settings original = UserSettings.get();
    Settings updated = new Settings();
    updated.fps = original.fps;
    updated.fullscreen = original.fullscreen;
    updated.vsync = original.vsync;
    updated.uiScale = original.uiScale;
    updated.displayMode = original.displayMode;
    updated.showTimer = false;
    updated.showFps = true;
    updated.musicVolume = 0.4f;
    updated.soundVolume = 0.2f;
    updated.windowWidth = 1600;
    updated.windowHeight = 900;
    updated.onlinePlay = true;
    updated.displayName = "Ada";
    updated.muteUnfocused = true;

    try {
      UserSettings.set(updated, false);
      Settings loaded = UserSettings.get();
      assertFalse(loaded.showTimer);
      assertTrue(loaded.showFps);
      assertEquals(0.4f, loaded.musicVolume);
      assertEquals(0.2f, loaded.soundVolume);
      assertEquals(1600, loaded.windowWidth);
      assertEquals(900, loaded.windowHeight);
      assertTrue(loaded.onlinePlay);
      assertEquals("Ada", loaded.displayName);
      assertTrue(loaded.muteUnfocused);
    } finally {
      UserSettings.set(original, false);
    }
  }

  /** This exists to make the constructor public */
  static class CustomDisplayMode extends DisplayMode {
    public CustomDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
