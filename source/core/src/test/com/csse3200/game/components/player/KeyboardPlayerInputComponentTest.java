package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class KeyboardPlayerInputComponentTest {
  @Test
  void shouldKeepRoomAndItemInteractionsSeparateOnEKey() {
    Entity player = new Entity();
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    player.addComponent(input);
    int[] roomInteractions = {0};
    int[] itemPickups = {0};
    player.getEvents().addListener("interact", () -> roomInteractions[0]++);
    player.getEvents().addListener("itemPickup", () -> itemPickups[0]++);

    assertTrue(input.keyDown(Keys.E));

    assertEquals(1, roomInteractions[0]);
    assertEquals(1, itemPickups[0]);
  }
}
