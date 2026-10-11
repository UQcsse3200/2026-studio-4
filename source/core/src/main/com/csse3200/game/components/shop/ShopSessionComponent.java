package com.csse3200.game.components.shop;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/** Opens after the merchant greeting, owning only the shop's world freeze and control lock. */
public class ShopSessionComponent extends Component {
  private final Entity player;
  private final ShopView view;
  private boolean open;
  private boolean disposed;

  public ShopSessionComponent(Entity player, ShopView view) {
    this.player = Objects.requireNonNull(player);
    this.view = Objects.requireNonNull(view);
  }

  @Override
  public void create() {
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_FINISHED,
            (String npcId, Entity npc) -> {
              if ("merchant".equals(npcId)) open();
            });
    player.getEvents().addListener("goldChanged", (Integer gold) -> refresh());
    player
        .getEvents()
        .addListener("weaponUpgraded", (Class<? extends WeaponComponent> weapon) -> refresh());
    player
        .getEvents()
        .addListener("consumableInventoryChanged", (String id, Integer count) -> refresh());
    player.getEvents().addListener("entityDied", this::close);
  }

  public boolean isOpen() {
    return open;
  }

  public void open() {
    if (open || disposed) return;
    open = true;
    hold(true);
    InteractionPromptDisplay prompt = player.getComponent(InteractionPromptDisplay.class);
    if (prompt != null) prompt.clearPrompt();
    try {
      view.show(this::close);
      refresh();
    } catch (RuntimeException failure) {
      close();
      throw failure;
    }
  }

  public void close() {
    if (!open) return;
    open = false;
    try {
      view.close();
    } finally {
      hold(false);
    }
  }

  private void refresh() {
    if (open && !disposed) view.refresh();
  }

  private void hold(boolean held) {
    if (ServiceLocator.getEntityService() != null) {
      ServiceLocator.getEntityService().setFrozen(this, held);
    }
    PlayerActions actions = player.getComponent(PlayerActions.class);
    if (actions != null) actions.setControlsLocked(this, held);
  }

  @Override
  public void dispose() {
    disposed = true;
    close();
    super.dispose();
  }
}
