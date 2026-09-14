package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the inventory and hotbar's layout and cleanup.
 */
@ExtendWith(GameExtension.class)
class InventoryDisplayTest {
  @Test
  void hotbarPersistsAcrossBookVisibilityPageChangesAndResize() {
    // GameExtension supplies a headless libGDX environment and mocked OpenGL calls.
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    // Registering the entity calls InventoryDisplay.create(), which adds the book and hotbar
    // to the stage. These services also support the component's normal disposal path below.
    InventoryDisplay display = new InventoryDisplay();
    Entity ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    try {
      // Initial state: the book is closed, while the decorative hotbar is visible and cannot
      // intercept mouse/touch input. Actor names let us find the UI without accessing private fields.
      Table hotbar = stage.getRoot().findActor("inventory-hotbar");
      assertNotNull(hotbar);
      assertTrue(hotbar.isVisible());
      assertEquals(Touchable.disabled, hotbar.getTouchable());
      assertFalse(stage.getRoot().findActor("inventory-book").isVisible());

      // Exercise page changes with the book both open and closed. Repeating the sequence catches
      // abandoned book tables accumulating on the stage when a page is rebuilt.
      for (int i = 0; i < 4; i++) {
        display.setVisible(true);
        display.changePage();
        assertTrue(stage.getRoot().findActor("inventory-book").isVisible());
        display.setVisible(false);
        display.changePage();
        assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
        assertTrue(hotbar.isVisible());
        // Only two top-level actors belong in this isolated stage: the book and the hotbar.
        // Their nested slot images and page contents do not count as top-level actors.
        assertEquals(2, stage.getActors().size, "Page changes must not leak stage actors");
      }

      // Resize the same UI from a narrow window to wider ones at a fixed height of 706 pixels.
      // Reusing it also checks that cached sizes update, rather than only testing fresh layouts.
      for (int width : new int[] {906, 1280, 1918}) {
        stage.getViewport().update(width, 706, true);
        // No render loop is running, so explicitly invalidate cached layout and recalculate it.
        hotbar.invalidateHierarchy();
        hotbar.validate();
        // The spacer between the groups is an empty table cell, not an actor, so the two groups
        // are children 0 and 1. Each group should contain three circle images.
        Table left = (Table) hotbar.getChildren().get(0);
        Table right = (Table) hotbar.getChildren().get(1);
        assertEquals(3, left.getChildren().size);
        assertEquals(3, right.getChildren().size);
        // Stage coordinates start at the bottom-left. The left group begins 340 pixels inward;
        // its top is y = 706 - 48 = 658, leaving room above it for the area title.
        assertEquals(340f, left.getX(), 0.1f);
        assertEquals(658f, left.getY() + left.getHeight(), 0.1f);
        assertTrue(
            right.getX() + right.getWidth() <= width - 96f,
            "Right group must leave room for Exit");
        assertTrue(
            right.getX() > left.getX() + left.getWidth(),
            "The two groups must remain separated on narrower windows");
        // At the widest size there is room for matching 340-pixel insets on both sides.
        // Narrower layouts may reduce the right inset, but must still clear Exit and the left group.
        if (width >= 1918) {
          assertEquals(width - 340f, right.getX() + right.getWidth(), 0.1f);
        }
        // Open and lay out the book too: its upper edge must remain below the hotbar's lower edge.
        display.setVisible(true);
        Table book = stage.getRoot().findActor("inventory-book");
        book.invalidateHierarchy();
        book.validate();
        // The first child is the stack containing the book cover and its pages.
        Actor cover = book.getChildren().first();
        assertTrue(
            cover.getY() + cover.getHeight() < left.getY(),
            "The open inventory book must remain below the hotbar");
        for (Actor circle : left.getChildren()) {
          // Full-size circles are 96 pixels. At width 906, the available width per circle is
          // (906 - 340 left inset - 96 right inset - 24 group gap - 24 slot gaps) / 6 = 70.333.
          // Allow one pixel of tolerance because Scene2D rounds fractional bounds to whole pixels.
          assertEquals(width >= 1280 ? 96f : 70.333f, circle.getWidth(), 1f);
        }
      }
    } finally {
      // Dispose even if an assertion fails. Entity disposal should remove both UI tables (and
      // dispose the hotbar texture); the actor-count assertion checks the stage cleanup specifically.
      ui.dispose();
      assertEquals(0, stage.getActors().size);
      stage.dispose();
    }
  }
}
