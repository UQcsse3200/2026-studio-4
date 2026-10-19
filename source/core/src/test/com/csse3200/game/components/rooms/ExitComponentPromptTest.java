package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The door prompt follows the player after room updates moved off {@link RoomManager}. */
@ExtendWith(GameExtension.class)
class ExitComponentPromptTest {
  @Test
  void showsTheDoorPromptWhenThePlayerIsClose() {
    ExitComponent exits = exitsAt(1, 2, false);

    assertEquals(InteractionPrompt.NEXT_ROOM, exits.promptAt(new Vector2(1f, 2f), true));
  }

  @Test
  void hidesThePromptWhenThePlayerIsOutOfRange() {
    ExitComponent exits = exitsAt(1, 2, false);

    assertEquals("", exits.promptAt(new Vector2(4f, 2f), true));
  }

  @Test
  void asksForTheRoomToBeClearedWhenTheDoorRequiresIt() {
    ExitComponent exits = exitsAt(1, 2, true);

    assertEquals(InteractionPrompt.CLEAR_REQUIRED, exits.promptAt(new Vector2(1f, 2f), false));
  }

  private static ExitComponent exitsAt(int x, int y, boolean requiresClear) {
    ExitConfig door = new ExitConfig();
    door.x = x;
    door.y = y;
    door.kind = "DOOR";
    door.available = true;
    door.requiresClear = requiresClear;

    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.tileToWorldPosition(x, y)).thenReturn(new Vector2(x, y));

    ExitComponent exits = new ExitComponent(new ExitConfig[] {door});
    new Entity().addComponent(terrain).addComponent(exits);
    return exits;
  }
}
