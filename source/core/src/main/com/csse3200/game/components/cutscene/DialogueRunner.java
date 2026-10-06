package com.csse3200.game.components.cutscene;

import com.csse3200.game.components.cutscene.DialogueScript.Line;
import com.csse3200.game.components.cutscene.DialogueScript.Speaker;

/**
 * Steps through a {@link DialogueScript} one line at a time with a typewriter reveal. Holds no UI
 * or libGDX state, so the flow (reveal, skip-to-end, next line, finish) is easy to test.
 */
public class DialogueRunner {
  /** Characters revealed per second. */
  public static final float CHARS_PER_SECOND = 45f;

  /** Notified as the dialogue progresses. */
  public interface Listener {
    void onLineShown(int index, Line line);

    void onFinished();
  }

  private final DialogueScript script;
  private final Listener listener;
  private int lineIndex = -1;
  private float revealedChars;
  private boolean finished;

  public DialogueRunner(DialogueScript script, Listener listener) {
    this.script = script;
    this.listener = listener;
  }

  /** Shows the first line. A script with no lines finishes immediately. */
  public void start() {
    if (lineIndex != -1 || finished) {
      return;
    }
    showLine(0);
  }

  /** Advances the typewriter reveal. */
  public void update(float deltaSeconds) {
    if (finished || lineIndex < 0 || isLineComplete()) {
      return;
    }
    revealedChars += CHARS_PER_SECOND * deltaSeconds;
  }

  /**
   * The player pressed the advance key: finishes revealing the current line, or if it is already
   * fully shown moves to the next line (closing the dialogue after the last one).
   */
  public void advance() {
    if (finished || lineIndex < 0) {
      return;
    }
    if (!isLineComplete()) {
      revealedChars = currentText().length();
      return;
    }
    showLine(lineIndex + 1);
  }

  /** Closes the dialogue immediately without showing the remaining lines. */
  public void skipAll() {
    finish();
  }

  /**
   * @return the part of the current line revealed so far
   */
  public String getVisibleText() {
    String text = currentText();
    return text.substring(0, Math.min(text.length(), (int) revealedChars));
  }

  /**
   * @return true once every character of the current line is showing
   */
  public boolean isLineComplete() {
    return revealedChars >= currentText().length();
  }

  public boolean isFinished() {
    return finished;
  }

  public int getLineIndex() {
    return lineIndex;
  }

  /**
   * @return the line being shown, or null before the start / after the end
   */
  public Line getCurrentLine() {
    return lineIndex >= 0 && lineIndex < script.lines.length ? script.lines[lineIndex] : null;
  }

  /**
   * @return who is speaking the current line, or null if unknown
   */
  public Speaker getCurrentSpeaker() {
    Line line = getCurrentLine();
    return line == null ? null : script.findSpeaker(line.speaker);
  }

  private String currentText() {
    Line line = getCurrentLine();
    return line == null || line.text == null ? "" : line.text;
  }

  private void showLine(int index) {
    if (index >= script.lines.length) {
      finish();
      return;
    }
    lineIndex = index;
    revealedChars = 0f;
    listener.onLineShown(index, script.lines[index]);
  }

  private void finish() {
    if (finished) {
      return;
    }
    finished = true;
    listener.onFinished();
  }
}
