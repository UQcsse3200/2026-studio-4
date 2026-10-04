package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;
import java.util.List;

/** Selects an existing consumable for the hotbar without owning stock or effect rules. */
public class ConsumableSelectionComponent extends Component {
  public static final String CYCLE_REQUEST = "cycleConsumable";
  public static final String USE_SELECTED_REQUEST = "useSelectedConsumable";
  public static final String SELECTION_CHANGED = "selectedConsumableChanged";
  public static final List<ItemType> SLOTS =
      List.of(
          ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION);

  private int selectedIndex;

  @Override
  public void create() {
    entity.getEvents().addListener(CYCLE_REQUEST, this::cycle);
    entity.getEvents().addListener(USE_SELECTED_REQUEST, this::useSelected);
  }

  public ItemType getSelectedType() {
    return SLOTS.get(selectedIndex);
  }

  /** Cycle every slot, including empty slots, and wrap back to the first. */
  public void cycle() {
    selectedIndex = (selectedIndex + 1) % SLOTS.size();
    entity.getEvents().trigger(SELECTION_CHANGED, getSelectedType());
  }

  /** The existing effect component validates stock and consumes only a successful use. */
  public void useSelected() {
    entity.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, getSelectedType());
  }
}
