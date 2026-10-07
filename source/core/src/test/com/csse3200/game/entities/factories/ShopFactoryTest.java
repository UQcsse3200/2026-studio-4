package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
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
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
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
  private Terminal terminal;
  private WeaponUpgradeComponent upgrades;

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
    upgrades = new WeaponUpgradeComponent();
    player = new Entity().addComponent(inventory).addComponent(upgrades);
    terminal = new Terminal();
    shop = ShopFactory.createShop(player, terminal);
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
  void debugCommandUsesTheSameModalAndLeavesTerminalClosedAfterEnter() {
    KeyboardTerminalInputComponent input = new KeyboardTerminalInputComponent(terminal);
    terminal.setOpen();
    terminal.setEnteredMessage("shop open");
    input.keyTyped('\r');
    assertFalse(terminal.isOpen());
    assertTrue(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertTrue(ServiceLocator.getEntityService().isFrozen());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
    assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    open();
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void debugCloseAndInvalidCommandsDoNotBuyOrLeaveLocksHeld() {
    for (String command : new String[] {"shop", "shop other", "shop open extra"}) {
      terminal.setEnteredMessage(command);
      assertFalse(terminal.processMessage());
      assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    }
    terminal.setEnteredMessage("shop open");
    assertTrue(terminal.processMessage());
    terminal.setEnteredMessage("shop close");
    assertTrue(terminal.processMessage());
    assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    assertEquals(25, inventory.getGold());
  }

  @Test
  void casinoReturnsToShopWithinOnePausedSessionAndKeepsPurchasesWorking() {
    open();
    TextButton casino = stage.getRoot().findActor("shop-casino");
    assertNotNull(casino);
    casino.fire(new ChangeEvent());
    assertFalse(stage.getRoot().findActor("shop-product-scroll").isVisible());
    assertTrue(stage.getRoot().findActor("coin-flip-panel").isVisible());
    assertTrue(ServiceLocator.getEntityService().isFrozen());
    TextButton back = stage.getRoot().findActor("shop-casino");
    back.fire(new ChangeEvent());
    assertTrue(stage.getRoot().findActor("shop-product-scroll").isVisible());
    assertFalse(stage.getRoot().findActor("coin-flip-panel").isVisible());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
    assertFalse(ServiceLocator.getEntityService().isFrozen());
  }

  private TextButton button(String name) {
    TextButton button = stage.getRoot().findActor(name);
    assertNotNull(button, name);
    return button;
  }

  private void enterCasino() {
    open();
    button("shop-casino").fire(new ChangeEvent());
  }

  @Test
  void weaponPagePurchasesSelectedWeaponOnceAndReturnsWithoutReleasingPause() {
    inventory.setGold(150);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    assertTrue(stage.getRoot().findActor("weapon-upgrade-panel").isVisible());
    button("weapon-select-knife").fire(new ChangeEvent());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(90, inventory.getGold());
    assertTrue(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(90, inventory.getGold());
    assertTrue(button("weapon-upgrade-buy").isDisabled());
    button("shop-weapons").fire(new ChangeEvent());
    assertTrue(stage.getRoot().findActor("shop-product-scroll").isVisible());
    assertTrue(ServiceLocator.getEntityService().isFrozen());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(80, inventory.getGold());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(80, inventory.getGold());
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
    assertFalse(ServiceLocator.getEntityService().isFrozen());
  }

  @Test
  void weaponAffordabilityAndExternalUpgradesRefreshWhileOpen() {
    open();
    button("shop-weapons").fire(new ChangeEvent());
    TextButton upgrade = button("weapon-upgrade-buy");
    assertTrue(upgrade.isDisabled());
    upgrade.fire(new ChangeEvent());
    assertEquals(25, inventory.getGold());
    inventory.setGold(60);
    assertFalse(upgrade.isDisabled());
    upgrades.setUpgraded(SwordWeaponComponent.class, true);
    assertTrue(upgrade.isDisabled());
    upgrades.setUpgraded(SwordWeaponComponent.class, false);
    assertFalse(upgrade.isDisabled());
    upgrade.fire(new ChangeEvent());
    assertEquals(0, inventory.getGold());
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    button("shop-close").fire(new ChangeEvent());
    upgrade.fire(new ChangeEvent());
    assertEquals(0, inventory.getGold());
    open();
    button("shop-weapons").fire(new ChangeEvent());
    assertTrue(upgrade.isDisabled());
    assertEquals(1, stage.getActors().size);
  }

  @Test
  void weaponUpgradeCallbackCanCloseSessionWithoutHoldingLocksOrChargingAgain() {
    inventory.setGold(120);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    player
        .getEvents()
        .addListener(
            "weaponUpgraded",
            (Class<?> weapon) -> shop.getComponent(ShopSessionComponent.class).close());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(60, inventory.getGold());
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(60, inventory.getGold());
  }

  @Test
  void weaponPageNavigationNeverEnablesHiddenPurchaseControls() {
    inventory.setGold(120);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    button("coin-flip-button").fire(new ChangeEvent());
    assertEquals(120, inventory.getGold());
    button("shop-casino").fire(new ChangeEvent());
    assertFalse(stage.getRoot().findActor("weapon-upgrade-panel").isVisible());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(120, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    button("shop-weapons").fire(new ChangeEvent());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(60, inventory.getGold());
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void returningFromWeaponsAfterEitherCasinoGameRestoresSupplyInstructions() {
    open();
    Label feedback = stage.getRoot().findActor("shop-feedback");
    String supplies = feedback.getText().toString();
    for (boolean itemDraw : new boolean[] {false, true}) {
      button("shop-casino").fire(new ChangeEvent());
      if (itemDraw) button("casino-select-item").fire(new ChangeEvent());
      assertNotEquals(supplies, feedback.getText().toString());
      button("shop-weapons").fire(new ChangeEvent());
      button("shop-weapons").fire(new ChangeEvent());
      assertEquals(supplies, feedback.getText().toString());
      assertTrue(stage.getRoot().findActor("shop-product-scroll").isVisible());
      assertFalse(stage.getRoot().findActor("weapon-upgrade-panel").isVisible());
      assertEquals(25, inventory.getGold());
      assertTrue(shop.getComponent(ShopSessionComponent.class).isOpen());
    }
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void weaponUpgradeCallbackMayDisposeShop() {
    assertWeaponPurchaseDisposal(true);
  }

  @Test
  void weaponPurchaseResolvedCallbackMayDisposeShop() {
    assertWeaponPurchaseDisposal(false);
  }

  private void assertWeaponPurchaseDisposal(boolean duringUpgradeEvent) {
    inventory.setGold(120);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    Runnable disposeShop =
        () -> {
          Entity closing = shop;
          shop = null;
          closing.dispose();
        };
    if (duringUpgradeEvent) {
      player.getEvents().addListener("weaponUpgraded", (Class<?> weapon) -> disposeShop.run());
    } else {
      shop.getEvents()
          .addListener(
              "weaponUpgradePurchaseResolved",
              (com.csse3200.game.components.shop.ShopPurchaseResult result) -> disposeShop.run());
    }
    TextButton upgrade = button("weapon-upgrade-buy");
    assertDoesNotThrow(() -> upgrade.fire(new ChangeEvent()));
    assertEquals(60, inventory.getGold());
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    assertEquals(0, stage.getActors().size);
    upgrade.fire(new ChangeEvent());
    assertEquals(60, inventory.getGold());
  }

  @Test
  void weaponShortcutsSelectAndPurchaseOnlyWhileThatPageIsOpen() {
    inventory.setGold(120);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.NUM_2);
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.U);
    assertTrue(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertEquals(60, inventory.getGold());
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.U);
    assertEquals(60, inventory.getGold());
    button("shop-weapons").fire(new ChangeEvent());
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.NUM_1);
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.U);
    assertEquals(60, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void weaponNavigationAndPurchaseFitSmallWindows() {
    open();
    button("shop-weapons").fire(new ChangeEvent());
    Table root = stage.getRoot().findActor("shop-root");
    for (int[] size : new int[][] {{1280, 800}, {960, 600}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      for (String name :
          new String[] {"shop-close", "shop-weapons", "shop-casino", "weapon-upgrade-buy"}) {
        Actor actor = stage.getRoot().findActor(name);
        Vector2 corner = actor.localToStageCoordinates(new Vector2());
        assertTrue(corner.x >= 0 && corner.x + actor.getWidth() <= stage.getWidth(), name);
        assertTrue(corner.y >= 0 && corner.y + actor.getHeight() <= stage.getHeight(), name);
      }
    }
  }

  @Test
  void rejectedWeaponUpgradeExplainsFailureWithoutCharging() {
    shop.dispose();
    inventory = new InventoryComponent(120);
    upgrades = new WeaponUpgradeComponent(java.util.Map.of());
    player = new Entity().addComponent(inventory).addComponent(upgrades);
    shop = ShopFactory.createShop(player);
    ServiceLocator.getEntityService().register(shop);
    open();
    button("shop-weapons").fire(new ChangeEvent());
    button("weapon-upgrade-buy").fire(new ChangeEvent());
    assertEquals(120, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    Label feedback = stage.getRoot().findActor("weapon-upgrade-feedback");
    assertTrue(feedback.getText().toString().toLowerCase().contains("unavailable"));
  }

  @Test
  void animationBlocksDuplicateWagersAndReturningRefreshesAffordability() {
    enterCasino();
    int[] resolutions = {0};
    shop.getEvents()
        .addListener(
            "coinFlipResolved",
            (Boolean won, Integer stake) -> {
              assertEquals(10, stake);
              resolutions[0]++;
            });
    TextButton flip = button("coin-flip-button");
    flip.fire(new ChangeEvent());
    int after = inventory.getGold();
    assertEquals(10, Math.abs(after - 25));
    flip.fire(new ChangeEvent());
    button("coin-flip-up").fire(new ChangeEvent());
    assertEquals(after, inventory.getGold());
    assertEquals(1, resolutions[0]);
    assertTrue(flip.isDisabled());
    for (int i = 0; i < 4; i++) stage.act(0.3f);
    assertFalse(flip.isDisabled());
    Label result = stage.getRoot().findActor("coin-flip-result");
    assertTrue(result.getText().toString().contains(after > 25 ? "won" : "lost"));
    button("shop-casino").fire(new ChangeEvent());
    assertEquals(after < 20, buy(ItemIds.STRENGTH_POTION).isDisabled());
    Label gold = stage.getRoot().findActor("shop-gold");
    assertEquals("Gold: " + after, gold.getText().toString());
  }

  @Test
  void leavingDuringAnimationSettlesOnceAndHiddenControlsCannotGamble() {
    enterCasino();
    TextButton flip = button("coin-flip-button");
    flip.fire(new ChangeEvent());
    int settled = inventory.getGold();
    button("shop-close").fire(new ChangeEvent());
    for (int i = 0; i < 4; i++) stage.act(0.3f);
    flip.fire(new ChangeEvent());
    assertEquals(settled, inventory.getGold());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    open();
    assertTrue(stage.getRoot().findActor("shop-product-scroll").isVisible());
    flip.fire(new ChangeEvent());
    assertEquals(settled, inventory.getGold());
  }

  @Test
  void zeroGoldDisablesFlipAndSmallBalanceCanBeWagered() {
    inventory.setGold(0);
    enterCasino();
    assertTrue(button("coin-flip-button").isDisabled());
    inventory.setGold(5);
    player.getEvents().trigger("goldChanged", 5);
    assertFalse(button("coin-flip-button").isDisabled());
    Label stake = stage.getRoot().findActor("coin-flip-stake");
    assertEquals("5 Gold", stake.getText().toString());
    button("coin-flip-button").fire(new ChangeEvent());
    assertTrue(inventory.getGold() == 0 || inventory.getGold() == 10);
  }

  @Test
  void casinoNavigationAndLeaveRemainInsideSmallWindow() {
    enterCasino();
    Table root = stage.getRoot().findActor("shop-root");
    for (int[] size : new int[][] {{1280, 800}, {906, 706}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      for (String name : new String[] {"shop-close", "shop-casino"}) {
        Actor actor = stage.getRoot().findActor(name);
        Vector2 corner = actor.localToStageCoordinates(new Vector2());
        assertTrue(corner.x >= 0 && corner.x + actor.getWidth() <= stage.getWidth(), name);
        assertTrue(corner.y >= 0 && corner.y + actor.getHeight() <= stage.getHeight(), name);
      }
    }
  }

  @Test
  void resolutionListenerMayCloseSessionWithoutLeavingAnimationOrLocks() {
    enterCasino();
    shop.getEvents()
        .addListener(
            "coinFlipResolved",
            (Boolean won, Integer stake) -> shop.getComponent(ShopSessionComponent.class).close());
    button("coin-flip-button").fire(new ChangeEvent());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    int settled = inventory.getGold();
    for (int i = 0; i < 4; i++) stage.act(0.3f);
    assertEquals(settled, inventory.getGold());
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
      Actor close = stage.getRoot().findActor("shop-close");
      Vector2 corner = close.localToStageCoordinates(new Vector2());
      assertTrue(
          corner.y >= 0 && corner.y + close.getHeight() <= stage.getHeight(),
          "Close button must remain inside the resized window");
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

  @Test
  void closeRestoresFocusAfterScrollingMerchantColumn() {
    Actor background = new Actor();
    stage.addActor(background);
    stage.setScrollFocus(background);
    open();
    Actor merchantScroll = stage.getRoot().findActor("shop-merchant-scroll");
    assertNotNull(merchantScroll);
    stage.setScrollFocus(merchantScroll);
    shop.getComponent(ShopSessionComponent.class).close();
    assertSame(background, stage.getScrollFocus());
  }

  @Test
  void itemDrawSettlesOnceAndHiddenGamesCannotCharge() {
    open();
    TextButton casino = stage.getRoot().findActor("shop-casino");
    casino.fire(new ChangeEvent());
    TextButton select = stage.getRoot().findActor("casino-select-item");
    assertNotNull(select);
    select.fire(new ChangeEvent());
    TextButton draw = stage.getRoot().findActor("item-draw-button");
    assertNotNull(draw);
    draw.fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    draw.fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    TextButton coin = stage.getRoot().findActor("casino-select-coin");
    coin.fire(new ChangeEvent());
    draw.fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertFalse(stage.getRoot().findActor("item-draw-panel").isVisible());
    casino.fire(new ChangeEvent());
    assertFalse(stage.getRoot().findActor("coin-flip-panel").isVisible());
  }

  @Test
  void itemDrawRequiresItsFullConfiguredPrice() {
    inventory.setGold(5);
    open();
    ((TextButton) stage.getRoot().findActor("shop-casino")).fire(new ChangeEvent());
    TextButton select = stage.getRoot().findActor("casino-select-item");
    assertNotNull(select);
    select.fire(new ChangeEvent());
    TextButton draw = stage.getRoot().findActor("item-draw-button");
    assertTrue(draw.isDisabled());
    draw.fire(new ChangeEvent());
    assertEquals(5, inventory.getGold());
    inventory.setGold(10);
    assertFalse(draw.isDisabled());
    draw.fire(new ChangeEvent());
    assertEquals(0, inventory.getGold());
    assertTrue(draw.isDisabled());
  }

  @Test
  void itemDrawRewardEventMatchesAtomicInventoryAndMayCloseSession() {
    int[] resolved = {0};
    shop.getEvents()
        .addListener(
            "itemGambleResolved",
            (com.csse3200.game.shop.GambleResult outcome) -> {
              resolved[0]++;
              assertEquals(15, inventory.getGold());
              if (outcome.entry().isBust()) {
                assertEquals(com.csse3200.game.shop.GambleResult.Status.BUST, outcome.status());
              } else {
                assertEquals(
                    outcome.entry().quantity(),
                    inventory.getConsumableCount(outcome.entry().itemId()));
              }
              shop.getComponent(ShopSessionComponent.class).close();
            });
    open();
    ((TextButton) stage.getRoot().findActor("shop-casino")).fire(new ChangeEvent());
    ((TextButton) stage.getRoot().findActor("casino-select-item")).fire(new ChangeEvent());
    TextButton draw = stage.getRoot().findActor("item-draw-button");
    draw.fire(new ChangeEvent());
    stage.act(1f);
    draw.fire(new ChangeEvent());
    assertEquals(1, resolved[0]);
    assertEquals(15, inventory.getGold());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
  }

  @Test
  void itemDrawReflowsAndScrollsWithoutHorizontalOverflow() {
    open();
    ((TextButton) stage.getRoot().findActor("shop-casino")).fire(new ChangeEvent());
    ((TextButton) stage.getRoot().findActor("casino-select-item")).fire(new ChangeEvent());
    Table root = stage.getRoot().findActor("shop-root");
    for (int[] size : new int[][] {{1280, 800}, {906, 706}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      com.badlogic.gdx.scenes.scene2d.ui.ScrollPane scroll =
          stage.getRoot().findActor("item-draw-scroll");
      scroll.validate();
      assertEquals(0f, scroll.getMaxX(), 0.01f);
      Actor last = stage.getRoot().findActor("item-draw-odds-bust");
      Vector2 location = last.localToAscendantCoordinates(scroll.getActor(), new Vector2());
      scroll.scrollTo(location.x, location.y, last.getWidth(), last.getHeight());
      scroll.updateVisualScroll();
      scroll.act(0f);
      // ScrollPane applies its widget translation during drawing. Verify its visible content
      // interval here rather than using the stale pre-draw stage transform.
      float top = scroll.getActor().getHeight() - scroll.getScrollY();
      assertTrue(location.y >= top - scroll.getScrollHeight() - 0.01f);
      assertTrue(location.y + last.getHeight() <= top + 0.01f);
      Actor selector = stage.getRoot().findActor("casino-select-item");
      Vector2 corner = selector.localToStageCoordinates(new Vector2());
      assertTrue(corner.x >= 0 && corner.x + selector.getWidth() <= stage.getWidth());
      assertTrue(corner.y >= 0 && corner.y + selector.getHeight() <= stage.getHeight());
    }
  }
}
