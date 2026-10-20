package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks hotbar rendering independently and its integration with the inventory book. */
@ExtendWith(GameExtension.class)
class HotbarDisplayTest {
  private Stage stage;
  private ResourceService resources;
  private WeaponSelectionComponent selection;
  private Entity player;
  private Entity ui;
  private HotbarDisplay display;
  private Table hotbar;

  @BeforeEach
  void setUp() {
    // GameExtension supplies a headless libGDX environment and mocked OpenGL calls.
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    // Registering the UI entity creates the hotbar actors without needing an inventory.
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    selection = new WeaponSelectionComponent();
    player =
        new Entity()
            .addComponent(new WeaponAssetsComponent())
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(selection);
    ServiceLocator.getEntityService().register(player);
    display = new HotbarDisplay(player);
    ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    hotbar = stage.getRoot().findActor("inventory-hotbar");
  }

  @AfterEach
  void tearDown() {
    // Release the UI before its player-owned textures, even if a test assertion fails.
    if (ui != null) {
      ui.dispose();
    }
    player.dispose();
    resources.dispose();
    stage.dispose();
  }

  @Test
  void hotbarStartsVisibleWithOrderedItemIconsAndLabels() {
    // The hotbar works without an inventory and must not intercept mouse/touch input.
    assertNotNull(hotbar);
    assertTrue(hotbar.isVisible());
    assertEquals(Touchable.disabled, hotbar.getTouchable());
    assertNull(stage.getRoot().findActor("inventory-book"));
    for (int i = 0; i < 3; i++) {
      Image icon = hotbar.findActor("weapon-icon-" + (i + 1));
      Texture expected =
          resources.getAsset(selection.getWeapons().get(i).getTexture(), Texture.class);
      assertSame(expected, ((TextureRegionDrawable) icon.getDrawable()).getRegion().getTexture());
      Label label = hotbar.findActor("weapon-key-" + (i + 1));
      assertEquals(Integer.toString(i + 1), label.getText().toString());
    }
  }

  @Test
  void highlightsInitialSwordAndUpdatesWhenSelectionChanges() {
    Image swordFrame = hotbar.findActor("weapon-frame-1");
    Image knifeFrame = hotbar.findActor("weapon-frame-2");
    Image bowFrame = hotbar.findActor("weapon-frame-3");
    assertEquals(Color.WHITE, swordFrame.getColor());
    assertEquals(Color.GRAY, knifeFrame.getColor());
    assertEquals(Color.GRAY, bowFrame.getColor());
    selection.equip(WeaponType.BOW);
    assertEquals(Color.GRAY, swordFrame.getColor());
    assertEquals(Color.WHITE, bowFrame.getColor());
  }

  @Test
  void hotbarPersistsAcrossBookVisibilityAndPageChanges() {
    InventoryDisplay bookDisplay = new InventoryDisplay(mock(InventoryComponent.class));
    Entity bookUi =
        new Entity().addComponent(bookDisplay).addComponent(new InventoryActions(bookDisplay));
    ServiceLocator.getEntityService().register(bookUi);
    try {
      Image knifeFrame = hotbar.findActor("weapon-frame-2");
      Image bowFrame = hotbar.findActor("weapon-frame-3");
      // Exercise page changes with the book both open and closed. Repeating the sequence catches
      // abandoned book tables accumulating on the stage when a page is rebuilt.
      for (int i = 0; i < 4; i++) {
        // Change away from the knife each time so every rebuilt page tests a new selection event.
        selection.equip(WeaponType.BOW);
        bookDisplay.setVisible(true);
        bookUi.getEvents().trigger("nextPage");
        assertTrue(stage.getRoot().findActor("inventory-book").isVisible());
        bookDisplay.setVisible(false);
        bookUi.getEvents().trigger("nextPage");
        assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
        assertTrue(hotbar.isVisible());
        selection.equip(WeaponType.DAGGER);
        assertEquals(Color.WHITE, knifeFrame.getColor());
        assertEquals(Color.GRAY, bowFrame.getColor());
        // Only two top-level actors belong in this isolated stage: the book and the hotbar.
        // Their nested slot images and page contents do not count as top-level actors.
        assertEquals(2, stage.getActors().size, "Page changes must not leak stage actors");
      }
    } finally {
      bookUi.dispose();
    }
    assertTrue(hotbar.isVisible(), "Disposing the book must not remove the hotbar");
    assertEquals(1, stage.getActors().size);
  }

  @Test
  void resizingKeepsOnlyThreeWeaponSlotsWithFittedIcons() {
    // Resize the same UI from a narrow window to wider ones at a fixed height of 706 pixels.
    // Reusing it also checks that cached sizes update, rather than only testing fresh layouts.
    for (int width : new int[] {906, 1280, 1918}) {
      stage.getViewport().update(width, 706, true);
      // No render loop is running, so explicitly invalidate cached layout and recalculate it.
      hotbar.invalidateHierarchy();
      hotbar.validate();
      Table left = (Table) hotbar.getChildren().get(0);
      assertEquals(3, left.getChildren().size);
      Image frame = hotbar.findActor("weapon-frame-1");
      Texture frameTexture = ((TextureRegionDrawable) frame.getDrawable()).getRegion().getTexture();
      assertEquals(3, countFrames(hotbar, frameTexture), "Unused spell frames must be removed");
      assertEquals(340f, left.getX(), 0.1f);
      assertEquals(48f, left.getY(), 0.1f);
      for (Actor circle : left.getChildren()) {
        assertTrue(circle.getWidth() > 0f && circle.getWidth() <= 96f);
        assertEquals(circle.getWidth(), circle.getHeight(), 1f);
      }
      Image icon = hotbar.findActor("weapon-icon-1");
      assertEquals(left.getChildren().first().getWidth() * 0.55f, icon.getWidth(), 1f);
    }
  }

  private int countFrames(Actor actor, Texture texture) {
    if (actor instanceof Image image
        && image.getDrawable() instanceof TextureRegionDrawable drawable
        && drawable.getRegion().getTexture() == texture) {
      return 1;
    }
    int count = 0;
    if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
      for (Actor child : group.getChildren()) count += countFrames(child, texture);
    }
    return count;
  }

  @Test
  void disposingDisplayRemovesActorsButKeepsPlayerOwnedTextures() {
    // Hotbar disposal removes its table, but shared weapon textures belong to the player.
    ui.dispose();
    ui = null; // Already disposed; teardown still releases the player, resources and stage.
    assertEquals(0, stage.getActors().size);
    assertDoesNotThrow(
        () -> selection.equip(WeaponType.BOW),
        "A late selection event must be harmless after the hotbar is disposed");
    // Disposing the display must not unload the player-owned weapon textures.
    assertNotNull(resources.getAsset(selection.getWeapons().get(0).getTexture(), Texture.class));
  }
}
