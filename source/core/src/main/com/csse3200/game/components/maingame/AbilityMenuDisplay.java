package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.player.PlayerAbility;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws Hecate's menu. It owns no state: every frame it shows what {@link AbilityMenu} says, so the
 * choosing can be tested without a GL context and this only has to be looked at.
 */
public class AbilityMenuDisplay extends ModalMenuDisplay {
  /** Skin {@code pressed}, darkened a little so it reads on the highlight too. */
  private static final Color CARRIED_TEXT = Color.valueOf("9c6b18");

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
  private Table rowHolder;

  /**
   * @param menu the state this panel shows
   */
  public AbilityMenuDisplay(AbilityMenu menu) {
    super("ability-menu", AbilityMenu.MENU_OPENED, AbilityMenu.MENU_CLOSED);
    this.menu = menu;
  }

  @Override
  protected void buildPanel(Table panel) {
    addTitle(panel, "Hecate's Gifts");
    addCaption(panel, "You may carry only one. Press L to use it.").padBottom(18f).row();

    rowHolder = new Table();
    rowHolder.setName("ability-menu-rows");
    panel.add(rowHolder).growX().row();
  }

  @Override
  protected String hintText() {
    return HINT;
  }

  /** Rebuilt on every open, because which ability is carried changes between visits. */
  @Override
  protected void onOpened() {
    rowHolder.clear();
    rows.clear();
    for (PlayerAbility ability : menu.getOptions()) {
      Row row = new Row(ability, menu.isAttuned(ability));
      rows.add(row);
      rowHolder.add(row.table).growX().padBottom(6f).row();
    }
  }

  @Override
  protected int selectedIndex() {
    return menu.getSelectedIndex();
  }

  @Override
  protected void highlight(int selected) {
    for (int i = 0; i < rows.size(); i++) {
      rows.get(i).setSelected(i == selected);
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
      styleRow(table, name, selected);
    }
  }
}
