package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.cutscene.DialogueScript;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QA entry point for the dialogue system: {@code dialogue <file>} plays {@code
 * configs/dialogues/<file>.json}, e.g. {@code dialogue demo}. The terminal stays open when the file
 * cannot be found.
 */
public class DialogueCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DialogueCommand.class);
  private final Entity player;

  /** Binds the command to the player whose event handler runs the dialogue. */
  public DialogueCommand(Entity player) {
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (player == null || args == null || args.size() != 1) {
      return false;
    }
    DialogueScript script = DialogueScript.load(args.get(0));
    if (script == null || !script.hasLines()) {
      logger.warn("No playable dialogue file named '{}' in configs/dialogues/", args.get(0));
      return false;
    }
    CutsceneEvents.playDialogue(player, script);
    return true;
  }
}
