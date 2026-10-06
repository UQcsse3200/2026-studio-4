package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopSessionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopFactoryTest {
  private Stage stage;
  private ResourceService resources;
  private InventoryComponent inventory;
  private Entity player;
  private Entity shop;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 800, true);
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerInputService(new InputService());
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    inventory = new InventoryComponent(25);
    player = new Entity().addComponent(inventory);
    shop = ShopFactory.createShop(player);
    ServiceLocator.getEntityService().register(shop);
  }

  @AfterEach
  void tearDown() {
    if (shop != null) shop.dispose();
    stage.dispose();
    resources.dispose();
  }

  private void open() {
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
  }

  private TextButton buy(String id) {
    return stage.getRoot().findActor("shop-buy-" + id);
  }

  @Test
  void purchaseUpdatesBalanceOwnedCountAndAffordability() {
    assertTrue(shop.updatesWhilePaused());
    open();
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    Label gold = stage.getRoot().findActor("shop-gold");
    Label owned = stage.getRoot().findActor("shop-owned-" + ItemIds.HEALTH_POTION);
    assertEquals(15, inventory.getGold());
    assertEquals("Gold: 15", gold.getText().toString());
    assertEquals("Owned: 1", owned.getText().toString());
    assertTrue(buy(ItemIds.STRENGTH_POTION).isDisabled());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(5, inventory.getGold());
    assertTrue(buy(ItemIds.HEALTH_POTION).isDisabled());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(5, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void closeAndReopenPreserveInventoryWithoutActorLeaks() {
    open();
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    TextButton close = stage.getRoot().findActor("shop-close");
    close.fire(new ChangeEvent());
    Table root = stage.getRoot().findActor("shop-root");
    assertFalse(root.isVisible());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    open();
    assertTrue(root.isVisible());
    assertEquals(1, stage.getActors().size);
    assertEquals(15, inventory.getGold());
  }

  @Test
  void resizedPanelFitsWindowAndDisposalRemovesActorsAndLocks() {
    open();
    Table root = stage.getRoot().findActor("shop-root");
    Table panel = stage.getRoot().findActor("shop-panel");
    for (int[] size : new int[][] {{1280, 800}, {906, 706}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      assertTrue(panel.getWidth() <= stage.getWidth());
      assertTrue(panel.getHeight() <= stage.getHeight());
    }
    shop.dispose();
    shop = null;
    assertEquals(0, stage.getActors().size);
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
    assertEquals(0, stage.getActors().size);
  }

  @Test
  void modalOwnsKeyboardAndScrollFocusThenRestoresPreviousFocus() {
    Actor background = new Actor();
    int[] typed = {0};
    background.addListener(
        new InputListener() {
          @Override
          public boolean keyTyped(InputEvent event, char character) {
            typed[0]++;
            return true;
          }
        });
    stage.addActor(background);
    stage.setKeyboardFocus(background);
    stage.setScrollFocus(background);
    open();
    ServiceLocator.getInputService().keyTyped('x');
    assertEquals(0, typed[0]);
    assertNotSame(background, stage.getScrollFocus());
    shop.getComponent(ShopSessionComponent.class).close();
    assertSame(background, stage.getKeyboardFocus());
    assertSame(background, stage.getScrollFocus());
  }
}
