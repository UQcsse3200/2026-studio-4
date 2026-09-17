package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks that opening the inventory covers actors added later, such as enemy health bars. */
@ExtendWith(GameExtension.class)
class InventoryDisplayLayeringTest {
  @Test
  void openingAndReopeningBookBringsItAboveEnemyOverlays() {
    // Use real Scene2D actors to check their drawing order, without opening a game window.
    // GameExtension provides headless libGDX; the mocked batch avoids actual rendering.
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    // The inventory no longer needs a player or weapon textures.
    InventoryDisplay display = new InventoryDisplay(mock(InventoryComponent.class));
    Entity ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    try {
      // Find the book by its stable actor name.
      Actor book = stage.getRoot().findActor("inventory-book");
      assertNotNull(book);
      assertFalse(book.isVisible());
      // A plain actor stands in for an enemy health bar: only its stage order matters here.
      Actor enemyOverlay = new Actor();
      // Repeat to check both the first opening and reopening after another overlay is added.
      for (int i = 0; i < 2; i++) {
        // Newly spawned enemies append their health bars after the existing inventory actor.
        // Among siblings on the stage, a larger actor Z-index means it is drawn on top.
        stage.addActor(enemyOverlay);
        assertTrue(enemyOverlay.getZIndex() > book.getZIndex());
        // Opening the inventory must both show the book and move it in front of the overlay.
        display.setVisible(true);
        book = stage.getRoot().findActor("inventory-book");
        assertTrue(book.isVisible());
        assertTrue(book.getZIndex() > enemyOverlay.getZIndex());

        // Page switching rebuilds the book; the replacement must also cover the health bar.
        display.changePage();
        // Resolve the rebuilt book by name rather than holding the removed page actor.
        book = stage.getRoot().findActor("inventory-book");
        assertNotNull(book);
        assertNotSame(enemyOverlay, book);
        assertTrue(book.isVisible());
        assertTrue(book.getZIndex() > enemyOverlay.getZIndex());
        // Closing the book should not hide the enemy's health bar as a side effect.
        display.setVisible(false);
        assertFalse(book.isVisible());
        assertTrue(enemyOverlay.isVisible());
        // Remove our stand-in so the next iteration can append it as a newly created overlay.
        enemyOverlay.remove();
      }
    } finally {
      // Remove the book and release the headless stage even if an assertion fails.
      ui.dispose();
      stage.dispose();
    }
  }
}
