package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
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
  void disposingBookRemovesItsActor() {
    ui.dispose();
    ui = null;
    assertEquals(0, stage.getActors().size);
  }
}
