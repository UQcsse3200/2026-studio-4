package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FpsOverlayTest {
  private Settings original;
  private FpsOverlay overlay;

  @BeforeEach
  void setUp() {
    original = UserSettings.get();
    Settings hidden = new Settings();
    hidden.showFps = false;
    UserSettings.set(hidden, false);

    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    overlay = new FpsOverlay();
    new Entity().addComponent(overlay).create();
  }

  @AfterEach
  void restoreSettings() {
    UserSettings.set(original, false);
  }

  @Test
  void createsTheCounterWhenShowFpsIsApplied() throws Exception {
    assertNull(table());

    Settings shown = new Settings();
    shown.showFps = true;
    UserSettings.set(shown, false);
    overlay.update();

    Table created = table();
    assertNotNull(created);
    assertTrue(created.isVisible());

    shown.showFps = false;
    UserSettings.set(shown, false);
    overlay.update();
    assertFalse(created.isVisible());
  }

  private Table table() throws Exception {
    Field field = FpsOverlay.class.getDeclaredField("table");
    field.setAccessible(true);
    return (Table) field.get(overlay);
  }
}
