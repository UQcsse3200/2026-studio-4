package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;

/** Selects an existing consumable for the hotbar without owning stock or effect rules. */
public class ConsumableSelectionComponent extends Component {
  public static final String CYCLE_REQUEST = "cycleConsumable";
  public static final String USE_SELECTED_REQUEST = "useSelectedConsumable";
  public static final String SELECTION_CHANGED = "selectedConsumableChanged";
  private int selectedIndex;

  @Override
  public void create() {
    entity.getEvents().addListener(CYCLE_REQUEST, this::cycle);
    entity.getEvents().addListener(USE_SELECTED_REQUEST, this::useSelected);
  }

  public ItemType getSelectedType() {
    return entity.getComponent(InventoryComponent.class).getConsumableSlot(selectedIndex);
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  /** Cycle every slot, including empty slots, and wrap back to the first. */
  public void cycle() {
    selectedIndex = (selectedIndex + 1) % InventoryComponent.CONSUMABLE_SLOT_COUNT;
    entity.getEvents().trigger(SELECTION_CHANGED, getSelectedType());
  }

  /** The existing effect component validates stock and consumes only a successful use. */
  public void useSelected() {
    ItemType type = getSelectedType();
    if (type != null) {
      entity.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, type);
    }
  }
}
