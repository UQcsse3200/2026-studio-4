package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UiScaleTest {
  @Test
  void keepsScaleInsideTheSliderRange() {
    assertEquals(1f, UiScale.clamp(Float.NaN));
    assertEquals(0.2f, UiScale.clamp(0f));
    assertEquals(1.5f, UiScale.clamp(1.5f));
    assertEquals(2f, UiScale.clamp(4f));
  }
}
