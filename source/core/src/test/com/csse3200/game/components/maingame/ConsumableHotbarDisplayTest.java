package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.ConsumableSelectionComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.KeyboardPlayerInputComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises the new HUD against main's real inventory and consumable effects. */
@ExtendWith(GameExtension.class)
class ConsumableHotbarDisplayTest {
  private Stage stage;
  private ResourceService resources;
  private Entity player;
  private Entity ui;
  private InventoryComponent inventory;
  private KeyboardPlayerInputComponent input;
  private AtomicLong now;

  @BeforeEach
  void setUp() {
    now = new AtomicLong();
    GameTime clock = mock(GameTime.class);
    when(clock.getTime()).thenAnswer(inv -> now.get());
    ServiceLocator.registerTimeSource(clock);
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 720, true);
    RenderService render = new RenderService();
    render.setStage(stage);
    ServiceLocator.registerRenderService(render);
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(
        new String[] {
          "images/consumable-slot-idle.png", "images/consumable-slot-selected.png",
          "images/health_potion_pixel.png", "images/shield_consumable_pixel.png",
          "images/speed_potion_pixel.png", "images/strength_potion_pixel.png"
        });
    resources.loadAll();
    inventory = new InventoryComponent(0);
    input = new KeyboardPlayerInputComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 8))
            .addComponent(inventory)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new ConsumableEffectComponent())
            .addComponent(new ConsumableSelectionComponent())
            .addComponent(input);
    player.create();
    ui = new Entity().addComponent(new ConsumableHotbarDisplay(player));
    ui.create();
  }

  @AfterEach
  void tearDown() {
    if (ui != null) ui.dispose();
    player.dispose();
    resources.dispose();
    stage.dispose();
  }

  @Test
  void emptySlotsHideIconsCountsAndUseHintsWithoutInterceptingInput() {
    Table root = stage.getRoot().findActor("consumable-hotbar");
    assertNotNull(root);
    assertEquals(Touchable.disabled, root.getTouchable());
    for (String id : new String[] {"health_potion", "shield", "speed_potion", "strength_potion"}) {
      assertNotNull(actor("consumable-slot-" + id));
      assertFalse(actor("consumable-icon-" + id).isVisible());
      assertFalse(actor("consumable-count-" + id).isVisible());
      assertFalse(actor("consumable-use-" + id).isVisible());
    }
    assertTrue(actor("consumable-pointer-health_potion").isVisible());
  }

  @Test
  void pickupAndQUseUpdateRealHealthQuantityAndHudTogether() {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(50);
    inventory.addConsumable(ItemType.HEALTH_POTION, 2);
    assertEquals("2", count("health_potion").getText().toString());
    assertTrue(actor("consumable-icon-health_potion").isVisible());
    assertTrue(actor("consumable-use-health_potion").isVisible());
    assertTrue(input.keyDown(Keys.Q));
    assertEquals(75, stats.getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
    assertEquals("1", count("health_potion").getText().toString());
    input.keyDown(Keys.Q);
    assertEquals(100, stats.getHealth());
    assertFalse(actor("consumable-icon-health_potion").isVisible());
    assertFalse(count("health_potion").isVisible());
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void fullHealthUseLeavesPotionAndVisibleCountUnchanged() {
    inventory.addConsumable(ItemType.HEALTH_POTION, 2);
    input.keyDown(Keys.Q);
    assertEquals(2, inventory.getConsumableCount(ItemType.HEALTH_POTION));
    assertEquals("2", count("health_potion").getText().toString());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void tabWrapsAllFourSlotsAndUpdatesHighlightEvenWhenEmpty() {
    for (String id : new String[] {"shield", "speed_potion", "strength_potion", "health_potion"}) {
      input.keyDown(Keys.TAB);
      assertTrue(actor("consumable-pointer-" + id).isVisible());
      Group slot = actor("consumable-slot-" + id);
      Image frame = (Image) slot.getChildren().get(0);
      assertSame(
          resources.getAsset("images/consumable-slot-selected.png", Texture.class),
          ((TextureRegionDrawable) frame.getDrawable()).getRegion().getTexture());
    }
    assertFalse(actor("consumable-pointer-shield").isVisible());
    assertEquals(
        ItemType.HEALTH_POTION,
        player.getComponent(ConsumableSelectionComponent.class).getSelectedType());
  }

  @Test
  void selectedQUseAppliesExistingShieldSpeedAndStrengthRules() {
    ConsumableEffectComponent effects = player.getComponent(ConsumableEffectComponent.class);
    for (ItemType type :
        new ItemType[] {ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION}) {
      inventory.addConsumable(type, 2);
      input.keyDown(Keys.TAB);
      input.keyDown(Keys.Q);
      assertEquals(1, inventory.getConsumableCount(type));
      assertEquals(8000, effects.getRemainingMs(type));
    }
    assertTrue(effects.isShielded());
    assertEquals(12, player.getComponent(CombatStatsComponent.class).getEffectiveBaseAttack());
  }

  @Test
  void speedRingTracksRealEffectRefreshExpiryAndRemoval() {
    inventory.addConsumable(ItemType.SPEED_POTION, 2);
    input.keyDown(Keys.TAB);
    input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    Actor ring = actor("speed-potion-timer-ring");
    Batch batch = mock(Batch.class);
    ring.draw(batch, 1f);
    assertEquals(48, drawCalls(batch));
    clearInvocations(batch);
    now.set(4000);
    ring.draw(batch, 1f);
    assertEquals(24, drawCalls(batch));
    clearInvocations(batch);
    input.keyDown(Keys.Q);
    ring.draw(batch, 1f);
    assertEquals(48, drawCalls(batch));
    clearInvocations(batch);
    now.set(12000);
    ring.draw(batch, 1f);
    assertEquals(0, drawCalls(batch));
    inventory.addConsumable(ItemType.SPEED_POTION);
    input.keyDown(Keys.Q);
    player.getComponent(StatusEffectsControllerComponent.class).clearStatusEffects();
    ring.draw(batch, 1f);
    assertEquals(0, drawCalls(batch));
  }

  @Test
  void goldChangesRemainVisibleAfterReplacingOldHud() {
    inventory.addGold(25);
    ui.update();
    Label label = stage.getRoot().findActor("consumable-gold");
    assertEquals("Gold: 25", label.getText().toString());
    inventory.addGold(-10);
    ui.update();
    assertEquals("Gold: 15", label.getText().toString());
  }

  @Test
  void resizingKeepsSlotsOnScreenAboveTheBottomHotbar() {
    Table root = stage.getRoot().findActor("consumable-hotbar");
    for (int[] size : new int[][] {{1920, 1080}, {1280, 720}, {906, 600}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      Actor first = actor("consumable-slot-health_potion");
      Actor last = actor("consumable-slot-strength_potion");
      Vector2 top = first.localToStageCoordinates(new Vector2(first.getWidth(), first.getHeight()));
      Vector2 bottom = last.localToStageCoordinates(new Vector2());
      assertTrue(top.x <= size[0], "Slots must fit horizontally");
      assertTrue(top.y <= size[1], "Slots must fit vertically");
      assertTrue(bottom.y > 144f, "Slots must clear the existing 48+96px bottom hotbar");
    }
  }

  @Test
  void disposingHudRemovesActorsAndLeavesSharedAssetsAvailableForReplacement() {
    ui.dispose();
    ui = null;
    assertNull(stage.getRoot().findActor("consumable-hotbar"));
    assertDoesNotThrow(() -> inventory.addConsumable(ItemType.SHIELD));
    assertDoesNotThrow(() -> input.keyDown(Keys.TAB));
    assertNotNull(resources.getAsset("images/consumable-slot-idle.png", Texture.class));
    ui = new Entity().addComponent(new ConsumableHotbarDisplay(player));
    ui.create();
    assertEquals(1, stage.getActors().size);
    assertEquals("1", count("shield").getText().toString());
  }

  private <T extends Actor> T actor(String name) {
    return stage.getRoot().findActor(name);
  }

  private Label count(String id) {
    return actor("consumable-count-" + id);
  }

  private long drawCalls(Batch batch) {
    return mockingDetails(batch).getInvocations().stream()
        .filter(inv -> inv.getMethod().getName().equals("draw"))
        .count();
  }
}
