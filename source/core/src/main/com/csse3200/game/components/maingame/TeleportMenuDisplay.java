package com.csse3200.game.components.maingame;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.entities.configs.InteractableNpcConfig.TeleportOffer;

/**
 * Draws the question an NPC puts to the player when offering to teleport them, from what {@link
 * TeleportMenu} says.
 */
public class TeleportMenuDisplay extends ModalMenuDisplay {
  private static final String HINT = "W/S choose     ENTER confirm     ESC stay";
  private static final float ROW_WIDTH = 420f;

  private final TeleportMenu menu;
  private final Label[] rowLabels = new Label[2];
  private final Table[] rows = new Table[2];
  private Label title;
  private Label question;

  /**
   * @param menu the state this panel shows
   */
  public TeleportMenuDisplay(TeleportMenu menu) {
    super("teleport-menu", TeleportMenu.MENU_OPENED, TeleportMenu.MENU_CLOSED);
    this.menu = menu;
  }

  @Override
  protected void buildPanel(Table panel) {
    title = addTitle(panel, "");
    question = addCaption(panel, "").width(ROW_WIDTH).padTop(2f).padBottom(18f).getActor();
    question.setWrap(true);
    panel.row();

    for (int i = 0; i < rows.length; i++) {
      rowLabels[i] = new Label("", skin, "default");
      rows[i] = new Table();
      rows[i].pad(10f, 14f, 10f, 14f);
      rows[i].add(rowLabels[i]).left().expandX();
      panel.add(rows[i]).width(ROW_WIDTH).padBottom(6f).row();
    }
  }

  @Override
  protected String hintText() {
    return HINT;
  }

  @Override
  protected void onOpened() {
    TeleportOffer offer = menu.getOffer();
    title.setText(offer.title == null ? "" : offer.title);
    question.setText(offer.prompt == null ? "" : offer.prompt);
    rowLabels[TeleportMenu.ACCEPT_ROW].setText(offer.confirmLabel);
    rowLabels[TeleportMenu.DECLINE_ROW].setText(offer.declineLabel);
  }

  @Override
  protected int selectedIndex() {
    return menu.getSelectedIndex();
  }

  @Override
  protected void highlight(int selected) {
    for (int i = 0; i < rows.length; i++) {
      styleRow(rows[i], rowLabels[i], i == selected);
    }
  }
}
