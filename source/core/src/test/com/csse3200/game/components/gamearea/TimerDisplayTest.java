package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.Settings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the HUD panel's label text against a real {@link RunTimer} driven by a mocked {@link
 * GameTime}, plus visibility toggling and the T-key input handler.
 */
@ExtendWith(GameExtension.class)
class TimerDisplayTest {
  private GameTime gameTime;
  private RunTimer runTimer;
  private TimerDisplay display;
  private Entity ui;
  private EntityService entityService;

  @BeforeEach
  void setUp() {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    gameTime = mock(GameTime.class);
    runTimer = new RunTimer(gameTime);
    ServiceLocator.registerRunTimer(runTimer);

    display = new TimerDisplay();
    ui = new Entity().addComponent(display);
    ui.create();
  }

  private void tick(float delta) {
    when(gameTime.getDeltaTime()).thenReturn(delta);
    runTimer.update();
  }

  /**
   * Advances the timer by a total amount of time, split across several sub-frame ticks no larger
   * than RunTimer's clamp, mirroring how a real game loop accumulates time across many frames.
   */
  private void advance(float totalSeconds) {
    final float step = 0.2f; // comfortably under RunTimer's clamp
    float remaining = totalSeconds;
    while (remaining > step) {
      tick(step);
      remaining -= step;
    }
    if (remaining > 0f) {
      tick(remaining);
    }
  }

  @Test
  void createsPanelWithDefaultLabels() {
    assertEquals("00:00", label("runTime").getText().toString());
    assertEquals("--:--", label("dungeonTime").getText().toString());
    assertEquals("DUNGEON", label("dungeonCaption").getText().toString());
  }

  @Test
  void drawShowsRunTimeOnceRunStarts() {
    runTimer.startRun();
    advance(75f); // 1 minute 15 seconds
    display.draw(mock(SpriteBatch.class));
    assertEquals("01:15", label("runTime").getText().toString());
  }

  @Test
  void drawShowsPlaceholderWhenNoDungeonActive() {
    runTimer.startRun();
    tick(5f);
    display.draw(mock(SpriteBatch.class));
    assertEquals("--:--", label("dungeonTime").getText().toString());
    assertEquals("DUNGEON", label("dungeonCaption").getText().toString());
  }

  @Test
  void drawShowsDungeonTimeAndLabelWhenDungeonActive() {
    runTimer.startRun();
    advance(10f);
    runTimer.startDungeon("dungeonOne");
    advance(42f);
    display.draw(mock(SpriteBatch.class));
    assertEquals("00:42", label("dungeonTime").getText().toString());
    assertEquals("DUNGEON: 1", label("dungeonCaption").getText().toString());
  }

  @Test
  void dungeonCaptionMapsEachKnownDungeonId() {
    runTimer.startRun();
    assertDungeonLabel("dungeonOne", "DUNGEON: 1");
    assertDungeonLabel("dungeonTwo", "DUNGEON: 2");
    assertDungeonLabel("dungeonThree", "DUNGEON: 3");
    assertDungeonLabel("finalDungeon", "DUNGEON: END");
  }

  private void assertDungeonLabel(String dungeonId, String expectedCaption) {
    runTimer.stopDungeon();
    runTimer.startDungeon(dungeonId);
    tick(1f);
    display.draw(mock(SpriteBatch.class));
    assertEquals(expectedCaption, label("dungeonCaption").getText().toString());
  }

  @Test
  void drawBeforeCreateDoesNothing() {
    TimerDisplay fresh = new TimerDisplay();
    assertDoesNotThrow(() -> fresh.draw(mock(SpriteBatch.class)));
  }

  @Test
  void toggleFlipsPanelVisibility() {
    Table rootTable = field("rootTable", Table.class);
    assertTrue(rootTable.isVisible());
    display.toggle();
    assertFalse(rootTable.isVisible());
    display.toggle();
    assertTrue(rootTable.isVisible());
  }

  @Test
  void toggleInputTogglesOnTKeyOnly() {
    TimerDisplay.ToggleInput input = new TimerDisplay.ToggleInput(display);
    Table rootTable = field("rootTable", Table.class);
    assertTrue(rootTable.isVisible());

    assertFalse(input.keyDown(Input.Keys.A));
    assertTrue(rootTable.isVisible());

    assertTrue(input.keyDown(Input.Keys.T));
    assertFalse(rootTable.isVisible());
  }

  @Test
  void hidesPanelWhenShowTimerIsOff() {
    Settings original = UserSettings.get();
    Settings hidden = new Settings();
    hidden.fps = original.fps;
    hidden.fullscreen = original.fullscreen;
    hidden.vsync = original.vsync;
    hidden.uiScale = original.uiScale;
    hidden.displayMode = original.displayMode;
    hidden.showTimer = false;
    hidden.showVictoryStats = original.showVictoryStats;

    try {
      UserSettings.set(hidden, false);
      TimerDisplay hiddenDisplay = new TimerDisplay();
      new Entity().addComponent(hiddenDisplay).create();
      assertFalse(fieldOf(hiddenDisplay, "rootTable", Table.class).isVisible());
    } finally {
      UserSettings.set(original, false);
    }
  }

  @Test
  void disposeRemovesPanelAndMakesSubsequentDrawSafe() {
    assertDoesNotThrow(display::dispose);
    assertNull(field("rootTable", Table.class));
    assertDoesNotThrow(() -> display.draw(mock(SpriteBatch.class)));
  }

  private Label label(String fieldName) {
    return field(fieldName, Label.class);
  }

  private <T> T field(String fieldName, Class<T> type) {
    return fieldOf(display, fieldName, type);
  }

  /** Reads a private field via reflection since TimerDisplay exposes no public getters. */
  private static <T> T fieldOf(TimerDisplay target, String fieldName, Class<T> type) {
    try {
      Field f = TimerDisplay.class.getDeclaredField(fieldName);
      f.setAccessible(true);
      return type.cast(f.get(target));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not read field '" + fieldName + "' via reflection", e);
    }
  }
}
