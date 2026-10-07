package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the coin flip booth's stake handling, win/lose Gold transactions, and the {@code
 * coinFlipResolved} event it fires after every flip.
 */
@ExtendWith(GameExtension.class)
class CoinFlipDisplayTest {
  private Stage stage;
  private ResourceService resources;
  private Entity ui;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    resources.dispose();
    stage.dispose();
  }

  @Test
  void startsHiddenAndOpenCloseToggleVisibility() {
    CoinFlipDisplay display = createDisplay(new InventoryComponent(100), () -> true);
    assertFalse(display.isOpen());
    display.open();
    assertTrue(display.isOpen());
    display.close();
    assertFalse(display.isOpen());
  }

  @Test
  void rejectsNullConstructorArguments() {
    InventoryComponent inventory = new InventoryComponent(100);
    assertThrows(IllegalArgumentException.class, () -> new CoinFlipDisplay(null));
    assertThrows(IllegalArgumentException.class, () -> new CoinFlipDisplay(inventory, null));
  }

  @Test
  void openingClampsStakeToAvailableGold() {
    // Starting stake is 10 (the minimum); with only 5 Gold the booth cannot offer that stake,
    // so it clamps down to whatever Gold the player actually has.
    CoinFlipDisplay display = createDisplay(new InventoryComponent(5), () -> true);
    display.open();
    assertEquals(5, display.getStake());
  }

  @Test
  void winningFlipDoublesStakeBackAndFiresEvent() {
    InventoryComponent inventory = new InventoryComponent(100);
    CoinFlipDisplay display = createDisplay(inventory, () -> true);
    boolean[] fired = {false};
    ui.getEvents()
        .addListener(
            "coinFlipResolved",
            (Boolean won, Integer stake) -> {
              fired[0] = true;
              assertTrue(won);
              assertEquals(10, stake);
            });

    display.open();
    clickButton("Flip");

    assertTrue(fired[0]);
    assertEquals(110, inventory.getGold());
  }

  @Test
  void losingFlipDeductsStakeAndFiresEvent() {
    InventoryComponent inventory = new InventoryComponent(100);
    CoinFlipDisplay display = createDisplay(inventory, () -> false);
    boolean[] fired = {false};
    ui.getEvents()
        .addListener(
            "coinFlipResolved",
            (Boolean won, Integer stake) -> {
              fired[0] = true;
              assertFalse(won);
              assertEquals(10, stake);
            });

    display.open();
    clickButton("Flip");

    assertTrue(fired[0]);
    assertEquals(90, inventory.getGold());
  }

  @Test
  void stakeButtonsAdjustWithinBounds() {
    CoinFlipDisplay display = createDisplay(new InventoryComponent(100), () -> true);
    display.open();

    clickButton("+10");
    assertEquals(20, display.getStake());

    clickButton("-10");
    clickButton("-10");
    // Already at the minimum; a further decrease must not drop below it.
    clickButton("-10");
    assertEquals(10, display.getStake());
  }

  private CoinFlipDisplay createDisplay(
      InventoryComponent inventory, java.util.function.BooleanSupplier coinToss) {
    CoinFlipDisplay display = new CoinFlipDisplay(inventory, coinToss);
    ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    return display;
  }

  private void clickButton(String text) {
    List<TextButton> buttons = new java.util.ArrayList<>();
    collectButtons(stage.getRoot(), text, buttons);
    assertTrue(buttons.size() >= 1, "button not found: " + text);
    buttons.get(0).toggle();
  }

  private static void collectButtons(Actor actor, String text, List<TextButton> out) {
    if (actor instanceof TextButton button && text.contentEquals(button.getText())) {
      out.add(button);
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collectButtons(child, text, out);
      }
    }
  }
}
