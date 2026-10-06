package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.util.ArrayList;
import java.util.List;

/**
 * Word-wrapped text that is revealed a few characters at a time. Wrapping is decided from the
 * <i>full</i> text, so words never jump to the next line half way through being typed (which is
 * what a plain wrapped Label does).
 */
class TypewriterText extends Actor {
  private final BitmapFont font;
  private final Color textColor;
  private final GlyphLayout layout = new GlyphLayout();
  private final List<String> lines = new ArrayList<>();
  private String fullText = "";
  private int revealed;
  private float wrappedWidth = -1f;

  TypewriterText(BitmapFont font, Color color) {
    this.font = font;
    this.textColor = color;
  }

  /** Sets the whole line. Nothing is visible until {@link #setRevealed(int)} is called. */
  void setFullText(String text) {
    fullText = text == null ? "" : text;
    revealed = 0;
    wrappedWidth = -1f;
  }

  void setRevealed(int characters) {
    revealed = characters;
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    if (getWidth() != wrappedWidth) {
      wrap(getWidth());
    }
    font.setColor(textColor.r, textColor.g, textColor.b, textColor.a * parentAlpha);
    float lineHeight = font.getLineHeight();
    float y = getY() + getHeight();
    int remaining = revealed;
    for (String line : lines) {
      if (remaining <= 0) {
        break;
      }
      String shown = line.substring(0, Math.min(line.length(), remaining));
      font.draw(batch, shown, getX(), y);
      remaining -= line.length();
      y -= lineHeight;
    }
  }

  /** Splits the full text on spaces/newlines so that no line is wider than the actor. */
  private void wrap(float width) {
    wrappedWidth = width;
    lines.clear();
    // Line lengths include the single separator consumed at each break so reveal counts line up
    // with positions in fullText.
    int lineStart = 0;
    int lastBreak = -1;
    for (int i = 0; i <= fullText.length(); i++) {
      boolean end = i == fullText.length();
      char c = end ? ' ' : fullText.charAt(i);
      if (c == '\n' || end) {
        addLine(lineStart, i);
        lineStart = i + 1;
        lastBreak = -1;
        continue;
      }
      if (c == ' ') {
        lastBreak = i;
      }
      layout.setText(font, fullText.substring(lineStart, i + 1));
      if (layout.width > width && lastBreak >= lineStart) {
        addLine(lineStart, lastBreak);
        lineStart = lastBreak + 1;
        lastBreak = -1;
      }
    }
  }

  private void addLine(int from, int to) {
    // +1 accounts for the space or newline skipped at the break
    String text = fullText.substring(from, Math.min(to, fullText.length()));
    lines.add(to < fullText.length() ? text + " " : text);
  }
}
