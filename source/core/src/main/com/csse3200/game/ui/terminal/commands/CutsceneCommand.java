package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.cutscene.CutsceneScript;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QA entry point for the video cutscenes: {@code cutscene <file>} plays {@code
 * configs/cutscenes/<file>.json}, e.g. {@code cutscene demovideo}. The terminal stays open when the
 * file cannot be found.
 */
public class CutsceneCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CutsceneCommand.class);
  private final Entity player;

  /** Binds the command to the player whose event handler runs the cutscene. */
  public CutsceneCommand(Entity player) {
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (player == null || args == null || args.size() != 1) {
      return false;
    }
    CutsceneScript script = CutsceneScript.load(args.get(0));
    if (script == null) {
      logger.warn("No cutscene file named '{}' in configs/cutscenes/", args.get(0));
      return false;
    }
    CutsceneEvents.playCutscene(player, script);
    return true;
  }
}
