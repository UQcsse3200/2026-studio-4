package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;

/**
 * Screen-space prompt for nearby interactions. Lives on the player so it survives room changes.
 * Top-right, white text on a dark panel so it stays clear of the hotbar and health HUD.
 */
public class InteractionPromptDisplay extends UIComponent {
  private Table table;
  private Table bubble;
  private Label promptLabel;
  private String currentText = "";

  @Override
  public void create() {
    super.create();
    table = new Table();
    table.setFillParent(true);
    table.align(Align.top | Align.right);
    table.padTop(50f).padRight(20f);

    promptLabel = new Label("", skin, "statDisplay");
    promptLabel.setColor(Color.WHITE);
    promptLabel.setFontScale(1f);

    bubble = new Table();
    bubble.setBackground(skin.getDrawable("tooltip"));
    bubble.pad(8f, 12f, 8f, 12f);
    bubble.add(promptLabel);
    bubble.setVisible(false);

    table.add(bubble);
    stage.addActor(table);
  }

  /**
   * Sets the prompt text. Unchanged text is ignored to avoid flicker.
   *
   * @param text prompt to show, or empty/null to hide
   */
  public void setPrompt(String text) {
    String next = text == null ? "" : text;
    if (next.equals(currentText)) {
      return;
    }
    currentText = next;
    promptLabel.setText(next);
    boolean show = !next.isEmpty();
    promptLabel.setVisible(show);
    bubble.setVisible(show);
  }

  /** Hides the prompt. */
  public void clearPrompt() {
    setPrompt("");
  }

  /**
   * @return text currently shown, or empty if hidden
   */
  public String getPrompt() {
    return currentText;
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void dispose() {
    super.dispose();
    if (table != null) {
      table.remove();
    }
  }
}
