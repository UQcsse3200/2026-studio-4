package com.csse3200.game.components.cutscene;

/** What the narrative manager needs from whatever plays animation cutscenes on screen. */
public interface CutsceneView {
  /**
   * Starts the cutscene. {@code onFinished} must be called exactly once, whether the cutscene ran
   * to the end, was stopped, or could not be played at all.
   */
  void play(CutsceneScript script, Runnable onFinished);

  /** Ends the running cutscene early, hiding the video. Calls onFinished. */
  void stop();

  boolean isActive();
}
