package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.ui.UiScale;
import com.csse3200.game.utils.StringDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings menu display and logic. If you bork the settings, they can be changed manually in
 * DECO2800Game/settings.json under your home directory (This is C:/users/[username] on Windows).
 */
public class SettingsMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(SettingsMenuDisplay.class);
  private final GdxGame game;
  private final Runnable onClose;
  private final boolean scaleStage;
  private boolean menuOpen;

  private Table rootTable;
  private Table actionsTable;
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
    ServiceLocator.getEntityService().setFrozen(this, true);
    if (uiScaleSlider != null) {
      applyUiScale(uiScaleSlider.getValue());
    }
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    Label title = new Label("Settings", skin, "title");
    Label hint =
        new Label("Apply saves these options. Exit leaves them unchanged.", skin, "caption");
    Table settingsTable = makeSettingsTable();
    ScrollPane pane = new ScrollPane(settingsTable, skin);
    pane.setFadeScrollBars(false);
    pane.setScrollingDisabled(true, false);
    Table menuBtns = makeMenuBtns();

    rootTable = new Table();
    rootTable.setName(UiScale.SETTINGS_CONTENT);
    rootTable.setFillParent(true);
    rootTable.top();

    rootTable.add(title).expandX().top().padTop(16f);
    rootTable.row().padTop(6f);
    rootTable.add(hint).expandX().top();
    rootTable.row().padTop(12f);
    rootTable.add(pane).grow().pad(8f, 48f, 8f, 48f);

    actionsTable = menuBtns;
    actionsTable.setName(UiScale.SETTINGS_ACTIONS);
    actionsTable.setFillParent(true);
    actionsTable.bottom();

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
    if (rootTable != null) {
      rootTable.setVisible(visible);
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

    fpsText = new TextField(Integer.toString(settings.fps), skin);
    fullScreenCheck = new CheckBox("", skin);
    fullScreenCheck.setChecked(settings.fullscreen);
    vsyncCheck = new CheckBox("", skin);
    vsyncCheck.setChecked(settings.vsync);
    uiScaleSlider = new Slider(0.2f, 2f, 0.1f, false, skin);
    uiScaleSlider.setValue(settings.uiScale);
    displayModeSelect = new SelectBox<>(skin);
    Monitor selectedMonitor = Gdx.graphics.getMonitor();
    displayModeSelect.setItems(getDisplayModes(selectedMonitor));
    displayModeSelect.setSelected(getActiveMode(displayModeSelect.getItems()));

    windowSizeSelect = new SelectBox<>(skin);
    windowSizeSelect.setItems(new Array<>(WindowSize.presets()));
    windowSizeSelect.setSelected(WindowSize.matching(settings.windowWidth, settings.windowHeight));

    form.section("Display", "Window size is used when fullscreen is off.");
    form.row("FPS cap", fpsText);
    form.row("Fullscreen", fullScreenCheck);
    form.row("VSync", vsyncCheck);
    uiScaleSlider.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            applyUiScale(uiScaleSlider.getValue());
          }
        });
    form.row("Resolution", displayModeSelect);
    form.row("Window size", windowSizeSelect);

    musicSlider = new Slider(0f, 1f, 0.05f, false, skin);
    musicSlider.setValue(settings.musicVolume);
    soundSlider = new Slider(0f, 1f, 0.05f, false, skin);
    soundSlider.setValue(settings.soundVolume);
    muteUnfocusedCheck = new CheckBox("", skin);
    muteUnfocusedCheck.setChecked(settings.muteUnfocused);
    form.section("Audio", "Effects follow the slider. Music follows it on the next track.");
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

    Label scaleValue = new Label(String.format("%.2fx", uiScaleSlider.getValue()), skin);
    uiScaleSlider.addListener(
        event -> {
          scaleValue.setText(String.format("%.2fx", uiScaleSlider.getValue()));
          return false;
        });
    Table scaleLine = new Table();
    scaleLine.add(new Label("UI scale", skin)).padRight(12f);
    scaleLine.add(uiScaleSlider).width(220f);
    scaleLine.add(scaleValue).padLeft(12f).width(72f);

    Table table = new Table();
    table.add(scaleLine).left().pad(0f, 15f, 15f, 0f);
    table.add(exitBtn).expandX().right().pad(0f, 15f, 15f, 8f);
    table.add(applyBtn).right().pad(0f, 0f, 15f, 15f);
    return table;
  }

  private void applyChanges() {
    UserSettings.Settings settings = UserSettings.get();

    Integer fpsVal = parseOrNull(fpsText.getText());
    if (fpsVal != null) {
      settings.fps = fpsVal;
    }
    settings.fullscreen = fullScreenCheck.isChecked();
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
    applyUiScale(settings.uiScale);
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

  private void applyUiScale(float scale) {
    if (scaleStage) {
      UiScale.applyToStage(stage, scale);
      return;
    }
    UiScale.apply(rootTable, scale);
  }

  private void loadFromSavedSettings() {
    if (fpsText == null) {
      return;
    }
    UserSettings.Settings settings = UserSettings.get();
    fpsText.setText(Integer.toString(settings.fps));
    fullScreenCheck.setChecked(settings.fullscreen);
    vsyncCheck.setChecked(settings.vsync);
    uiScaleSlider.setValue(settings.uiScale);
    displayModeSelect.setSelected(getActiveMode(displayModeSelect.getItems()));
    windowSizeSelect.setSelected(WindowSize.matching(settings.windowWidth, settings.windowHeight));
    showTimerCheck.setChecked(settings.showTimer);
    showFpsCheck.setChecked(settings.showFps);
    musicSlider.setValue(settings.musicVolume);
    soundSlider.setValue(settings.soundVolume);
    muteUnfocusedCheck.setChecked(settings.muteUnfocused);
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
      if (scaleStage) {
        UiScale.applyToStage(stage, UserSettings.get().uiScale);
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
    if (uiScaleSlider != null) {
      applyUiScale(uiScaleSlider.getValue());
    }
  }

  @Override
  public void dispose() {
    if (ServiceLocator.getEntityService() != null) {
      ServiceLocator.getEntityService().setFrozen(this, false);
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
