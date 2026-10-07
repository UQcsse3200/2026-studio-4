package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemIds;
import java.util.List;

/** Owns the fixed consumable slots and the currently selected slot. */
public class ConsumableSelectionComponent extends Component {
  public static final String CYCLE_REQUEST = "cycleConsumable";
  public static final String USE_SELECTED_REQUEST = "useSelectedConsumable";
  public static final String SELECTION_CHANGED = "selectedConsumableChanged";

  public static final List<String> SLOTS =
      List.of(
          ItemIds.HEALTH_POTION,
          ItemIds.SHIELD,
          ItemIds.SPEED_POTION,
          ItemIds.STRENGTH_POTION,
          ItemIds.FREEZE_BOMB,
          // Added for #197 C1; ConsumableHotbarDisplay sizes itself off this list, so this is
          // the only change needed to give Burn Vial a 6th slot. Flagging for review since this
          // list is shared by every player.
          ItemIds.BURN_VIAL);

  private int selectedIndex;

  @Override
  public void create() {
    entity.getEvents().addListener(CYCLE_REQUEST, this::cycle);
    entity.getEvents().addListener(USE_SELECTED_REQUEST, this::useSelected);
  }

  public String getSelectedType() {
    return SLOTS.get(selectedIndex);
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  /** Advances one slot and wraps after the last. */
  public void cycle() {
    selectedIndex = (selectedIndex + 1) % SLOTS.size();
    entity.getEvents().trigger(SELECTION_CHANGED, getSelectedType());
  }

  /** The effect component validates inventory and whether the selected item can be used. */
  public void useSelected() {
    entity.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, getSelectedType());
  }
}
