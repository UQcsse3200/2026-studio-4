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
import com.csse3200.game.components.shop.HigherLowerGame;
import com.csse3200.game.components.shop.RockPaperScissorsGame;
import com.csse3200.game.components.shop.ScratchCardGame;
import com.csse3200.game.components.shop.ShellGame;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercises all seven games through the real merchant casino and player inventory. */
@ExtendWith(GameExtension.class)
class ShopCasinoMinigamesTest {
  private static final List<String[]> GAMES =
      List.of(
          new String[] {"casino-select-coin", "coin-flip-panel", "Coin Flip"},
          new String[] {"casino-select-item", "item-draw-panel", "Item Draw"},
          new String[] {"casino-select-dice", "dice-duel-panel", "Dice Duel"},
          new String[] {"casino-select-rps", "rps-panel", "Rock Paper Scissors"},
          new String[] {"casino-select-scratch", "scratch-card-panel", "Scratch Card"},
          new String[] {"casino-select-higher-lower", "higher-lower-panel", "Higher/Lower"},
          new String[] {"casino-select-shell-game", "shell-game-panel", "Shell Game"});

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

  @ParameterizedTest
  @CsvSource({
    "casino-select-dice, dice-duel-roll",
    "casino-select-rps, rps-start",
    "casino-select-scratch, scratch-buy",
    "casino-select-higher-lower, higher-lower-start",
    "casino-select-shell-game, shell-game-start"
  })
  void closedMinigameIgnoresInputAndCanBeReopened(String selector, String action) {
    openCasino();
    button(selector).fire(new ChangeEvent());
    TextButton play = button(action);
    button("casino-select-coin").fire(new ChangeEvent());

    // A stale actor event must not spend gold, even if the hidden button was re-enabled.
    play.setDisabled(false);
    play.fire(new ChangeEvent());
    assertEquals(25, inventory.getGold());

    button(selector).fire(new ChangeEvent());
    play.fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
  }

  @ParameterizedTest
  @CsvSource({
    "casino-select-dice, dice-duel-roll",
    "casino-select-rps, rps-start",
    "casino-select-scratch, scratch-buy",
    "casino-select-higher-lower, higher-lower-start",
    "casino-select-shell-game, shell-game-start"
  })
  void disabledMinigameButtonIgnoresInput(String selector, String action) {
    openCasino();
    button(selector).fire(new ChangeEvent());
    TextButton play = button(action);
    play.setDisabled(true);
    play.fire(new ChangeEvent());
    assertEquals(25, inventory.getGold());

    play.setDisabled(false);
    play.fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
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
  void higherLowerRoundSurvivesSwitchingGamesAndCashOutPaysOnce() {
    List<HigherLowerGame.Round> rounds = new ArrayList<>();
    shop.getEvents()
        .addListener("higherLowerResolved", (HigherLowerGame.Round round) -> rounds.add(round));
    openCasino();
    button("casino-select-higher-lower").fire(new ChangeEvent());
    button("higher-lower-start").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, rounds.size());
    assertEquals(HigherLowerGame.Outcome.ACTIVE, rounds.get(0).outcome());

    button("casino-select-coin").fire(new ChangeEvent());
    button("higher-lower-cashout").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, rounds.size());
    button("casino-select-higher-lower").fire(new ChangeEvent());
    assertTrue(button("higher-lower-start").isDisabled());
    assertFalse(button("higher-lower-cashout").isDisabled());
    button("higher-lower-cashout").fire(new ChangeEvent());
    button("higher-lower-cashout").fire(new ChangeEvent());

    assertEquals(2, rounds.size());
    assertEquals(HigherLowerGame.Outcome.CASHED_OUT, rounds.get(1).outcome());
    assertEquals(10, rounds.get(1).payout());
    assertEquals(25, inventory.getGold());
  }

  @Test
  void shellGameRoundSurvivesSwitchingGamesAndGuessPaysOnce() {
    List<ShellGame.Round> rounds = new ArrayList<>();
    shop.getEvents().addListener("shellGameResolved", (ShellGame.Round round) -> rounds.add(round));
    openCasino();
    button("casino-select-shell-game").fire(new ChangeEvent());
    button("shell-game-start").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());

    button("casino-select-coin").fire(new ChangeEvent());
    button("shell-game-cup-0").fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertTrue(rounds.isEmpty());
    button("casino-select-shell-game").fire(new ChangeEvent());
    assertTrue(button("shell-game-start").isDisabled());
    assertFalse(button("shell-game-cup-0").isDisabled());
    button("shell-game-cup-0").fire(new ChangeEvent());
    button("shell-game-cup-0").fire(new ChangeEvent());

    assertEquals(1, rounds.size());
    ShellGame.Round round = rounds.get(0);
    assertEquals(round.winningCup() == 0, round.won());
    assertEquals(round.won() ? 20 : 0, round.payout());
    assertEquals(15 + round.payout(), inventory.getGold());
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
