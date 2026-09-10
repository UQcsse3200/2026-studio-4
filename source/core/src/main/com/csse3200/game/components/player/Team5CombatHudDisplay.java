package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Displays Team 5's always-visible Sprint 2 combat information.
 *
 * <p>This component is intentionally separate from the shared health HUD and the full inventory
 * screen. Until the inventory and input contracts are available, it provides the visual quick-bar
 * layout with zero quantities and no selected consumable.
 */
public class Team5CombatHudDisplay extends UIComponent {
  private static final String LABEL_STYLE = "statDisplay";

  /** Presentation-only slots for the four planned Sprint 2 consumables. */
  public enum ConsumableSlot {
    HEALTH("1", "Health"),
    SHIELD("2", "Shield"),
    SPEED("3", "Speed"),
    STRENGTH("4", "Strength");

    private final String key;
    private final String displayName;

    ConsumableSlot(String key, String displayName) {
      this.key = key;
      this.displayName = displayName;
    }
  }

  private final Map<ConsumableSlot, Label> quantityLabels =
      new EnumMap<>(ConsumableSlot.class);
  private Table table;
  private Label goldLabel;
  private Label selectedLabel;
  private int displayedGold;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    table = new Table();
    table.bottom();
    table.setFillParent(true);
    table.padBottom(20f);

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    displayedGold = inventory == null ? 0 : inventory.getGold();
    goldLabel = new Label(formatGold(displayedGold), skin, LABEL_STYLE);
    selectedLabel = new Label(formatSelected(null), skin, LABEL_STYLE);

    table.add(goldLabel).colspan(4).padBottom(6f);
    table.row();
    for (ConsumableSlot slot : ConsumableSlot.values()) {
      Label label = new Label(formatSlot(slot, 0), skin, LABEL_STYLE);
      quantityLabels.put(slot, label);
      table.add(label).padLeft(10f).padRight(10f);
    }
    table.row();
    table.add(selectedLabel).colspan(4).padTop(6f);

    stage.addActor(table);
  }

  /** Keeps the visible Gold value in sync with the existing inventory while an event is agreed. */
  @Override
  public void update() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory != null && inventory.getGold() != displayedGold) {
      updateGold(inventory.getGold());
    }
  }

  /** Updates the Gold value shown on the combat HUD. */
  public void updateGold(int gold) {
    displayedGold = Math.max(gold, 0);
    if (goldLabel != null) {
      goldLabel.setText(formatGold(displayedGold));
    }
  }

  /** Updates a presentation slot without defining the inventory's future item-type contract. */
  public void updateConsumableCount(ConsumableSlot slot, int count) {
    Label label = quantityLabels.get(slot);
    if (label != null) {
      label.setText(formatSlot(slot, count));
    }
  }

  /** Updates the selected consumable indicator. A null slot means that nothing is selected. */
  public void updateSelectedConsumable(ConsumableSlot slot) {
    if (selectedLabel != null) {
      selectedLabel.setText(formatSelected(slot));
    }
  }

  static String formatGold(int gold) {
    return String.format("Gold: %d", Math.max(gold, 0));
  }

  static String formatSlot(ConsumableSlot slot, int count) {
    return String.format("[%s] %s x%d", slot.key, slot.displayName, Math.max(count, 0));
  }

  static String formatSelected(ConsumableSlot slot) {
    return String.format("Selected: %s", slot == null ? "None" : slot.displayName);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Draw is handled by the stage.
  }

  @Override
  public void dispose() {
    super.dispose();
    if (table != null) {
      table.remove();
    }
  }
}
