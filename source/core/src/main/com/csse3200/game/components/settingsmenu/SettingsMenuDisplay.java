package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.files.GameProgress;
import com.csse3200.game.files.GameProgress.SaveData;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.files.WindowSize;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.ui.UiScale;
import com.csse3200.game.utils.StringDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings menu display and logic. If you bork the settings, they can be changed manually in
 * CSSE3200Game/settings.json under your home directory (This is C:/users/[username] on Windows).
 */
public class SettingsMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(SettingsMenuDisplay.class);
  private final GdxGame game;
  private final Runnable onClose;
  private final boolean scaleStage;
  private boolean menuOpen;

  private Table rootTable;
  private Table actionsTable;
  private Table backdrop;
  private final Array<Actor> hiddenHud = new Array<>();
  private InputComponent gameplayKeys;
  private TextField fpsText;
  private CheckBox fullScreenCheck;
  private CheckBox vsyncCheck;
  private Slider uiScaleSlider;
  private SelectBox<StringDecorator<DisplayMode>> displayModeSelect;
  private SelectBox<WindowSize.Preset> windowSizeSelect;
  private CheckBox showTimerCheck;
  private CheckBox showFpsCheck;
  private CheckBox muteUnfocusedCheck;
  private Slider musicSlider;
  private Slider soundSlider;
  private Label lastRunLabel;
  private Label bestRunLabel;
  private Label resetStatusLabel;
  private WindowSize.Preset loadedWindow;
  private StringDecorator<DisplayMode> loadedResolution;

  public SettingsMenuDisplay(GdxGame game) {
    this(game, null, false);
  }

  /**
   * @param onClose runs instead of leaving for the main menu. Used by the in-game overlay.
   * @param scaleStage when true, the slider scales the whole stage so the in-game HUD follows it
   */
  public SettingsMenuDisplay(GdxGame game, Runnable onClose, boolean scaleStage) {
    super();
    this.game = game;
    this.onClose = onClose;
    this.scaleStage = scaleStage;
    this.menuOpen = onClose == null;
  }

  /** Shows the menu over the current screen and pauses the world. */
  public void open() {
    loadFromSavedSettings();
    menuOpen = true;
    setMenuVisible(true);
    showScaleOnSettingsPage(1f);
    if (scaleStage) {
      hideGameUi();
    }
    ServiceLocator.getEntityService().setFrozen(this, true);
    if (uiScaleSlider != null) {
      applyUiScale(uiScaleSlider.getValue());
    }
  }

  @Override
  public void create() {
    super.create();
    addActors();
    if (scaleStage) {
      gameplayKeys = new GameplayKeyBlocker();
      gameplayKeys.setEntity(entity);
      gameplayKeys.create();
    }
  }

  private void addActors() {
    Label title = new Label("Settings", skin, "title");
    Label hint =
        new Label("Apply saves these options. Exit leaves them unchanged.", skin, "caption");
    Table settingsTable = makeSettingsTable();
    ScrollPane pane = new ScrollPane(settingsTable, skin);
    pane.setFadeScrollBars(false);
    pane.setScrollingDisabled(true, false);
    pane.setCancelTouchFocus(false);
    Table menuBtns = makeMenuBtns();

    backdrop = new Table();
    backdrop.setName(UiScale.SETTINGS_BACKDROP);
    backdrop.setFillParent(true);
    backdrop.setBackground(skin.getDrawable("window-w"));
    backdrop.setTouchable(Touchable.enabled);

    rootTable = new Table();
    rootTable.setName(UiScale.SETTINGS_CONTENT);
    rootTable.setFillParent(true);
    rootTable.top();

    rootTable.add(title).expandX().top().padTop(16f);
    rootTable.row().padTop(6f);
    rootTable.add(hint).expandX().top();
    rootTable.row().padTop(12f);
    rootTable.add(pane).grow().pad(8f, 48f, 72f, 48f);

    actionsTable = menuBtns;
    actionsTable.setName(UiScale.SETTINGS_ACTIONS);
    actionsTable.setFillParent(true);
    actionsTable.bottom();

    stage.addActor(backdrop);
    stage.addActor(rootTable);
    stage.addActor(actionsTable);
    if (menuOpen) {
      applyUiScale(uiScaleSlider.getValue());
      actionsTable.toFront();
    } else {
      setMenuVisible(false);
    }
  }

  private void setMenuVisible(boolean visible) {
    if (backdrop != null) {
      backdrop.setVisible(visible);
      if (visible) {
        backdrop.toFront();
      }
    }
    if (rootTable != null) {
      rootTable.setVisible(visible);
      if (visible) {
        rootTable.toFront();
      }
    }
    if (actionsTable != null) {
      actionsTable.setVisible(visible);
      if (visible) {
        actionsTable.toFront();
      }
    }
  }

  private Table makeSettingsTable() {
    UserSettings.Settings settings = UserSettings.get();
    SettingsForm form = new SettingsForm(skin);

    fpsText = new TextField(Integer.toString(settings.fps), readableField());
    fullScreenCheck = new CheckBox("", skin);
    fullScreenCheck.setChecked(settings.fullscreen);
    vsyncCheck = new CheckBox("", skin);
    vsyncCheck.setChecked(settings.vsync);
    uiScaleSlider = new Slider(UiScale.MIN, UiScale.MAX, 0.1f, false, skin);
    uiScaleSlider.setValue(UiScale.clamp(settings.uiScale));
    displayModeSelect = readableSelect();
    Monitor selectedMonitor = Gdx.graphics.getMonitor();
    displayModeSelect.setItems(getDisplayModes(selectedMonitor));
    displayModeSelect.setSelected(getActiveMode(displayModeSelect.getItems()));

    windowSizeSelect = readableSelect();
    windowSizeSelect.setItems(new Array<>(WindowSize.presets()));
    windowSizeSelect.setSelected(WindowSize.matching(settings.windowWidth, settings.windowHeight));
    rememberLoadedSelection();

    form.section("Display", "Window size is used when fullscreen is off.");
    form.row("FPS cap", fpsText);
    form.row("Fullscreen", fullScreenCheck);
    form.row("VSync", vsyncCheck);
    form.slider("UI scale", uiScaleSlider, "%.2fx");
    form.row("Resolution", displayModeSelect);
    form.row("Window size", windowSizeSelect);

    musicSlider = new Slider(0f, 1f, 0.05f, false, skin);
    musicSlider.setValue(settings.musicVolume);
    soundSlider = new Slider(0f, 1f, 0.05f, false, skin);
    soundSlider.setValue(settings.soundVolume);
    muteUnfocusedCheck = new CheckBox("", skin);
    muteUnfocusedCheck.setChecked(settings.muteUnfocused);
    form.section(
        "Audio", "Music during a run follows Apply. Effects use the new volume on the next sound.");
    form.percent("Music", musicSlider);
    form.percent("Effects", soundSlider);
    form.row("Mute in background", muteUnfocusedCheck);

    showTimerCheck = new CheckBox("", skin);
    showTimerCheck.setChecked(settings.showTimer);
    showFpsCheck = new CheckBox("", skin);
    showFpsCheck.setChecked(settings.showFps);
    form.section("Gameplay", "Timer and FPS appear on the run HUD.");
    form.row("Show run timer", showTimerCheck);
    form.row("Show FPS", showFpsCheck);

    SaveData save = GameProgress.get();
    lastRunLabel = new Label(GameProgress.formatTime(save.lastRunMs), skin);
    bestRunLabel = new Label(GameProgress.formatTime(save.bestRunMs), skin);
    resetStatusLabel = new Label("", skin);
    form.section(
        "Run history",
        "Last and best time on this computer. Achievements stay with save and load.");
    form.row("Last run", lastRunLabel);
    form.row("Best run", bestRunLabel);
    form.row("Saved on this computer", resetStatusLabel);
    form.row("", resetTimesButton());

    return form.table();
  }

  private TextButton resetTimesButton() {
    TextButton resetSaveBtn = new TextButton("Reset times", skin);
    resetSaveBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            GameProgress.clearSave();
            refreshProgressLabel();
            resetStatusLabel.setText("Run times cleared.");
          }
        });
    return resetSaveBtn;
  }

  private StringDecorator<DisplayMode> getActiveMode(Array<StringDecorator<DisplayMode>> modes) {
    DisplayMode active = Gdx.graphics.getDisplayMode();

    for (StringDecorator<DisplayMode> stringMode : modes) {
      DisplayMode mode = stringMode.object;
      if (active.width == mode.width
          && active.height == mode.height
          && active.refreshRate == mode.refreshRate) {
        return stringMode;
      }
    }
    return null;
  }

  private Array<StringDecorator<DisplayMode>> getDisplayModes(Monitor monitor) {
    DisplayMode[] displayModes = Gdx.graphics.getDisplayModes(monitor);
    Array<StringDecorator<DisplayMode>> arr = new Array<>();

    for (DisplayMode displayMode : displayModes) {
      arr.add(new StringDecorator<>(displayMode, this::prettyPrint));
    }

    return arr;
  }

  private String prettyPrint(DisplayMode displayMode) {
    return displayMode.width + "x" + displayMode.height + ", " + displayMode.refreshRate + "hz";
  }

  /**
   * Text field style with black text and a black cursor. The shared skin draws white text on the
   * gold field, which cannot be read.
   *
   * @return a copy of the default field style
   */
  private TextField.TextFieldStyle readableField() {
    TextField.TextFieldStyle style =
        new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
    Color ink = skin.getColor("black");
    style.fontColor = ink;
    style.cursor = skin.newDrawable("white", ink);
    return style;
  }

  /**
   * Dropdown with black text. The shared skin draws white text on the gold box.
   *
   * @param <T> item type
   * @return an empty dropdown whose current value is black
   */
  private <T> SelectBox<T> readableSelect() {
    SelectBox.SelectBoxStyle style =
        new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
    style.fontColor = skin.getColor("black");
    return new SelectBox<>(style);
  }

  private Table makeMenuBtns() {
    TextButton exitBtn = new TextButton("Exit", skin);
    TextButton applyBtn = new TextButton("Apply", skin);

    exitBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Exit button clicked");
            exitMenu();
          }
        });

    applyBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Apply button clicked");
            applyChanges();
          }
        });

    Table table = new Table();
    table.add(exitBtn).expandX().left().pad(0f, 15f, 15f, 0f);
    table.add(applyBtn).expandX().right().pad(0f, 0f, 15f, 15f);
    return table;
  }

  /** Hides the run HUD so the settings page can be read. */
  private void hideGameUi() {
    if (stage == null) {
      return;
    }
    for (Actor actor : stage.getActors()) {
      if (actor == backdrop || actor == rootTable || actor == actionsTable || !actor.isVisible()) {
        continue;
      }
      actor.setVisible(false);
      hiddenHud.add(actor);
    }
  }

  private void showGameUi() {
    for (Actor actor : hiddenHud) {
      actor.setVisible(true);
    }
    hiddenHud.clear();
  }

  /**
   * Eats gameplay keys while the in-game menu is open. The stage still receives keys first, so the
   * FPS field can be typed in. Keys the field does not use, such as Q, stop here.
   */
  private final class GameplayKeyBlocker extends InputComponent {
    private GameplayKeyBlocker() {
      super(9);
    }

    private boolean blocksGameplayKeys() {
      return scaleStage && menuOpen;
    }

    @Override
    public boolean keyDown(int keycode) {
      return blocksGameplayKeys();
    }

    @Override
    public boolean keyUp(int keycode) {
      return blocksGameplayKeys();
    }

    @Override
    public boolean keyTyped(char character) {
      return blocksGameplayKeys();
    }
  }

  private void applyChanges() {
    UserSettings.Settings settings = UserSettings.get();

    Integer fpsVal = parseOrNull(fpsText.getText());
    if (fpsVal != null) {
      if (fpsVal != settings.fps) {
        vsyncCheck.setChecked(false);
      }
      settings.fps = fpsVal;
    }
    boolean windowChanged = windowSelectionChanged();
    boolean resolutionChanged = resolutionSelectionChanged();
    boolean fullscreen = fullScreenCheck.isChecked();
    if (windowChanged && !resolutionChanged) {
      fullscreen = false;
    } else if (resolutionChanged && !windowChanged) {
      fullscreen = true;
    }
    fullScreenCheck.setChecked(fullscreen);
    settings.fullscreen = fullscreen;
    DisplaySettings chosen = chosenDisplaySettings(displayModeSelect.getSelected());
    if (chosen != null) {
      settings.displayMode = chosen;
    }
    settings.vsync = vsyncCheck.isChecked();
    WindowSize.Preset window = windowSizeSelect.getSelected();
    if (window != null) {
      settings.windowWidth = window.width;
      settings.windowHeight = window.height;
    }
    settings.showTimer = showTimerCheck.isChecked();
    settings.showFps = showFpsCheck.isChecked();
    settings.musicVolume = musicSlider.getValue();
    settings.soundVolume = soundSlider.getValue();
    settings.muteUnfocused = muteUnfocusedCheck.isChecked();
    settings.uiScale = UiScale.clamp(uiScaleSlider.getValue());

    UserSettings.set(settings, true);
    rememberLoadedSelection();
    applyUiScale(settings.uiScale);
    showScaleOnSettingsPage(settings.uiScale);
  }

  /**
   * Scales the settings page itself so Apply shows the chosen UI scale. Opening or leaving the page
   * puts it back to normal size. The run HUD is scaled separately.
   *
   * @param scale multiplier from 0.5 to 2
   */
  private void showScaleOnSettingsPage(float scale) {
    if (rootTable != null) {
      UiScale.apply(rootTable, scale);
    }
  }

  /** Remembers the dropdowns currently shown, so a later Apply can tell what the player changed. */
  private void rememberLoadedSelection() {
    loadedWindow = windowSizeSelect.getSelected();
    loadedResolution = displayModeSelect.getSelected();
  }

  /**
   * True when the player picked a different window size from the one shown when the page opened.
   *
   * @return whether the window-size dropdown changed
   */
  private boolean windowSelectionChanged() {
    WindowSize.Preset selected = windowSizeSelect.getSelected();
    if (selected == null || loadedWindow == null) {
      return selected != loadedWindow;
    }
    return selected.width != loadedWindow.width || selected.height != loadedWindow.height;
  }

  /**
   * True when the player picked a different resolution from the one shown when the page opened.
   *
   * @return whether the resolution dropdown changed
   */
  private boolean resolutionSelectionChanged() {
    return !sameMode(loadedResolution, displayModeSelect.getSelected());
  }

  /**
   * True when two dropdown entries describe the same width, height, and refresh rate.
   *
   * @param first one resolution entry
   * @param second another resolution entry
   * @return whether the two modes match
   */
  private static boolean sameMode(
      StringDecorator<DisplayMode> first, StringDecorator<DisplayMode> second) {
    if (first == second) {
      return true;
    }
    if (first == null || second == null || first.object == null || second.object == null) {
      return false;
    }
    DisplayMode a = first.object;
    DisplayMode b = second.object;
    return a.width == b.width && a.height == b.height && a.refreshRate == b.refreshRate;
  }

  /**
   * Resolution to save, or null when the dropdown has no selection. A missing selection keeps the
   * display mode already stored in settings.
   */
  static DisplaySettings chosenDisplaySettings(StringDecorator<DisplayMode> selected) {
    if (selected == null || selected.object == null) {
      return null;
    }
    return new DisplaySettings(selected.object);
  }

  /** Scales the run HUD only. The settings page itself stays at normal size. */
  private void applyUiScale(float scale) {
    if (scaleStage) {
      UiScale.applyToStage(stage, scale);
    }
  }

  private void loadFromSavedSettings() {
    if (fpsText == null) {
      return;
    }
    UserSettings.Settings settings = UserSettings.get();
    fpsText.setText(Integer.toString(settings.fps));
    fullScreenCheck.setChecked(settings.fullscreen);
    vsyncCheck.setChecked(settings.vsync);
    uiScaleSlider.setValue(UiScale.clamp(settings.uiScale));
    displayModeSelect.setSelected(getActiveMode(displayModeSelect.getItems()));
    windowSizeSelect.setSelected(WindowSize.matching(settings.windowWidth, settings.windowHeight));
    showTimerCheck.setChecked(settings.showTimer);
    showFpsCheck.setChecked(settings.showFps);
    musicSlider.setValue(settings.musicVolume);
    soundSlider.setValue(settings.soundVolume);
    muteUnfocusedCheck.setChecked(settings.muteUnfocused);
    rememberLoadedSelection();
  }

  private void refreshProgressLabel() {
    SaveData save = GameProgress.get();
    if (lastRunLabel != null) {
      lastRunLabel.setText(GameProgress.formatTime(save.lastRunMs));
    }
    if (bestRunLabel != null) {
      bestRunLabel.setText(GameProgress.formatTime(save.bestRunMs));
    }
  }

  private void exitMenu() {
    if (onClose != null) {
      menuOpen = false;
      showScaleOnSettingsPage(1f);
      if (scaleStage) {
        UiScale.applyToStage(stage, UserSettings.get().uiScale);
        showGameUi();
      }
      if (stage != null) {
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
      }
      setMenuVisible(false);
      ServiceLocator.getEntityService().setFrozen(this, false);
      onClose.run();
      return;
    }
    game.setScreen(ScreenType.MAIN_MENU);
  }

  private Integer parseOrNull(String num) {
    try {
      return Integer.parseInt(num, 10);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void update() {
    if (!menuOpen) {
      return;
    }
    if (!scaleStage) {
      stage.act(ServiceLocator.getTimeSource().getDeltaTime());
    }
  }

  @Override
  public void dispose() {
    if (ServiceLocator.getEntityService() != null) {
      ServiceLocator.getEntityService().setFrozen(this, false);
    }
    showGameUi();
    if (gameplayKeys != null && ServiceLocator.getInputService() != null) {
      gameplayKeys.dispose();
    }
    if (backdrop != null) {
      backdrop.clear();
    }
    if (rootTable != null) {
      rootTable.clear();
    }
    if (actionsTable != null) {
      actionsTable.clear();
    }
    super.dispose();
  }
}
