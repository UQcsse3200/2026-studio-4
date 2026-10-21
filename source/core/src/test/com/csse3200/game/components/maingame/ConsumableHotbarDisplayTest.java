package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.csse3200.game.components.weapons.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UiScale;
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
          "images/consumable-slot-idle.png",
          "images/consumable-slot-selected.png",
          "images/health_potion_pixel.png",
          "images/shield_consumable_pixel.png",
          "images/health_potion_small_pixel.png",
          "images/health_potion_medium_pixel.png",
          "images/health_potion_large_pixel.png",
          "images/speed_potion_pixel.png",
          "images/strength_potion_pixel.png",
          "images/burn_vial_pixel.png",
          "images/magnet_potion_pixel.png"
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
  void burnVialPickupUsesAnExistingSlotAndQUpdatesItsIconAndCount() {
    ServiceLocator.registerWorldCamera(new OrthographicCamera(20f, 10f));
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    ItemCatalog.create(ItemIds.BURN_VIAL, 2).pickUp(player);

    assertEquals(ItemIds.BURN_VIAL, inventory.getConsumableSlot(1));
    assertEquals("2", count("2").getText().toString());
    Image icon = actor("consumable-icon-2");
    assertSame(
        resources.getAsset("images/burn_vial_pixel.png", Texture.class),
        ((TextureRegionDrawable) icon.getDrawable()).getRegion().getTexture());
    assertNull(stage.getRoot().findActor("consumable-slot-5"));
    assertNull(stage.getRoot().findActor("consumable-slot-6"));

    input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    assertEquals("1", count("2").getText().toString());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    assertNull(inventory.getConsumableSlot(1));
    assertFalse(icon.isVisible());
    assertFalse(count("2").isVisible());
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemIds.BURN_VIAL));
  }

  @Test
  void magnetUsesCatalogIconInFourthSlotAndQRefreshesItsCount() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.BURN_VIAL);
    ItemCatalog.create(ItemIds.MAGNET_POTION, 2).pickUp(player);
    Image icon = actor("consumable-icon-4");
    assertSame(
        resources.getAsset("images/magnet_potion_pixel.png", Texture.class),
        ((TextureRegionDrawable) icon.getDrawable()).getRegion().getTexture());
    assertEquals("2", count("4").getText().toString());
    for (int i = 0; i < 3; i++) input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    assertEquals("1", count("4").getText().toString());
    assertEquals(
        10000,
        player.getComponent(ConsumableEffectComponent.class).getRemainingMs(ItemIds.MAGNET_POTION));
    input.keyDown(Keys.Q);
    assertFalse(icon.isVisible());
    assertFalse(count("4").isVisible());
    assertNull(inventory.getConsumableSlot(3));
    assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
  }

  @Test
  void emptySlotsHideIconsCountsAndUseHintsWithoutInterceptingInput() {
    Table root = stage.getRoot().findActor("consumable-hotbar");
    assertNotNull(root);
    assertEquals(Touchable.disabled, root.getTouchable());
    for (String id : new String[] {"1", "2", "3", "4"}) {
      assertNotNull(actor("consumable-slot-" + id));
      assertFalse(actor("consumable-icon-" + id).isVisible());
      assertFalse(actor("consumable-count-" + id).isVisible());
      assertFalse(actor("consumable-use-" + id).isVisible());
    }
    assertNull(actor("consumable-slot-5"));
    assertTrue(actor("consumable-pointer-1").isVisible());
  }

  @Test
  void drawingKeepsHudAboveLateSpawnedEnemyBarsWithoutReorderingOtherActors() {
    Actor hud = actor("consumable-hotbar");
    Actor existingUi = new Actor();
    stage.addActor(existingUi);
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 8))
            .addComponent(new com.csse3200.game.components.npc.EnemyStatDisplay());
    enemy.create();
    try {
      Actor healthBar = stage.getActors().peek();
      assertTrue(healthBar.getZIndex() > hud.getZIndex());
      ui.getComponent(ConsumableHotbarDisplay.class).draw(mock(SpriteBatch.class));
      assertTrue(hud.getZIndex() > healthBar.getZIndex());
      assertTrue(healthBar.getZIndex() > existingUi.getZIndex());
      assertTrue(healthBar.isVisible());
      assertTrue(existingUi.isVisible());

      // Another spawn after the first render must not cover the HUD either.
      Actor nextHealthBar = new Actor();
      stage.addActor(nextHealthBar);
      ui.getComponent(ConsumableHotbarDisplay.class).draw(mock(SpriteBatch.class));
      assertTrue(hud.getZIndex() > nextHealthBar.getZIndex());
      assertTrue(nextHealthBar.getZIndex() > healthBar.getZIndex());
    } finally {
      enemy.dispose();
    }
  }

  @Test
  void openInventoryStaysAboveHudAndOnlyHudMovesWhenBookCloses() {
    Actor hud = actor("consumable-hotbar");
    Actor book = new Actor();
    book.setName("inventory-book");
    stage.addActor(book);
    Actor healthBar = new Actor();
    stage.addActor(healthBar);
    // Opening the book uses its existing behavior to bring it forward.
    book.toFront();
    ui.getComponent(ConsumableHotbarDisplay.class).draw(mock(SpriteBatch.class));
    assertTrue(book.getZIndex() > hud.getZIndex());
    assertTrue(hud.getZIndex() > healthBar.getZIndex());

    // Exercise the case where the HUD already follows an open book.
    hud.toFront();
    ui.getComponent(ConsumableHotbarDisplay.class).draw(mock(SpriteBatch.class));
    assertTrue(book.getZIndex() > hud.getZIndex());
    assertTrue(hud.getZIndex() > healthBar.getZIndex());
    book.setVisible(false);
    stage.addActor(new Actor());
    ui.getComponent(ConsumableHotbarDisplay.class).draw(mock(SpriteBatch.class));
    assertSame(hud, stage.getActors().peek());
    assertFalse(book.isVisible());
    assertTrue(healthBar.isVisible());
  }

  @Test
  void pickupAndQUseUpdateRealHealthQuantityAndHudTogether() {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(50);
    inventory.addConsumable(ItemIds.HEALTH_POTION, 2);
    assertEquals("2", count("1").getText().toString());
    assertTrue(actor("consumable-icon-1").isVisible());
    assertTrue(actor("consumable-use-1").isVisible());
    assertTrue(input.keyDown(Keys.Q));
    assertEquals(75, stats.getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals("1", count("1").getText().toString());
    input.keyDown(Keys.Q);
    assertEquals(100, stats.getHealth());
    assertFalse(actor("consumable-icon-1").isVisible());
    assertFalse(count("1").isVisible());
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void fullHealthUseLeavesPotionAndVisibleCountUnchanged() {
    inventory.addConsumable(ItemIds.HEALTH_POTION, 2);
    input.keyDown(Keys.Q);
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals("2", count("1").getText().toString());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void fullHealthUseFlashesTheRejectedSlotRedThenFadesBack() {
    inventory.addConsumable(ItemIds.HEALTH_POTION, 2);
    Group slot = actor("consumable-slot-1");
    Image frame = (Image) slot.getChildren().get(0);
    assertEquals(Color.WHITE, frame.getColor());

    input.keyDown(Keys.Q);
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    stage.act(0.08f);
    Color flashed = frame.getColor();
    assertTrue(flashed.r > 0.9f);
    assertTrue(flashed.g < 0.5f && flashed.b < 0.5f);

    stage.act(0.25f);
    assertEquals(Color.WHITE, frame.getColor());
  }

  @Test
  void successfulUseDoesNotFlashTheSlotRed() {
    player.getComponent(CombatStatsComponent.class).setHealth(50);
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    Group slot = actor("consumable-slot-1");
    Image frame = (Image) slot.getChildren().get(0);

    input.keyDown(Keys.Q);
    stage.act(0.08f);
    assertEquals(Color.WHITE, frame.getColor());
  }

  @Test
  void tabWrapsAllFourSlotsAndUpdatesHighlightEvenWhenEmpty() {
    for (String id : new String[] {"2", "3", "4", "1"}) {
      input.keyDown(Keys.TAB);
      assertTrue(actor("consumable-pointer-" + id).isVisible());
      Group slot = actor("consumable-slot-" + id);
      Image frame = (Image) slot.getChildren().get(0);
      assertSame(
          resources.getAsset("images/consumable-slot-selected.png", Texture.class),
          ((TextureRegionDrawable) frame.getDrawable()).getRegion().getTexture());
    }
    assertFalse(actor("consumable-pointer-2").isVisible());
    assertEquals(null, player.getComponent(ConsumableSelectionComponent.class).getSelectedType());
  }

  @Test
  void selectedQUseAppliesExistingShieldSpeedAndStrengthRules() {
    ConsumableEffectComponent effects = player.getComponent(ConsumableEffectComponent.class);
    for (String type :
        new String[] {ItemIds.SHIELD, ItemIds.SPEED_POTION, ItemIds.STRENGTH_POTION}) {
      inventory.addConsumable(type, 2);
      if (!ItemIds.SHIELD.equals(type)) input.keyDown(Keys.TAB);
      input.keyDown(Keys.Q);
      assertEquals(1, inventory.getConsumableCount(type));
      assertEquals(8000, effects.getRemainingMs(type));
    }
    assertTrue(effects.isShielded());
    assertEquals(12, player.getComponent(CombatStatsComponent.class).getEffectiveBaseAttack());
  }

  @Test
  void speedRingTracksRealEffectRefreshExpiryAndRemoval() {
    inventory.addConsumable(ItemIds.SPEED_POTION, 3);
    input.keyDown(Keys.Q);
    Actor ring = actor("speed-potion-timer-ring-1");
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
    inventory.addConsumable(ItemIds.SPEED_POTION);
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
  void resizingKeepsFourConsumablesInOneBottomRowBesideWeapons() {
    Entity weaponPlayer =
        new Entity()
            .addComponent(new WeaponAssetsComponent())
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(new WeaponSelectionComponent());
    weaponPlayer.create();
    Entity weaponUi = new Entity().addComponent(new HotbarDisplay(weaponPlayer));
    weaponUi.create();
    Table root = stage.getRoot().findActor("consumable-hotbar");
    Table weapons = stage.getRoot().findActor("inventory-hotbar");
    try {
      inventory.addConsumable(ItemIds.SPEED_POTION, 2);
      for (int[] size :
          new int[][] {{1920, 1080}, {1280, 720}, {960, 540}, {906, 600}, {1920, 1080}}) {
        for (float uiScale : new float[] {1f, 0.5f, 2f, 1f}) {
          stage.getViewport().update(size[0], size[1], true);
          UiScale.applyToStage(stage, uiScale);
          weapons.validate();
          root.validate();
          Actor weapon = actor("weapon-frame-3");
          Vector2 weaponPosition = weapon.localToStageCoordinates(new Vector2());
          float previousRight =
              weapon.localToStageCoordinates(new Vector2(weapon.getWidth(), 0f)).x;
          for (int i = 1; i <= 4; i++) {
            Actor slot = actor("consumable-slot-" + i);
            Vector2 bottom = slot.localToStageCoordinates(new Vector2());
            assertEquals(weaponPosition.y, bottom.y, 1f, "All seven slots share a baseline");
            assertEquals(48f * uiScale, bottom.y, 1f);
            assertTrue(bottom.x > previousRight, "Slots must run left to right without overlap");
            assertEquals(weapon.getWidth(), slot.getWidth(), 1f, "Frames use the same diameter");
            previousRight = slot.localToStageCoordinates(new Vector2(slot.getWidth(), 0f)).x;
            assertTrue(previousRight <= size[0] - 24f, "All four consumables fit on screen");
            assertTrue(
                slot.localToStageCoordinates(new Vector2(0f, slot.getHeight())).y <= size[1]);
            Actor hint = actor("consumable-use-" + i);
            Vector2 hintPosition = hint.localToStageCoordinates(new Vector2());
            assertTrue(hintPosition.y >= 0f);
            assertTrue(
                hint.localToStageCoordinates(new Vector2(0f, hint.getHeight())).y <= bottom.y,
                "Use hint stays below slot");
          }
          Actor icon = actor("consumable-icon-1");
          Actor first = actor("consumable-slot-1");
          assertTrue(icon.getWidth() < first.getWidth(), "Icon stays within its frame");
        }
      }
    } finally {
      weaponUi.dispose();
      weaponPlayer.dispose();
    }
  }

  @Test
  void timerRingScalesInsideSmallSlots() {
    inventory.addConsumable(ItemIds.SPEED_POTION, 2);
    input.keyDown(Keys.Q);
    Actor ring = actor("speed-potion-timer-ring-1");
    ring.setSize(42f, 42f);
    Batch batch = mock(Batch.class);
    ring.draw(batch, 1f);
    assertEquals(48, drawCalls(batch));
    mockingDetails(batch).getInvocations().stream()
        .filter(inv -> inv.getMethod().getName().equals("draw"))
        .forEach(
            inv -> {
              float x = inv.getArgument(1);
              float y = inv.getArgument(2);
              assertTrue(x >= 0f && x < 42f, "Timer arc must stay inside resized slot");
              assertTrue(y >= 0f && y < 42f, "Timer arc must stay inside resized slot");
            });
  }

  @Test
  void disposingHudRemovesActorsAndLeavesSharedAssetsAvailableForReplacement() {
    ui.dispose();
    ui = null;
    assertNull(stage.getRoot().findActor("consumable-hotbar"));
    assertDoesNotThrow(() -> inventory.addConsumable(ItemIds.SHIELD));
    assertDoesNotThrow(() -> input.keyDown(Keys.TAB));
    assertNotNull(resources.getAsset("images/consumable-slot-idle.png", Texture.class));
    ui = new Entity().addComponent(new ConsumableHotbarDisplay(player));
    ui.create();
    assertEquals(1, stage.getActors().size);
    assertEquals("1", count("1").getText().toString());
  }

  @Test
  void pickupOrderStacksDuplicatesAndReusesEmptySlotsWithoutMovingOtherItems() {
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.STRENGTH_POTION);
    inventory.addConsumable(ItemIds.SHIELD);
    Image first = actor("consumable-icon-1");
    Image second = actor("consumable-icon-2");
    assertSame(
        resources.getAsset("images/shield_consumable_pixel.png", Texture.class),
        ((TextureRegionDrawable) first.getDrawable()).getRegion().getTexture());
    assertSame(
        resources.getAsset("images/strength_potion_pixel.png", Texture.class),
        ((TextureRegionDrawable) second.getDrawable()).getRegion().getTexture());
    assertEquals("2", count("1").getText().toString());
    assertFalse(actor("consumable-icon-3").isVisible());
    input.keyDown(Keys.Q);
    assertEquals(1, inventory.getConsumableCount(ItemIds.SHIELD));
    input.keyDown(Keys.Q);
    assertFalse(first.isVisible());
    assertTrue(second.isVisible());
    inventory.addConsumable(ItemIds.SPEED_POTION);
    assertSame(
        resources.getAsset("images/speed_potion_pixel.png", Texture.class),
        ((TextureRegionDrawable) first.getDrawable()).getRegion().getTexture());
    input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    assertEquals(0, inventory.getConsumableCount(ItemIds.STRENGTH_POTION));
    assertEquals(1, inventory.getConsumableCount(ItemIds.SPEED_POTION));
    assertEquals(
        8000,
        player
            .getComponent(ConsumableEffectComponent.class)
            .getRemainingMs(ItemIds.STRENGTH_POTION));
    assertFalse(actor("speed-potion-timer-ring-2").isVisible());
  }

  @Test
  void healingSizesShowTheirOwnTexturesWithoutTheOldBottleCrop() {
    String[] ids = {
      ItemIds.HEALTH_POTION, ItemIds.MEDIUM_HEALTH_POTION, ItemIds.LARGE_HEALTH_POTION
    };
    String[] tiers = {"small", "medium", "large"};
    int[] widths = {370, 743, 928};
    int[] heights = {727, 1007, 1024};
    for (int i = 0; i < ids.length; i++) {
      inventory.addConsumable(ids[i]);
      Image icon = actor("consumable-icon-" + (i + 1));
      var region = ((TextureRegionDrawable) icon.getDrawable()).getRegion();
      assertSame(
          resources.getAsset("images/health_potion_" + tiers[i] + "_pixel.png", Texture.class),
          region.getTexture());
      assertEquals(widths[i], region.getRegionWidth());
      assertEquals(heights[i], region.getRegionHeight());
    }
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

  @Test
  void fourthSlotCanBeAssignedSelectedAndConsumedThroughQ() {
    player.getComponent(CombatStatsComponent.class).setHealth(50);
    inventory.addConsumable(ItemIds.HEALTH_POTION, 2);
    inventory.equipConsumable(ItemIds.HEALTH_POTION, 3);
    assertFalse(actor("consumable-icon-1").isVisible());
    assertTrue(actor("consumable-icon-4").isVisible());
    assertEquals("2", count("4").getText().toString());
    for (int i = 0; i < 3; i++) input.keyDown(Keys.TAB);
    assertTrue(actor("consumable-pointer-4").isVisible());
    input.keyDown(Keys.Q);
    assertEquals(75, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals("1", count("4").getText().toString());
  }
}
