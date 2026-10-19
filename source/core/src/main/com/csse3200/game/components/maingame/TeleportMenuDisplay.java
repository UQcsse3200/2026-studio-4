package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.entities.configs.InteractableNpcConfig.TeleportOffer;
import com.csse3200.game.ui.UIComponent;

/**
 * Draws the question an NPC puts to the player when offering to teleport them. It owns no state:
 * every frame it shows what {@link TeleportMenu} says.
 */
public class TeleportMenuDisplay extends UIComponent {
  /** Above the inventory book (2) and level with Hecate's menu, since it is modal too. */
  private static final float Z_INDEX = 3f;

  private static final Color HIGHLIGHT = Color.valueOf("ffe6a3");
  private static final Color SELECTED_TEXT = Color.valueOf("2e1503");
  private static final Color UNSELECTED_TEXT = Color.valueOf("6a5440");
  private static final Color BODY_TEXT = Color.valueOf("5c4633");
  private static final String CAPTION_STYLE = "caption";
  private static final String HINT = "W/S choose     ENTER confirm     ESC stay";
  private static final float ROW_WIDTH = 420f;

  private final TeleportMenu menu;
  private final Label[] rowLabels = new Label[2];
  private final Table[] rows = new Table[2];
  private Table root;
  private Label title;
  private Label question;
  private int shownSelection = -1;

  /**
   * @param menu the state this panel shows
   */
  public TeleportMenuDisplay(TeleportMenu menu) {
    this.menu = menu;
  }

  @Override
  public void create() {
    super.create();
    root = new Table();
    root.setName("teleport-menu");
    root.setFillParent(true);
    root.center();

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("window"));
    panel.pad(28f, 36f, 28f, 36f);

    title = new Label("", skin, "title");
    title.setAlignment(Align.center);
    // The title style's gold is pale against the cream panel; multiply it to a deeper amber.
    title.setColor(0.75f, 0.6f, 0.35f, 1f);
    panel.add(title).padBottom(6f).row();

    question = new Label("", skin, CAPTION_STYLE);
    question.setAlignment(Align.center);
    question.setWrap(true);
    question.setColor(BODY_TEXT);
    panel.add(question).width(ROW_WIDTH).padBottom(18f).row();

    for (int i = 0; i < rows.length; i++) {
      rowLabels[i] = new Label("", skin, "default");
      rows[i] = new Table();
      rows[i].pad(10f, 14f, 10f, 14f);
      rows[i].add(rowLabels[i]).left().expandX();
      panel.add(rows[i]).width(ROW_WIDTH).padBottom(6f).row();
    }

    Label hint = new Label(HINT, skin, CAPTION_STYLE);
    hint.setAlignment(Align.center);
    hint.setColor(UNSELECTED_TEXT);
    panel.add(hint).padTop(18f);

    root.add(panel);
    root.setVisible(false);
    stage.addActor(root);

    entity.getEvents().addListener(TeleportMenu.MENU_OPENED, this::show);
    entity.getEvents().addListener(TeleportMenu.MENU_CLOSED, this::hide);
  }

  @Override
  public void update() {
    if (root != null && root.isVisible() && shownSelection != menu.getSelectedIndex()) {
      highlightSelected();
    }
  }

  private void show() {
    TeleportOffer offer = menu.getOffer();
    title.setText(offer.title == null ? "" : offer.title);
    question.setText(offer.prompt == null ? "" : offer.prompt);
    rowLabels[TeleportMenu.CONFIRM].setText(offer.confirmLabel);
    rowLabels[TeleportMenu.DECLINE].setText(offer.declineLabel);
    highlightSelected();
    root.setVisible(true);
    root.toFront();
  }

  private void hide() {
    root.setVisible(false);
  }

  private void highlightSelected() {
    shownSelection = menu.getSelectedIndex();
    for (int i = 0; i < rows.length; i++) {
      boolean selected = i == shownSelection;
      rows[i].setBackground(selected ? skin.newDrawable("white", HIGHLIGHT) : null);
      rowLabels[i].setColor(selected ? SELECTED_TEXT : UNSELECTED_TEXT);
      rowLabels[i].setFontScale(selected ? 1.05f : 1f);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawn by the stage.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (root != null) {
      root.remove();
    }
  }
}
