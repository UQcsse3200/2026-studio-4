package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.PlayerAbility;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws Hecate's menu. It owns no state: every frame it shows what {@link AbilityMenu} says, so the
 * choosing can be tested without a GL context and this only has to be looked at.
 */
public class AbilityMenuDisplay extends UIComponent {
  /** Above the inventory book (2), since the menu is modal over everything else. */
  private static final float Z_INDEX = 3f;

  /**
   * The panel is the skin's cream "window", so every colour here is a dark ink meant to be read on
   * a light background. The highlight is the skin's own {@code highlight}, which the rest of the UI
   * uses for a chosen thing.
   */
  private static final Color HIGHLIGHT = Color.valueOf("ffe6a3");

  /** Skin {@code brown}: near-black, so a selected row stays legible on the pale highlight. */
  private static final Color SELECTED_TEXT = Color.valueOf("2e1503");

  private static final Color UNSELECTED_TEXT = Color.valueOf("6a5440");
  private static final Color BODY_TEXT = Color.valueOf("5c4633");

  /** Skin {@code pressed}, darkened a little so it reads on the highlight too. */
  private static final Color CARRIED_TEXT = Color.valueOf("9c6b18");

  /** Skin style for the small supporting text under and beside each ability name. */
  private static final String CAPTION_STYLE = "caption";

  /** The bitmap font has no arrow glyphs, so name the letter keys instead. */
  private static final String HINT = "W/S choose     ENTER attune     ESC leave";

  /**
   * {@link PlayerAbility#getName()} returns an event id, which is not what a player should read.
   */
  private static final Map<String, String> TITLES =
      Map.of(Invisibility.NAME, "Invisibility", LastStand.NAME, "Last Stand");

  private static final Map<String, String> BLURBS =
      Map.of(
          Invisibility.NAME,
          "Step out of sight. Nothing hostile can see or touch you while it lasts.",
          LastStand.NAME,
          "At the edge of death, strike harder and move faster than you ever could whole.");

  private final AbilityMenu menu;
  private final List<Row> rows = new ArrayList<>();
  private Table root;
  private Table rowHolder;
  private int shownSelection = -1;

  /**
   * @param menu the state this panel shows
   */
  public AbilityMenuDisplay(AbilityMenu menu) {
    this.menu = menu;
  }

  @Override
  public void create() {
    super.create();
    root = new Table();
    root.setName("ability-menu");
    root.setFillParent(true);
    root.center();

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("window"));
    panel.pad(28f, 36f, 28f, 36f);

    Label title = new Label("Hecate's Gifts", skin, "title");
    title.setAlignment(Align.center);
    // The title style's gold is pale against the cream panel; multiply it down to a deeper amber.
    title.setColor(0.75f, 0.6f, 0.35f, 1f);
    panel.add(title).padBottom(4f).row();

    Label subtitle = new Label("You may carry only one.", skin, CAPTION_STYLE);
    subtitle.setAlignment(Align.center);
    subtitle.setColor(BODY_TEXT);
    panel.add(subtitle).padBottom(18f).row();

    rowHolder = new Table();
    rowHolder.setName("ability-menu-rows");
    panel.add(rowHolder).growX().row();

    Label hint = new Label(HINT, skin, CAPTION_STYLE);
    hint.setAlignment(Align.center);
    hint.setColor(UNSELECTED_TEXT);
    panel.add(hint).padTop(18f);

    root.add(panel);
    root.setVisible(false);
    stage.addActor(root);

    entity.getEvents().addListener(AbilityMenu.MENU_OPENED, this::show);
    entity.getEvents().addListener(AbilityMenu.MENU_CLOSED, this::hide);
  }

  @Override
  public void update() {
    if (root != null && root.isVisible() && shownSelection != menu.getSelectedIndex()) {
      highlightSelected();
    }
  }

  private void show() {
    buildRows();
    highlightSelected();
    root.setVisible(true);
    root.toFront();
  }

  private void hide() {
    root.setVisible(false);
  }

  /** Rebuilt on every open, because which ability is carried changes between visits. */
  private void buildRows() {
    rowHolder.clear();
    rows.clear();
    for (PlayerAbility ability : menu.getOptions()) {
      Row row = new Row(ability, menu.isAttuned(ability));
      rows.add(row);
      rowHolder.add(row.table).growX().padBottom(6f).row();
    }
    shownSelection = -1;
  }

  private void highlightSelected() {
    shownSelection = menu.getSelectedIndex();
    for (int i = 0; i < rows.size(); i++) {
      rows.get(i).setSelected(i == shownSelection);
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

  private static String titleOf(PlayerAbility ability) {
    String name = ability.getName();
    String known = TITLES.get(name);
    if (known != null) {
      return known;
    }
    return name.isEmpty()
        ? name
        : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
  }

  /** One selectable ability. */
  private final class Row {
    private final Table table = new Table();
    private final Label name;

    private Row(PlayerAbility ability, boolean carried) {
      table.pad(10f, 14f, 10f, 14f);

      name = new Label(titleOf(ability), skin, "default");
      table.add(name).left().expandX();

      if (carried) {
        Label carriedLabel = new Label("carried", skin, CAPTION_STYLE);
        carriedLabel.setColor(CARRIED_TEXT);
        table.add(carriedLabel).right();
      } else {
        table.add(new Label("", skin, CAPTION_STYLE)).right();
      }
      table.row();

      Label blurb = new Label(BLURBS.getOrDefault(ability.getName(), ""), skin, CAPTION_STYLE);
      blurb.setColor(BODY_TEXT);
      blurb.setWrap(true);
      table.add(blurb).colspan(2).left().width(420f).padTop(4f).row();

      Label cooldown =
          new Label("Cooldown " + (ability.getCooldown() / 1000) + "s", skin, CAPTION_STYLE);
      cooldown.setColor(BODY_TEXT);
      table.add(cooldown).colspan(2).left().padTop(2f);
    }

    private void setSelected(boolean selected) {
      table.setBackground(selected ? skin.newDrawable("white", HIGHLIGHT) : null);
      name.setColor(selected ? SELECTED_TEXT : UNSELECTED_TEXT);
      name.setFontScale(selected ? 1.05f : 1f);
    }
  }
}
