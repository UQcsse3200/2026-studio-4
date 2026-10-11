package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.Graphics.DisplayMode;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.utils.StringDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SettingsMenuDisplayTest {
  @Test
  void keepsCurrentModeWhenNoResolutionIsSelected() {
    assertNull(SettingsMenuDisplay.chosenDisplaySettings(null));
    assertNull(SettingsMenuDisplay.chosenDisplaySettings(new StringDecorator<>(null, mode -> "")));
  }

  @Test
  void usesTheSelectedResolution() {
    DisplayMode mode = new CustomDisplayMode(1920, 1080, 60, 0);
    DisplaySettings chosen =
        SettingsMenuDisplay.chosenDisplaySettings(new StringDecorator<>(mode, unused -> "1920"));

    assertEquals(1920, chosen.width);
    assertEquals(1080, chosen.height);
    assertEquals(60, chosen.refreshRate);
  }

  /** DisplayMode's constructor is protected. */
  private static class CustomDisplayMode extends DisplayMode {
    private CustomDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
