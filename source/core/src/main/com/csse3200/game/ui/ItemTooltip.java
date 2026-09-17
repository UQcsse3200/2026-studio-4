package com.csse3200.game.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.csse3200.game.items.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the hover tooltip shown for an {@link Item} in the inventory.
 *
 * <p>A tooltip shows the item's name, a one line summary of its effect (omitted when the item has
 * none) and its description.
 *
 * <p>Two entry points are offered so callers can pick whichever suits their wiring. {@link
 * #forItem(Item, Skin)} returns a scene2d {@link Tooltip} to add straight onto the item's actor,
 * which gives hover detection, show delay and positioning near the cursor for free. {@link
 * #buildContent(Item, Skin)} returns just the contents, for callers that want to place the tooltip
 * themselves.
 */
public final class ItemTooltip {
  private static final float DESCRIPTION_WIDTH = 220f;
  private static final float CONTENT_PADDING = 8f;
  private static final float INITIAL_DELAY_SECONDS = 0.3f;
  private static final float SUBSEQUENT_DELAY_SECONDS = 0.1f;
  private static final float RESET_DELAY_SECONDS = 0.1f;

  /**
   * The lines of text shown in an item's tooltip, top to bottom: its name, its effect summary when
   * it has one, then its description. The last line is always the description.
   *
   * @param item item to describe
   * @return the tooltip's lines, never empty
   */
  public static List<String> lines(Item item) {
    List<String> lines = new ArrayList<>();
    lines.add(item.getName());
    if (!item.getEffectSummary().isEmpty()) {
      lines.add(item.getEffectSummary());
    }
    lines.add(item.getDescription());
    return lines;
  }

  /**
   * Builds the contents of an item's tooltip. The description is wrapped to a fixed width; the
   * lines above it are laid out one per row.
   *
   * @param item item to describe
   * @param skin skin used for the tooltip's labels and background
   * @return a table holding the item's tooltip lines
   */
  public static Table buildContent(Item item, Skin skin) {
    Table content = new Table(skin);
    content.pad(CONTENT_PADDING);

    List<String> lines = lines(item);
    int descriptionIndex = lines.size() - 1;

    for (int i = 0; i < lines.size(); i++) {
      Label label = new Label(lines.get(i), skin);

      if (i == 0) {
        label.setFontScale(2f);
      }

      if (i == descriptionIndex) {
        label.setWrap(true);
        content.add(label).width(DESCRIPTION_WIDTH).left().row();
      } else {
        content.add(label).left().row();
      }
    }
    return content;
  }

  /**
   * Builds a hover tooltip for an item. Add the result to the actor the item is drawn on, and
   * scene2d shows it while the pointer rests over that actor.
   *
   * @param item item to describe
   * @param skin skin used for the tooltip's labels and background
   * @return a tooltip listener for the item's actor
   */
  public static Tooltip<Table> forItem(Item item, Skin skin) {
    Tooltip<Table> tooltip = new Tooltip<>(buildContent(item, skin));
    tooltip.getContainer().background(skin.getDrawable("tooltip"));
    tooltip.getContainer().pad(4f);
    return tooltip;
  }

  /**
   * Applies the inventory's tooltip timing to scene2d's shared tooltip manager. Call once while
   * building the UI.
   */
  public static void configureManager() {
    TooltipManager manager = TooltipManager.getInstance();
    manager.initialTime = INITIAL_DELAY_SECONDS;
    manager.subsequentTime = SUBSEQUENT_DELAY_SECONDS;
    manager.resetTime = RESET_DELAY_SECONDS;
    manager.animations = false;
  }

  private ItemTooltip() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
