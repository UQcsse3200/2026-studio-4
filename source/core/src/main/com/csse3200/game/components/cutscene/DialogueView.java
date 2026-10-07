package com.csse3200.game.components.cutscene;

/** What the narrative manager needs from whatever shows dialogue on screen. */
public interface DialogueView {
  /** Opens the dialogue UI and starts running the script. */
  void show(DialogueScript script, DialogueRunner.Listener listener);

  /** The player pressed the advance key / clicked. */
  void advance();

  /** Ends the dialogue now, without showing the remaining lines. Reports finished as usual. */
  void skip();

  /** Hides the dialogue UI and releases its resources. */
  void close();

  boolean isActive();
}
