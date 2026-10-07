package com.csse3200.game.entities.factories;

import com.csse3200.game.components.cutscene.CutscenePlayer;
import com.csse3200.game.components.cutscene.DialogueDisplay;
import com.csse3200.game.components.cutscene.NarrativeInputComponent;
import com.csse3200.game.components.cutscene.NarrativeManagerComponent;
import com.csse3200.game.entities.Entity;
import java.util.function.BooleanSupplier;

/**
 * Creates the entity that owns the dialogue and cutscene systems.
 *
 * <p>It is separate from the player because it must keep running while the game world is frozen for
 * a dialogue or cutscene, and the player must not.
 */
public class NarrativeFactory {
  /**
   * Creates the narrative entity. Register it with the entity service once the player exists.
   *
   * @param player the player entity; dialogue and cutscene events are sent on its event handler
   * @param terminalOpen whether the debug terminal is open; while it is, it gets all the input
   * @return the narrative entity
   */
  public static Entity createNarrative(Entity player, BooleanSupplier terminalOpen) {
    DialogueDisplay dialogueDisplay = new DialogueDisplay();
    CutscenePlayer cutscenePlayer = new CutscenePlayer();
    NarrativeManagerComponent manager =
        new NarrativeManagerComponent(player, dialogueDisplay, cutscenePlayer);

    Entity narrative =
        new Entity()
            .addComponent(dialogueDisplay)
            .addComponent(cutscenePlayer)
            .addComponent(manager)
            .addComponent(new NarrativeInputComponent(manager, terminalOpen));
    narrative.setUpdatesWhilePaused(true);
    return narrative;
  }

  private NarrativeFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
