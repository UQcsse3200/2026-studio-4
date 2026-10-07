package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopSessionComponentTest {
  static class FakeView implements ShopView {
    int shows;
    int closes;
    int refreshes;
    Runnable onClose;
    boolean failShow;

    public void show(Runnable onClose) {
      if (failShow) throw new IllegalStateException("View unavailable");
      this.onClose = onClose;
      shows++;
    }

    public void close() {
      closes++;
    }

    public void refresh() {
      refreshes++;
    }
  }

  private Entity player;
  private PlayerActions actions;
  private EntityService entities;
  private FakeView view;
  private ShopSessionComponent session;

  @BeforeEach
  void setUp() {
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    actions = new PlayerActions();
    player = new Entity().addComponent(actions);
    player.create();
    view = new FakeView();
    session = new ShopSessionComponent(player, view);
    new Entity().addComponent(session).create();
  }

  @Test
  void shouldOpenOnlyAfterMerchantFinished() {
    player
        .getEvents()
        .trigger(NpcInteractionEvents.INTERACTION_FINISHED, "wanderingSpirit", new Entity());
    player
        .getEvents()
        .trigger(NpcInteractionEvents.INTERACTION_CANCELLED, "merchant", new Entity());
    assertFalse(session.isOpen());
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
    assertTrue(session.isOpen());
    assertTrue(entities.isFrozen());
    assertTrue(actions.areControlsLocked());
    assertEquals(1, view.shows);
    view.onClose.run();
    assertFalse(session.isOpen());
    assertFalse(entities.isFrozen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldIgnoreDuplicateOpen() {
    session.open();
    session.open();
    assertEquals(1, view.shows);
    session.close();
    session.open();
    assertEquals(2, view.shows);
  }

  @Test
  void shouldReleaseOnlyOwnedLocks() {
    Object otherOwner = new Object();
    entities.setFrozen(otherOwner, true);
    actions.setControlsLocked(otherOwner, true);
    session.open();
    session.close();
    assertTrue(entities.isFrozen());
    assertTrue(actions.areControlsLocked());
    entities.setFrozen(otherOwner, false);
    actions.setControlsLocked(otherOwner, false);
    assertFalse(entities.isFrozen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldCloseOnDispose() {
    session.open();
    session.dispose();
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
    session.open();
    assertFalse(session.isOpen());
    assertFalse(entities.isFrozen());
    assertFalse(actions.areControlsLocked());
    assertEquals(1, view.shows);
  }

  @Test
  void shouldReleaseLocksIfViewFailsToOpen() {
    view.failShow = true;
    assertThrows(IllegalStateException.class, session::open);
    assertFalse(session.isOpen());
    assertFalse(entities.isFrozen());
    assertFalse(actions.areControlsLocked());
  }

  @Test
  void shouldRefreshOnlyWhileOpen() {
    player.getEvents().trigger("goldChanged", 20);
    assertEquals(0, view.refreshes);
    session.open();
    int initial = view.refreshes;
    player.getEvents().trigger("goldChanged", 10);
    player.getEvents().trigger("consumableInventoryChanged", "HEALTH_POTION", 1);
    assertEquals(initial + 2, view.refreshes);
    session.close();
    player.getEvents().trigger("goldChanged", 0);
    assertEquals(initial + 2, view.refreshes);
  }

  @Test
  void shouldCloseOnDeath() {
    session.open();
    player.getEvents().trigger("entityDied");
    assertFalse(session.isOpen());
    assertFalse(entities.isFrozen());
  }
}
