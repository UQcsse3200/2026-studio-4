package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameProgress;
import com.csse3200.game.files.GameProgress.SaveData;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Victory stats follow the settings checkbox and the saved run. */
@ExtendWith(GameExtension.class)
class VictoryScreenTest {
  private Settings originalSettings;
  private SaveData originalSave;

  @BeforeEach
  void rememberFiles() {
    originalSettings = UserSettings.get();
    originalSave = GameProgress.get();

    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
  }

  @AfterEach
  void restoreFiles() {
    UserSettings.set(originalSettings, false);
    GameProgress.set(originalSave);
  }

  @Test
  void showsLastBestAndAchievementCountWhenEnabled() throws Exception {
    Settings settings = copy(originalSettings);
    settings.showVictoryStats = true;
    UserSettings.set(settings, false);

    GameProgress.recordRun(90_000L);
    GameProgress.recordRun(30_000L);
    GameProgress.unlock("boss");

    List<String> labels = labels(createDisplay());
    assertTrue(labels.contains("Victory"));
    assertTrue(labels.contains("Last run: 0:30"));
    assertTrue(labels.contains("Best run: 0:30"));
    assertTrue(labels.contains("Achievements: 1"));
  }

  @Test
  void hidesRunStatsWhenVictoryStatsAreOff() throws Exception {
    Settings settings = copy(originalSettings);
    settings.showVictoryStats = false;
    UserSettings.set(settings, false);
    GameProgress.recordRun(15_000L);
    GameProgress.unlock("boss");

    List<String> labels = labels(createDisplay());
    assertTrue(labels.contains("Victory"));
    assertFalse(labels.stream().anyMatch(text -> text.startsWith("Last run:")));
    assertFalse(labels.stream().anyMatch(text -> text.startsWith("Best run:")));
    assertFalse(labels.stream().anyMatch(text -> text.startsWith("Achievements:")));
  }

  private static Settings copy(Settings source) {
    Settings copy = new Settings();
    copy.fps = source.fps;
    copy.fullscreen = source.fullscreen;
    copy.vsync = source.vsync;
    copy.uiScale = source.uiScale;
    copy.displayMode = source.displayMode;
    copy.showTimer = source.showTimer;
    copy.showVictoryStats = source.showVictoryStats;
    return copy;
  }

  private static UIComponent createDisplay() throws Exception {
    Class<?> type = Class.forName("com.csse3200.game.screens.VictoryScreen$VictoryDisplay");
    Constructor<?> ctor = type.getDeclaredConstructor(GdxGame.class);
    ctor.setAccessible(true);
    UIComponent display = (UIComponent) ctor.newInstance(mock(GdxGame.class));
    new Entity().addComponent(display).create();
    return display;
  }

  private static List<String> labels(UIComponent display) throws Exception {
    Field tableField = display.getClass().getDeclaredField("table");
    tableField.setAccessible(true);
    Table table = (Table) tableField.get(display);
    List<String> texts = new ArrayList<>();
    for (Actor actor : table.getChildren()) {
      if (actor instanceof Label label) {
        texts.add(label.getText().toString());
      }
    }
    return texts;
  }
}
