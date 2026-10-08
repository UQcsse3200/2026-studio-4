package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.maingame.MainGameExitDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Headless check that a run can open settings, change them, and stay paused until the menu closes.
 */
@ExtendWith(GameExtension.class)
class InGameSettingsTest {
  private Stage stage;
  private Entity ui;
  private Entity world;
  private Settings originalSettings;

  @BeforeEach
  void setUp() {
    originalSettings = UserSettings.get();
    Graphics graphics = mock(Graphics.class);
    Monitor monitor = mock(Monitor.class);
    DisplayMode mode = new CustomDisplayMode(1280, 800, 60, 0);
    when(graphics.getWidth()).thenReturn(1280);
    when(graphics.getHeight()).thenReturn(800);
    when(graphics.getBackBufferWidth()).thenReturn(1280);
    when(graphics.getBackBufferHeight()).thenReturn(800);
    when(graphics.getDensity()).thenReturn(1f);
    when(graphics.getMonitor()).thenReturn(monitor);
    when(graphics.getDisplayMode()).thenReturn(mode);
    when(graphics.getDisplayModes(monitor)).thenReturn(new DisplayMode[] {mode});
    when(graphics.getDisplayModes()).thenReturn(new DisplayMode[] {mode});
    Gdx.graphics = graphics;

    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 800, true);
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    if (world != null) {
      world.dispose();
    }
    if (stage != null) {
      stage.dispose();
    }
    if (originalSettings != null) {
      UserSettings.set(originalSettings, false);
    }
  }

  @Test
  void openingSettingsPausesTheWorldUntilExit() {
    int[] ticks = {0};
    world =
        new Entity()
            .addComponent(
                new Component() {
                  @Override
                  public void update() {
                    ticks[0]++;
                  }
                });
    ServiceLocator.getEntityService().register(world);

    boolean[] closed = {false};
    SettingsMenuDisplay menu =
        new SettingsMenuDisplay(mock(GdxGame.class), () -> closed[0] = true, true);
    ui = new Entity().addComponent(menu);
    ui.setUpdatesWhilePaused(true);
    ServiceLocator.getEntityService().register(ui);

    Actor menuRoot = stage.getActors().first();
    assertFalse(menuRoot.isVisible());
    assertFalse(ServiceLocator.getEntityService().isFrozen());

    ServiceLocator.getEntityService().update();
    assertEquals(1, ticks[0]);

    menu.open();
    assertTrue(menuRoot.isVisible());
    assertTrue(ServiceLocator.getEntityService().isFrozen());
    ServiceLocator.getEntityService().update();
    assertEquals(1, ticks[0]);

    Slider scaleSlider = findSlider(stage.getRoot());
    assertNotNull(scaleSlider);
    float changed = Math.abs(originalSettings.uiScale - 1.4f) < 0.05f ? 0.6f : 1.4f;
    scaleSlider.setValue(changed);
    menu.update();
    assertEquals(changed, stage.getRoot().getScaleX(), 0.001f);

    findButton(stage.getRoot(), "Apply").fire(new ChangeListener.ChangeEvent());
    assertEquals(changed, UserSettings.get().uiScale, 0.001f);
    assertTrue(ServiceLocator.getEntityService().isFrozen());

    findButton(stage.getRoot(), "Exit").fire(new ChangeListener.ChangeEvent());
    assertTrue(closed[0]);
    assertFalse(menuRoot.isVisible());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    ServiceLocator.getEntityService().update();
    assertEquals(2, ticks[0]);
  }

  @Test
  void settingsButtonOpensTheOverlay() {
    boolean[] opened = {false};
    MainGameExitDisplay exit =
        new MainGameExitDisplay(() -> {}, () -> {}, () -> {}, () -> opened[0] = true);
    ui = new Entity().addComponent(exit);
    ServiceLocator.getEntityService().register(ui);

    TextButton settings = findButton(stage.getRoot(), "Settings");
    assertNotNull(settings);
    settings.fire(new ChangeListener.ChangeEvent());
    assertTrue(opened[0]);
  }

  private static Slider findSlider(Actor actor) {
    if (actor instanceof Slider slider && slider.getMaxValue() > 1f) {
      return slider;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        Slider found = findSlider(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static TextButton findButton(Actor actor, String text) {
    if (actor instanceof TextButton button && text.equals(button.getText().toString())) {
      return button;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        TextButton found = findButton(child, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static class CustomDisplayMode extends DisplayMode {
    private CustomDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
