package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.files.WindowSize.Preset;
import org.junit.jupiter.api.Test;

class WindowSizeTest {
  @Test
  void matchesAListedSize() {
    Preset preset = WindowSize.matching(1600, 900);

    assertEquals(1600, preset.width);
    assertEquals(900, preset.height);
    assertEquals("1600 x 900", preset.toString());
  }

  @Test
  void unknownSizeUsesTheDefaultWindow() {
    Preset preset = WindowSize.matching(100, 100);

    assertEquals(WindowSize.DEFAULT_WIDTH, preset.width);
    assertEquals(WindowSize.DEFAULT_HEIGHT, preset.height);
  }

  @Test
  void missingSizeFallsBackAndHugeSizeIsClamped() {
    assertEquals(WindowSize.DEFAULT_WIDTH, WindowSize.width(0));
    assertEquals(WindowSize.DEFAULT_HEIGHT, WindowSize.height(-1));
    assertEquals(3840, WindowSize.width(9000));
    assertEquals(540, WindowSize.height(100));
    assertEquals(960, WindowSize.width(960));
    assertEquals(540, WindowSize.height(540));
  }
}
