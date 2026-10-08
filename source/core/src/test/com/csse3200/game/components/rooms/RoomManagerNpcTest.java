package com.csse3200.game.components.rooms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** How {@link RoomManager} routes the E key to friendly NPCs before exits. */
@ExtendWith(GameExtension.class)
class RoomManagerNpcTest {
  private Entity player;
  private Entity room;
  private FriendlyNpcManagerComponent npcs;
  private NpcInteractableComponent npc;
  private GameAreaDisplay display;
  private RoomManager manager;

  @BeforeEach
  void setUp() {
    player = mock(Entity.class);
    when(player.getEvents()).thenReturn(new EventHandler());
    when(player.getCenterPosition()).thenReturn(new Vector2());
    room = mock(Entity.class);
    npcs = mock(FriendlyNpcManagerComponent.class);
    npc = mock(NpcInteractableComponent.class);
    display = mock(GameAreaDisplay.class);
    when(room.getComponent(FriendlyNpcManagerComponent.class)).thenReturn(npcs);
    when(room.getComponent(GameAreaDisplay.class)).thenReturn(display);
    when(npcs.findNearestInRange(player)).thenReturn(npc);

    manager = new RoomManager(player);
    manager.setCurrentRoom(room);
  }

  @Test
  void promptIsHiddenWhileAnythingHoldsThePlayersControls() {
    InteractionPromptDisplay prompt = mock(InteractionPromptDisplay.class);
    PlayerActions actions = mock(PlayerActions.class);
    when(player.getComponent(InteractionPromptDisplay.class)).thenReturn(prompt);
    when(player.getComponent(PlayerActions.class)).thenReturn(actions);
    // Hecate's menu, a boss sequence or an ending dialogue: all of them take a control lock.
    when(actions.areControlsLocked()).thenReturn(true);

    manager.refreshInteractionPrompt();

    verify(prompt).clearPrompt();
    verify(prompt, never()).setPrompt(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void npcInRangeConsumesTheKeyPress() {
    when(npc.interact(player)).thenReturn(true);

    // The test room has no exit config, so reaching the exit logic would throw.
    manager.interact();

    verify(npc).interact(player);
    verify(display, never()).showStatus(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void unavailableNpcExplainsWhy() {
    when(npc.interact(player)).thenReturn(false);
    when(npc.getPrompt(player)).thenReturn("The guardian will not stir.");

    manager.interact();

    verify(display).showStatus("The guardian will not stir.");
  }
}
