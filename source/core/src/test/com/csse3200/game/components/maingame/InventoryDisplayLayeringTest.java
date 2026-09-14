package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponAssetsComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
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
    ResourceService resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    // The hotbar now reads the player's equipment and borrows its loaded weapon textures.
    // Build only those dependencies rather than a full player with physics and movement.
    Entity player =
        new Entity()
            .addComponent(new WeaponAssetsComponent())
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(new WeaponSelectionComponent());
    ServiceLocator.getEntityService().register(player);
    InventoryDisplay display = new InventoryDisplay(player);
    Entity ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    try {
      // Find the book by name because the stage also contains the persistent hotbar.
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
        assertTrue(book.isVisible());
        assertTrue(book.getZIndex() > enemyOverlay.getZIndex());

        // Page switching rebuilds the book; the replacement must also cover the health bar.
        display.changePage();
        // The hotbar is brought to the front during page changes, so stage order cannot
        // identify the replacement book. Resolve the new book actor by its stable name.
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
      // Remove the UI before releasing the player-owned textures it borrows.
      ui.dispose();
      player.dispose();
      resources.dispose();
      stage.dispose();
    }
  }
}
