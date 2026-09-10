package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Displays Team 5's always-visible Sprint 2 combat information.
 *
 * <p>This component is intentionally separate from the shared health HUD and the full inventory
 * screen. Consumable slots stay hidden while empty and show the item's image and quantity once the
 * player stores at least one.
 */
public class Team5CombatHudDisplay extends UIComponent {
  private static final String LABEL_STYLE = "statDisplay";

  /** Presentation-only slots for the four planned Sprint 2 consumables. */
  public enum ConsumableSlot {
    HEALTH(ItemType.HEALTH_POTION),
    SHIELD(ItemType.SHIELD),
    SPEED(ItemType.SPEED_POTION),
    STRENGTH(ItemType.STRENGTH_POTION);

    private final ItemType itemType;

    ConsumableSlot(ItemType itemType) {
      this.itemType = itemType;
    }
  }

  private final Map<ConsumableSlot, Label> quantityLabels = new EnumMap<>(ConsumableSlot.class);
  private final Map<ConsumableSlot, Table> slotTables = new EnumMap<>(ConsumableSlot.class);
  private Table table;
  private Label goldLabel;
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

    table.add(goldLabel).colspan(4).padBottom(6f);
    table.row();
    for (ConsumableSlot slot : ConsumableSlot.values()) {
      Table slotTable = createSlotTable(slot);
      slotTables.put(slot, slotTable);
      table.add(slotTable).padLeft(10f).padRight(10f);
    }

    stage.addActor(table);
  }

  private Table createSlotTable(ConsumableSlot slot) {
    Texture texture =
        ServiceLocator.getResourceService().getAsset(slot.itemType.getTexturePath(), Texture.class);
    Image icon = new Image(texture);
    Label count = new Label(formatQuantity(0), skin, LABEL_STYLE);
    Table slotTable = new Table();
    slotTable.add(icon).size(32f, 32f);
    slotTable.add(count).padLeft(4f);
    slotTable.setVisible(false);
    quantityLabels.put(slot, count);
    return slotTable;
  }

  /** Keeps the visible Gold value in sync with the existing inventory while an event is agreed. */
  @Override
  public void update() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory != null && inventory.getGold() != displayedGold) {
      updateGold(inventory.getGold());
    }
    if (inventory != null) {
      for (ConsumableSlot slot : ConsumableSlot.values()) {
        updateConsumableCount(slot, inventory.getConsumableCount(slot.itemType));
      }
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
    Table slotTable = slotTables.get(slot);
    if (label != null) {
      label.setText(formatQuantity(count));
    }
    if (slotTable != null) {
      slotTable.setVisible(count > 0);
    }
  }

  static String formatGold(int gold) {
    return String.format("Gold: %d", Math.max(gold, 0));
  }

  static String formatQuantity(int count) {
    return count > 0 ? String.format("×%d", count) : "";
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
