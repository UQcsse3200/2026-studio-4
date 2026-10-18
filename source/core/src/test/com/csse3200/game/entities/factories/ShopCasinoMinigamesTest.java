package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.DiceDuelGame;
import com.csse3200.game.components.shop.RockPaperScissorsGame;
import com.csse3200.game.components.shop.ScratchCardGame;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Plays Dice Duel, Rock-Paper-Scissors and Scratch Card through the real merchant casino. */
@ExtendWith(GameExtension.class)
class ShopCasinoMinigamesTest {
  private static final List<String[]> GAMES =
      List.of(
          new String[] {"casino-select-coin", "coin-flip-panel", "Coin Flip"},
          new String[] {"casino-select-item", "item-draw-panel", "Item Draw"},
          new String[] {"casino-select-dice", "dice-duel-panel", "Dice Duel"},
          new String[] {"casino-select-rps", "rps-panel", "Rock Paper Scissors"},
          new String[] {"casino-select-scratch", "scratch-card-panel", "Scratch Card"});

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
    player = new Entity().addComponent(inventory).addComponent(new WeaponUpgradeComponent());
    shop = ShopFactory.createShop(player, new Terminal());
    ServiceLocator.getEntityService().register(shop);
  }

  @AfterEach
  void tearDown() {
    if (shop != null) shop.dispose();
    stage.dispose();
    resources.dispose();
  }

  private void openCasino() {
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
    button("shop-casino").fire(new ChangeEvent());
  }

  private TextButton button(String name) {
    TextButton button = stage.getRoot().findActor(name);
    assertNotNull(button, name);
    return button;
  }

  @Test
  void everyCasinoGameHasASelectorThatShowsOnlyItsPanel() {
    openCasino();
    for (String[] selected : GAMES) {
      button(selected[0]).fire(new ChangeEvent());
      for (String[] game : GAMES) {
        boolean shouldShow = game == selected;
        Actor panel = stage.getRoot().findActor(game[1]);
        assertNotNull(panel, game[1]);
        assertEquals(shouldShow, panel.isVisible(), game[1]);
        String expected = shouldShow ? "[ " + game[2] + " ]" : game[2];
        assertEquals(expected, button(game[0]).getText().toString());
      }
    }
  }

  @Test
  void diceDuelRoundSettlesOnceWithTheRealInventory() {
    int[] resolved = {0};
    shop.getEvents()
        .addListener("diceDuelResolved", (DiceDuelGame.Outcome outcome) -> resolved[0]++);
    openCasino();
    button("casino-select-dice").fire(new ChangeEvent());
    button("dice-duel-roll").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertFalse(button("dice-duel-stand").isDisabled());
    button("dice-duel-stand").fire(new ChangeEvent());
    button("dice-duel-stand").fire(new ChangeEvent());
    assertEquals(1, resolved[0]);
    // lose, tie, win, or win with a doubles bonus
    assertTrue(Set.of(15, 25, 35, 40).contains(inventory.getGold()));
  }

  @Test
  void rockPaperScissorsMatchFinishesWithinThreeRounds() {
    int[] resolved = {0};
    shop.getEvents()
        .addListener(
            "rockPaperScissorsResolved",
            (RockPaperScissorsGame.MatchOutcome outcome) -> resolved[0]++);
    openCasino();
    button("casino-select-rps").fire(new ChangeEvent());
    assertTrue(button("rps-rock").isDisabled());
    button("rps-start").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    for (int round = 0; round < RockPaperScissorsGame.MAX_ROUNDS && resolved[0] == 0; round++) {
      button("rps-rock").fire(new ChangeEvent());
    }
    assertEquals(1, resolved[0]);
    assertTrue(button("rps-rock").isDisabled());
    assertTrue(Set.of(15, 25, 35).contains(inventory.getGold()));
  }

  @Test
  void scratchCardPaysThroughTheRealInventory() {
    int[] payout = {-1};
    shop.getEvents()
        .addListener(
            "scratchCardResolved",
            (ScratchCardGame.CardOutcome outcome) -> payout[0] = outcome.payout());
    openCasino();
    button("casino-select-scratch").fire(new ChangeEvent());
    button("scratch-buy").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    button("scratch-all").fire(new ChangeEvent());
    assertTrue(payout[0] >= 0);
    assertEquals(15 + payout[0], inventory.getGold());
    assertTrue(button("scratch-spot-0").isDisabled());
  }

  @Test
  void leavingTheCasinoMidRoundSettlesInsteadOfLosingTheStake() {
    int[] resolved = {0};
    shop.getEvents()
        .addListener("diceDuelResolved", (DiceDuelGame.Outcome outcome) -> resolved[0]++);
    openCasino();
    button("casino-select-dice").fire(new ChangeEvent());
    button("dice-duel-roll").fire(new ChangeEvent());
    button("shop-casino").fire(new ChangeEvent()); // back to the shop mid-round
    assertEquals(1, resolved[0]);
    assertTrue(Set.of(15, 25, 35, 40).contains(inventory.getGold()));
  }

  @Test
  void newSelectorsStayOnScreenAtSmallWindowSizes() {
    openCasino();
    Table root = stage.getRoot().findActor("shop-root");
    for (int[] size : new int[][] {{1280, 800}, {906, 706}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      for (String[] game : GAMES) {
        Actor selector = button(game[0]);
        Vector2 corner = selector.localToStageCoordinates(new Vector2());
        assertTrue(corner.x >= 0 && corner.x + selector.getWidth() <= stage.getWidth(), game[0]);
        assertTrue(corner.y >= 0 && corner.y + selector.getHeight() <= stage.getHeight(), game[0]);
      }
    }
  }

  @Test
  void bettingYourWholeBalanceKeepsTheFullStake() {
    int[] stakes = {-1, -1, -1};
    shop.getEvents()
        .addListener("diceDuelResolved", (DiceDuelGame.Outcome o) -> stakes[0] = o.stake());
    shop.getEvents()
        .addListener(
            "rockPaperScissorsResolved",
            (RockPaperScissorsGame.MatchOutcome o) -> stakes[1] = o.stake());
    shop.getEvents()
        .addListener(
            "scratchCardResolved", (ScratchCardGame.CardOutcome o) -> stakes[2] = o.stake());
    openCasino();

    inventory.setGold(10);
    button("casino-select-dice").fire(new ChangeEvent());
    button("dice-duel-roll").fire(new ChangeEvent());
    button("dice-duel-stand").fire(new ChangeEvent());
    assertEquals(10, stakes[0]);

    inventory.setGold(10);
    button("casino-select-rps").fire(new ChangeEvent());
    button("rps-start").fire(new ChangeEvent());
    for (int round = 0; round < RockPaperScissorsGame.MAX_ROUNDS && stakes[1] < 0; round++) {
      button("rps-rock").fire(new ChangeEvent());
    }
    assertEquals(10, stakes[1]);

    inventory.setGold(10);
    button("casino-select-scratch").fire(new ChangeEvent());
    button("scratch-buy").fire(new ChangeEvent());
    button("scratch-all").fire(new ChangeEvent());
    assertEquals(10, stakes[2]);
  }
}
