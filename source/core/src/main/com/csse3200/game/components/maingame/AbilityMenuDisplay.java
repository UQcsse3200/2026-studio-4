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

  /** Matches the gold of the indicator that bobs over an NPC's head. */
  private static final Color HIGHLIGHT = new Color(1f, 0.85f, 0.3f, 0.25f);

  private static final Color SELECTED_TEXT = Color.WHITE;
  private static final Color UNSELECTED_TEXT = new Color(0.72f, 0.69f, 0.62f, 1f);
  private static final Color CARRIED_TEXT = new Color(1f, 0.85f, 0.3f, 1f);

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
    panel.add(title).padBottom(4f).row();

    Label subtitle = new Label("You may carry only one.", skin, "caption");
    subtitle.setAlignment(Align.center);
    panel.add(subtitle).padBottom(18f).row();

    rowHolder = new Table();
    rowHolder.setName("ability-menu-rows");
    panel.add(rowHolder).growX().row();

    Label hint = new Label("↑↓ choose     ENTER attune     ESC leave", skin, "caption");
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
        Label carriedLabel = new Label("carried", skin, "caption");
        carriedLabel.setColor(CARRIED_TEXT);
        table.add(carriedLabel).right();
      } else {
        table.add(new Label("", skin, "caption")).right();
      }
      table.row();

      Label blurb = new Label(BLURBS.getOrDefault(ability.getName(), ""), skin, "caption");
      blurb.setColor(UNSELECTED_TEXT);
      blurb.setWrap(true);
      table.add(blurb).colspan(2).left().width(420f).padTop(4f).row();

      Label cooldown =
          new Label("Cooldown " + (ability.getCooldown() / 1000) + "s", skin, "caption");
      cooldown.setColor(UNSELECTED_TEXT);
      table.add(cooldown).colspan(2).left().padTop(2f);
    }

    private void setSelected(boolean selected) {
      table.setBackground(selected ? skin.newDrawable("white", HIGHLIGHT) : null);
      name.setColor(selected ? SELECTED_TEXT : UNSELECTED_TEXT);
    }
  }
}
