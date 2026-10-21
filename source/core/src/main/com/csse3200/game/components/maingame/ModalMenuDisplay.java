package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;

/**
 * The look shared by the game's modal menus: a cream panel centred on screen with a title, some
 * rows and a key hint, and a highlighted row. It owns no state: every frame it shows what the menu
 * says, so the choosing can be tested without a GL context and this only has to be looked at.
 *
 * <p>A subclass says what goes inside the panel, what opening it shows, and how a row looks when it
 * is highlighted. Opening and closing are announced on the entity by the menu itself.
 */
public abstract class ModalMenuDisplay extends UIComponent {
  /** Above the inventory book (2), since a menu is modal over everything else. */
  private static final float Z_INDEX = 3f;

  /**
   * The panel is the skin's cream "window", so every colour here is a dark ink meant to be read on
   * a light background. The highlight is the skin's own {@code highlight}, which the rest of the UI
   * uses for a chosen thing.
   */
  private static final Color HIGHLIGHT = Color.valueOf("ffe6a3");

  /** Skin {@code brown}: near-black, so a selected row stays legible on the pale highlight. */
  private static final Color SELECTED_TEXT = Color.valueOf("2e1503");

  /** Ink for rows that are not highlighted, and for the key hint. */
  protected static final Color UNSELECTED_TEXT = Color.valueOf("6a5440");

  /** Ink for supporting text under a title. */
  protected static final Color BODY_TEXT = Color.valueOf("5c4633");

  /** Skin style for the small supporting text under and beside each row. */
  protected static final String CAPTION_STYLE = "caption";

  private final String rootName;
  private final String openedEvent;
  private final String closedEvent;
  private Table root;
  private int shownSelection = -1;

  /**
   * @param rootName name of the actor the panel is under, so it can be found on the stage
   * @param openedEvent event the menu triggers on this entity when it opens
   * @param closedEvent event the menu triggers on this entity when it closes
   */
  protected ModalMenuDisplay(String rootName, String openedEvent, String closedEvent) {
    this.rootName = rootName;
    this.openedEvent = openedEvent;
    this.closedEvent = closedEvent;
  }

  @Override
  public void create() {
    super.create();
    root = new Table();
    root.setName(rootName);
    root.setFillParent(true);
    root.center();

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("window"));
    panel.pad(28f, 36f, 28f, 36f);
    buildPanel(panel);

    Label hint = new Label(hintText(), skin, CAPTION_STYLE);
    hint.setAlignment(Align.center);
    hint.setColor(UNSELECTED_TEXT);
    panel.add(hint).padTop(18f);

    root.add(panel);
    root.setVisible(false);
    stage.addActor(root);

    entity.getEvents().addListener(openedEvent, this::show);
    entity.getEvents().addListener(closedEvent, this::hide);
  }

  /** Adds the title, any supporting text and the rows. The key hint is added after. */
  protected abstract void buildPanel(Table panel);

  /**
   * @return the line of key hints along the bottom of the panel
   */
  protected abstract String hintText();

  /** Fills in what the menu is currently offering. Called each time it opens. */
  protected abstract void onOpened();

  /**
   * @return the row the menu has highlighted
   */
  protected abstract int selectedIndex();

  /** Draws the highlight on one row and takes it off the others. */
  protected abstract void highlight(int selected);

  /** Adds a centred title to the panel. */
  protected final Label addTitle(Table panel, String text) {
    Label title = new Label(text, skin, "title");
    title.setAlignment(Align.center);
    // The title style's gold is pale against the cream panel; multiply it down to a deeper amber.
    title.setColor(0.75f, 0.6f, 0.35f, 1f);
    panel.add(title).padBottom(4f).row();
    return title;
  }

  /** Adds a centred line of supporting text under the title. */
  protected final Cell<Label> addCaption(Table panel, String text) {
    Label caption = new Label(text, skin, CAPTION_STYLE);
    caption.setAlignment(Align.center);
    caption.setColor(BODY_TEXT);
    return panel.add(caption);
  }

  /** Styles a row for being highlighted or not. */
  protected final void styleRow(Table row, Label name, boolean selected) {
    row.setBackground(selected ? skin.newDrawable("white", HIGHLIGHT) : null);
    name.setColor(selected ? SELECTED_TEXT : UNSELECTED_TEXT);
    name.setFontScale(selected ? 1.05f : 1f);
  }

  @Override
  public void update() {
    if (root != null && root.isVisible() && shownSelection != selectedIndex()) {
      applyHighlight();
    }
  }

  private void applyHighlight() {
    shownSelection = selectedIndex();
    highlight(shownSelection);
  }

  private void show() {
    onOpened();
    applyHighlight();
    root.setVisible(true);
    root.toFront();
  }

  private void hide() {
    root.setVisible(false);
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
