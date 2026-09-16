package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Displays Team 5's always-visible Sprint 2 combat information.
 *
 * <p>This component is intentionally separate from the shared health HUD and the full inventory
 * screen. It reads Team 5's Gold and consumable state without modifying Team 2's HUD shell or
 * {@link PlayerStatsDisplay}.
 */
public class Team5CombatHudDisplay extends UIComponent {
  private static final String LABEL_STYLE = "statDisplay";

  /** Presentation-only slots for the four planned Sprint 2 consumables. */
  public enum ConsumableSlot {
    HEALTH("Health", ItemType.HEALTH_POTION),
    SHIELD("Shield", ItemType.SHIELD),
    SPEED("Speed", ItemType.SPEED_POTION),
    STRENGTH("Strength", ItemType.STRENGTH_POTION);

    private final String displayName;
    private final ItemType itemType;

    ConsumableSlot(String displayName, ItemType itemType) {
      this.displayName = displayName;
      this.itemType = itemType;
    }

    static ConsumableSlot fromItemType(ItemType itemType) {
      if (itemType == null) {
        return null;
      }

      for (ConsumableSlot slot : values()) {
        if (slot.itemType == itemType) {
          return slot;
        }
      }
      return null;
    }
  }

  private final Map<ConsumableSlot, Label> quantityLabels = new EnumMap<>(ConsumableSlot.class);
  private Table table;
  private Label goldLabel;
  private Label selectedLabel;
  private int displayedGold;
  private final Label[] quickLabels = new Label[3];
  private boolean disposed;

  @Override
  public void create() {
    super.create();
    addActors();
    registerEventListeners();
  }

  void registerEventListeners() {
    entity
        .getEvents()
        .addListener("consumableInventoryChanged", this::onConsumableInventoryChanged);
    entity
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, this::onSelectedConsumableChanged);
    entity.getEvents().addListener(ConsumableLoadoutComponent.CHANGED, this::refreshQuickSlots);
  }

  private void addActors() {
    table = new Table();
    table.top().right();
    table.setFillParent(true);
    table.padTop(20f).padRight(20f);
    table.setName("team5-consumables");

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    displayedGold = inventory == null ? 0 : inventory.getGold();
    goldLabel = new Label(formatGold(displayedGold), skin, LABEL_STYLE);
    selectedLabel = new Label(formatSelected(null), skin, LABEL_STYLE);

    table.add(goldLabel).colspan(2).padBottom(6f);
    table.row();
    for (ConsumableSlot slot : ConsumableSlot.values()) {
      int count = inventory == null ? 0 : inventory.getConsumableCount(slot.itemType);
      Label label = new Label(formatSlot(slot, count), skin, LABEL_STYLE);
      quantityLabels.put(slot, label);
      table.add(label).left().colspan(2);
      table.row();
    }
    table.row();
    table.add(selectedLabel).colspan(2).padTop(6f);
    table.row();
    table.add(new Label("Quick use (8 / 9 / 0)", skin, LABEL_STYLE)).colspan(2);
    for (int i = 0; i < 3; i++) {
      final int slot = i;
      table.row();
      quickLabels[i] = new Label("", skin, LABEL_STYLE);
      TextButton change = new TextButton("Change", skin);
      change.setName("consumable-change-" + i);
      change.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              ConsumableLoadoutComponent loadout =
                  entity.getComponent(ConsumableLoadoutComponent.class);
              if (loadout != null) {
                loadout.cycleSlot(slot);
              }
            }
          });
      table.add(quickLabels[i]).left().padRight(8f);
      table.add(change);
    }
    refreshQuickSlots();

    stage.addActor(table);
  }

  /** Keeps the visible Gold value in sync with the existing inventory while an event is agreed. */
  @Override
  public void update() {
    if (disposed) {
      return;
    }
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
    if (disposed) {
      return;
    }
    Label label = quantityLabels.get(slot);
    if (label != null) {
      label.setText(formatSlot(slot, count));
    }
  }

  /** Updates the selected consumable indicator. A null slot means that nothing is selected. */
  public void updateSelectedConsumable(ConsumableSlot slot) {
    if (!disposed && selectedLabel != null) {
      selectedLabel.setText(formatSelected(slot));
    }
  }

  private void onConsumableInventoryChanged(ItemType itemType, int newCount) {
    ConsumableSlot slot = ConsumableSlot.fromItemType(itemType);
    if (slot != null) {
      updateConsumableCount(slot, newCount);
      refreshQuickSlots();
    }
  }

  private void onSelectedConsumableChanged(ItemType itemType) {
    updateSelectedConsumable(ConsumableSlot.fromItemType(itemType));
  }

  private void refreshQuickSlots() {
    if (disposed) {
      return;
    }
    ConsumableLoadoutComponent loadout = entity.getComponent(ConsumableLoadoutComponent.class);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    String[] keys = {"8", "9", "0"};
    for (int i = 0; i < quickLabels.length; i++) {
      if (quickLabels[i] != null) {
        ItemType type = loadout == null ? null : loadout.getSlot(i);
        ConsumableSlot slot = ConsumableSlot.fromItemType(type);
        int count = inventory == null ? 0 : inventory.getConsumableCount(type);
        quickLabels[i].setText(
            "[" + keys[i] + "] " + (slot == null ? "Empty" : slot.displayName + " x" + count));
      }
    }
  }

  static String formatGold(int gold) {
    return String.format("Gold: %d", Math.max(gold, 0));
  }

  static String formatSlot(ConsumableSlot slot, int count) {
    return String.format("%s x%d", slot.displayName, Math.max(count, 0));
  }

  static String formatSelected(ConsumableSlot slot) {
    return String.format("Last used: %s", slot == null ? "None" : slot.displayName);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Draw is handled by the stage.
  }

  @Override
  public void dispose() {
    disposed = true;
    super.dispose();
    if (table != null) {
      table.remove();
    }
  }
}
