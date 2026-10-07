package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class WinScreenRequestTest {
  @Test
  void winScreenEventCanBeReceived() {
    Entity player = new Entity();
    boolean[] requested = {false};
    player.getEvents().addListener("winScreenRequested", () -> requested[0] = true);

    player.getEvents().trigger("winScreenRequested");

    assertTrue(requested[0]);
  }
}
