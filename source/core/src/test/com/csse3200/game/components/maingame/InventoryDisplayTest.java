package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the book without requiring a player, weapon assets, or hotbar. */
@ExtendWith(GameExtension.class)
class InventoryDisplayTest {
  private Stage stage;
  private Entity ui;
  private InventoryDisplay display;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    display = new InventoryDisplay();
    ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    stage.dispose();
  }

  @Test
  void pageChangesPreserveVisibilityWithoutLeakingActors() {
    assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
    assertNull(stage.getRoot().findActor("inventory-hotbar"));
    for (int i = 0; i < 4; i++) {
      display.setVisible(true);
      display.changePage();
      assertTrue(stage.getRoot().findActor("inventory-book").isVisible());
      display.setVisible(false);
      display.changePage();
      assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
      assertEquals(1, stage.getActors().size);
    }
  }

  @Test
  void resizingKeepsBookVerticallyCentred() {
    // The original book layout centres itself without reserving any hotbar space.
    for (int[] size : new int[][] {{906, 706}, {1280, 800}, {1918, 1080}}) {
      stage.getViewport().update(size[0], size[1], true);
      display.setVisible(true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.invalidateHierarchy();
      book.validate();
      Actor cover = book.getChildren().first();
      assertEquals((stage.getHeight() - cover.getHeight()) / 2f, cover.getY(), 1f);
    }
  }

  @Test
  void boundBookUsesRealInventoryExactlyOnceAndRefreshesWhileGameplayIsPaused() {
    Entity player = bindPlayer(new AtomicLong());
    InventoryComponent stock = player.getComponent(InventoryComponent.class);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stock.addGold(12);
    stock.addConsumable(ItemType.HEALTH_POTION, 2);
    stats.setHealth(60);
    display.setVisible(true);
    assertEquals("Gold: 12", label("inventory-gold"));
    assertEquals("x2", label("inventory-count-HEALTH_POTION"));
    ImageButton use = stage.getRoot().findActor("inventory-use-HEALTH_POTION");
    use.fire(new ChangeEvent());
    assertEquals(85, stats.getHealth());
    assertEquals(1, stock.getConsumableCount(ItemType.HEALTH_POTION));
    assertEquals("x1", label("inventory-count-HEALTH_POTION"));
    use.fire(new ChangeEvent());
    assertEquals(100, stats.getHealth());
    assertEquals(0, stock.getConsumableCount(ItemType.HEALTH_POTION));
    stock.addConsumable(ItemType.HEALTH_POTION, 1);
    stock.addGold(3);
    stage.act(0f); // The UI component is disabled, but Scene2D remains active for the paused book.
    assertEquals("Gold: 15", label("inventory-gold"));
    assertEquals("x1", label("inventory-count-HEALTH_POTION"));
    assertTrue(use.isDisabled());
    use.fire(new ChangeEvent());
    assertEquals(1, stock.getConsumableCount(ItemType.HEALTH_POTION));
    display.setVisible(false);
    stock.addConsumable(ItemType.SPEED_POTION, 3);
    display.setVisible(true);
    assertEquals("x3", label("inventory-count-SPEED_POTION"));
    for (int i = 0; i < 3; i++) {
      display.changePage();
      display.changePage();
      assertEquals(1, stage.getActors().size);
    }
    assertEquals("x3", label("inventory-count-SPEED_POTION"));
    player.dispose();
  }

  @Test
  void bookButtonsApplyRealTimedEffectsAndShowExpiry() {
    AtomicLong now = new AtomicLong();
    Entity player = bindPlayer(now);
    InventoryComponent stock = player.getComponent(InventoryComponent.class);
    for (ItemType type :
        new ItemType[] {ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION}) {
      stock.addConsumable(type);
    }
    display.setVisible(true);
    for (ItemType type :
        new ItemType[] {ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION}) {
      ImageButton use = stage.getRoot().findActor("inventory-use-" + type.name());
      use.fire(new ChangeEvent());
      assertEquals(0, stock.getConsumableCount(type));
      assertEquals("8.0s", label("inventory-effect-" + type.name()));
      assertTrue(use.isDisabled());
    }
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    assertEquals(15, stats.getEffectiveBaseAttack());
    stats.takeDamage(10);
    assertEquals(100, stats.getHealth());
    now.set(8000);
    stage.act(0f);
    assertEquals("", label("inventory-effect-SHIELD"));
    assertEquals("", label("inventory-effect-SPEED_POTION"));
    assertEquals("", label("inventory-effect-STRENGTH_POTION"));
    assertEquals(10, stats.getEffectiveBaseAttack());
    player.dispose();
  }

  private Entity bindPlayer(AtomicLong now) {
    ui.dispose();
    GameTime clock = mock(GameTime.class);
    when(clock.getTime()).thenAnswer(inv -> now.get());
    ServiceLocator.registerTimeSource(clock);
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new InventoryComponent(0))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new ConsumableEffectComponent());
    player.create();
    display = new InventoryDisplay(player);
    display.setEnabled(false);
    ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    return player;
  }

  private String label(String name) {
    return ((Label) stage.getRoot().findActor(name)).getText().toString();
  }

  @Test
  void disposingBookRemovesItsActor() {
    ui.dispose();
    ui = null;
    assertEquals(0, stage.getActors().size);
  }
}
