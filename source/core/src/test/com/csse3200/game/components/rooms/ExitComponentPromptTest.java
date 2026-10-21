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
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.services.ServiceLocator;
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

  @Test
  void asksForTheOtherDungeonsBeforeTheBossShelf() {
    ServiceLocator.registerRoomClearService(new RoomClearService());
    ExitComponent exits = bookshelf("dungeonFiveShelf");

    assertEquals(
        InteractionPrompt.CLEAR_ALL_DUNGEONS_REQUIRED, exits.promptAt(new Vector2(1f, 2f), true));
  }

  @Test
  void offersTheBossShelfOnceFourDungeonsAreCleared() {
    RoomClearService clears = new RoomClearService();
    clears.clearAllDungeons();
    ServiceLocator.registerRoomClearService(clears);
    ExitComponent exits = bookshelf("dungeonFiveShelf");

    assertEquals(InteractionPrompt.ENTER_DUNGEON, exits.promptAt(new Vector2(1f, 2f), true));
  }

  private static ExitComponent bookshelf(String id) {
    ExitConfig shelf = new ExitConfig();
    shelf.x = 1;
    shelf.y = 2;
    shelf.id = id;
    shelf.kind = "BOOKSHELF";
    shelf.available = true;

    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.tileToWorldPosition(1, 2)).thenReturn(new Vector2(1f, 2f));

    ExitComponent exits = new ExitComponent(new ExitConfig[] {shelf});
    new Entity().addComponent(terrain).addComponent(exits);
    return exits;
  }

  private static ExitComponent exitsAt(int x, int y, boolean requiresClear) {
    ExitConfig door = new ExitConfig();
    door.x = x;
    door.y = y;
    door.kind = "DOOR";
    door.available = true;
    door.requiresClear = requiresClear;

    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.tileToWorldPosition(x, y)).thenReturn(new Vector2((float) x, (float) y));

    ExitComponent exits = new ExitComponent(new ExitConfig[] {door});
    new Entity().addComponent(terrain).addComponent(exits);
    return exits;
  }
}
