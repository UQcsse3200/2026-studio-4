package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays the available save slots and lets the player load, delete, or start a game in a slot.
 * Slots can be selected with the keyboard or mouse. Invalid saves can be deleted after a
 * confirmation input.
 */
public class LoadGameScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(LoadGameScreen.class);
  private static final String WINDOW_DRAWABLE = "window-c";
  private static final String SMALL_FONT = "font_small";
  private static final int SLOT_COUNT = 3;
  private static final Color DARK = Color.valueOf("211a14");
  private static final Color DEEP = Color.valueOf("100d0b");
  private static final Color GOLD = Color.valueOf("c5a15d");
  private static final Color SELECTED_GOLD = Color.valueOf("e5c47a");
  private static final Color TEXT = Color.valueOf("e6dcc7");
  private static final Color MUTED = Color.valueOf("a99a7d");

  private final GdxGame game;
  private final Renderer renderer;
  private final Skin skin;
  private final GameSaveData[] saves = new GameSaveData[SLOT_COUNT];
  private final boolean[] saveFiles = new boolean[SLOT_COUNT];
  private final Button[] slotButtons = new Button[SLOT_COUNT];
  private final Button.ButtonStyle[] normalStyles = new Button.ButtonStyle[SLOT_COUNT];
  private final Button.ButtonStyle[] selectedStyles = new Button.ButtonStyle[SLOT_COUNT];
  private final List<Texture> previewTextures = new ArrayList<>();
  private Label footer;
  private int selectedIndex;
  private boolean confirmingDelete;

  /**
   * Creates the save browser, loads saved slot data, and builds its user interface.
   *
   * @param game game instance used to start a save or return to the main menu
   */
  public LoadGameScreen(GdxGame game) {
    this.game = game;
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    Gdx.gl.glClearColor(0.035f, 0.027f, 0.02f, 1f);
    loadSlotData();
    createUI();
  }

  private void loadSlotData() {
    for (int index = 0; index < SLOT_COUNT; index++) {
      int slot = index + 1;
      saveFiles[index] = FileLoader.saveExists(slot);
      if (saveFiles[index]) {
        saves[index] = FileLoader.load(slot);
        if (saves[index] != null
            && (saves[index].version != 1
                || saves[index].checkpoint == null
                || saves[index].playerData == null)) {
          saves[index] = null;
        }
      }
    }
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Table root = new Table();
    root.setFillParent(true);
    root.pad(24f);

    Table frame = new Table();
    frame.setBackground(skin.newDrawable(WINDOW_DRAWABLE, GOLD));
    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WINDOW_DRAWABLE, DARK));
    panel.pad(22f, 28f, 18f, 28f);

    Table header = new Table();
    Table heading = new Table();
    heading.left();
    heading.add(label("DATA LIST", "font_large", SELECTED_GOLD, 1.15f)).left().row();
    heading.add(label("Select data to load", SMALL_FONT, TEXT, 1f)).left();
    header.add(heading).expandX().left();
    panel.add(header).expandX().fillX().left().padBottom(14f).row();

    Table divider = new Table();
    divider.setBackground(skin.newDrawable("button-c", GOLD));
    panel.add(divider).height(3f).expandX().fillX().padBottom(16f).row();

    float panelHeight = Math.min(680f, Gdx.graphics.getHeight() - 32f);
    float rowHeight = Math.max(52f, Math.min(112f, (panelHeight - 213f) / 3f));
    for (int index = 0; index < SLOT_COUNT; index++) {
      Button row = createSlotRow(index, rowHeight);
      slotButtons[index] = row;
      panel.add(row).expandX().fillX().height(rowHeight).padBottom(9f).row();
    }

    footer =
        label(
            "UP / DOWN  Select     ENTER  Load     F  Delete     ESC  Back",
            SMALL_FONT,
            MUTED,
            1f);
    footer.setAlignment(Align.left);
    panel.add(footer).expandX().fillX().left().padTop(8f);

    frame.add(panel).expand().fill().pad(12f);
    float width = Math.min(1120f, Gdx.graphics.getWidth() - 32f);
    float height = Math.min(680f, Gdx.graphics.getHeight() - 32f);
    root.add(frame).width(width).height(height).center();
    stage.addActor(root);
    updateSelection();

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10)).addComponent(new SaveListControls());
    ServiceLocator.getEntityService().register(ui);
  }

  private Button createSlotRow(int index, float rowHeight) {
    Button.ButtonStyle normal = new Button.ButtonStyle(skin.get(Button.ButtonStyle.class));
    normal.up = skin.newDrawable(WINDOW_DRAWABLE, DARK);
    normal.over = skin.newDrawable(WINDOW_DRAWABLE, Color.valueOf("342719"));
    normal.down = skin.newDrawable(WINDOW_DRAWABLE, Color.valueOf("49351e"));
    Button.ButtonStyle selected = new Button.ButtonStyle(normal);
    selected.up = skin.newDrawable(WINDOW_DRAWABLE, Color.valueOf("4a371f"));
    selected.over = skin.newDrawable(WINDOW_DRAWABLE, Color.valueOf("594224"));
    selected.down = skin.newDrawable(WINDOW_DRAWABLE, Color.valueOf("654d2c"));
    normalStyles[index] = normal;
    selectedStyles[index] = selected;

    Button row = new Button(normal);
    row.pad(10f, 16f, 10f, 16f);
    Table content = new Table();
    content.left();

    GameSaveData save = saves[index];
    Texture preview = loadPreview(index);
    addPreview(content, preview, saveFiles[index], rowHeight);

    Table details = createDetails(index, save);
    content.add(details).expandX().fillX().left();

    addCheckpoint(content, save);

    row.add(content).expand().fill();
    row.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            selectedIndex = index;
            updateSelection();
            loadSelectedSlot();
          }
        });
    return row;
  }

  private void addPreview(Table content, Texture preview, boolean saveFileExists, float rowHeight) {
    if (preview != null) {
      Image image = new Image(preview);
      image.setScaling(Scaling.fit);
      content.add(image).size(138f, rowHeight - 22f).padRight(18f);
      return;
    }
    Table placeholder = new Table();
    placeholder.setBackground(skin.newDrawable("button-c", DEEP));
    String text = saveFileExists ? "NO MAP\nPREVIEW" : "EMPTY SLOT";
    placeholder.add(label(text, SMALL_FONT, MUTED, 0.9f)).center();
    content.add(placeholder).size(138f, rowHeight - 22f).padRight(18f);
  }

  private Table createDetails(int index, GameSaveData save) {
    Table details = new Table();
    details.left();
    details.add(label("SAVE SLOT " + (index + 1), "font_large", SELECTED_GOLD, 0.92f)).left().row();
    if (save == null) {
      String status = saveFiles[index] ? "This record could not be read" : "No saved journey";
      details.add(label(status, SMALL_FONT, MUTED, 1f)).left();
      return details;
    }
    String room = save.resumePosition != null
        ? displayRoom(save.resumePosition.roomId)
        : (save.checkpoint == null ? "Unknown location" : displayRoom(save.checkpoint.roomId));
    details.add(label(room, "font", TEXT, 1f)).left().row();
    details.add(label("Play Time  " + formatPlayTime(save.playTimeSeconds), SMALL_FONT, TEXT, 1f)).left().row();
    details.add(label(formatDungeonTimes(save.dungeonTimesSeconds), SMALL_FONT, MUTED, 0.9f)).left().row();
    String stats = "Gold  " + save.playerData.gold + "     Charms  "
        + count(save.playerData.charms) + "     Upgrades  " + count(save.playerData.upgradedWeapons);
    details.add(label(stats, SMALL_FONT, MUTED, 0.95f)).left();
    return details;
  }

  private void addCheckpoint(Table content, GameSaveData save) {
    if (save == null || Gdx.graphics.getWidth() < 900) return;
    Table location = new Table();
    location.right();
    location.add(label("LAST CHECKPOINT", SMALL_FONT, MUTED, 0.82f)).right().row();
    String checkpoint = save.checkpoint == null ? "Unknown" : displayRoom(save.checkpoint.roomId);
    location.add(label(checkpoint, "font", TEXT, 0.95f)).right();
    content.add(location).width(230f).right().padLeft(12f);
  }

  private Texture loadPreview(int index) {
    int slot = index + 1;
    FileHandle preview = FileLoader.getSavePreview(slot);
    if (!preview.exists()) {
      return null;
    }
    try {
      Texture texture = new Texture(preview);
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      previewTextures.add(texture);
      return texture;
    } catch (RuntimeException exception) {
      logger.warn("Could not load preview for save slot {}", slot, exception);
      return null;
    }
  }

  private Label label(String text, String fontName, Color color, float scale) {
    Label.LabelStyle style = new Label.LabelStyle(skin.getFont(fontName), new Color(color));
    Label label = new Label(text, style);
    label.setFontScale(scale);
    return label;
  }

  private String displayRoom(String roomId) {
    if (roomId == null || roomId.isBlank()) {
      return "Unknown location";
    }
    String spaced =
        roomId.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ').replace('-', ' ');
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  private String formatDungeonTimes(java.util.Map<String, Float> times) {
    if (times == null || times.isEmpty()) {
      return "Dungeons  --";
    }
    List<String> dungeonIds = new ArrayList<>(times.keySet());
    dungeonIds.sort(String::compareTo);
    List<String> entries = new ArrayList<>();
    for (String dungeonId : dungeonIds) {
      entries.add(displayRoom(dungeonId) + " " + formatPlayTime(times.get(dungeonId)));
    }
    return String.join("  |  ", entries);
  }

  private int count(List<?> values) {
    return values == null ? 0 : values.size();
  }

  private String formatPlayTime(float seconds) {
    if (!Float.isFinite(seconds) || seconds < 0f) {
      seconds = 0f;
    }
    int totalSeconds = (int) seconds;
    int minutes = totalSeconds / 60;
    int wholeSeconds = totalSeconds % 60;
    int hundredths = (int) ((seconds - totalSeconds) * 100f);
    return String.format("%02d:%02d.%02d", minutes, wholeSeconds, hundredths);
  }

  private void updateSelection() {
    for (int index = 0; index < SLOT_COUNT; index++) {
      if (slotButtons[index] != null) {
        slotButtons[index].setStyle(
            index == selectedIndex ? selectedStyles[index] : normalStyles[index]);
      }
    }
  }

  private void loadSelectedSlot() {
    int slot = selectedIndex + 1;
    GameSaveData save = saves[selectedIndex];
    if (save == null) {
      if (saveFiles[selectedIndex]) {
        footer.setText("This save could not be read. Press F twice to delete it.");
      } else {
        game.startGame(null, slot);
      }
      return;
    }
    game.startGame(save, slot);
  }

  private void goBack() {
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  private void moveSelection(int amount) {
    selectedIndex = (selectedIndex + amount + SLOT_COUNT) % SLOT_COUNT;
    confirmingDelete = false;
    updateSelection();
    if (saves[selectedIndex] == null) {
      footer.setText(
          saveFiles[selectedIndex]
              ? "Unreadable record     F  Delete"
              : "Empty slot     ENTER  Begin a new journey here");
    } else {
      footer.setText("ENTER  Load selected record     F  Delete     ESC  Back");
    }
  }

  private void deleteSelectedSlot() {
    if (!saveFiles[selectedIndex]) {
      footer.setText("This slot is already empty");
      confirmingDelete = false;
      return;
    }
    if (!confirmingDelete) {
      confirmingDelete = true;
      footer.setText("Press F again to delete this slot and its map preview");
      return;
    }
    FileLoader.deleteSaveSlot(selectedIndex + 1);
    game.setScreen(GdxGame.ScreenType.LOAD_GAME);
  }

  private class SaveListControls extends InputComponent {
    private SaveListControls() {
      super(20);
    }

    @Override
    public boolean keyDown(int keycode) {
      switch (keycode) {
        case Input.Keys.UP:
          moveSelection(-1);
          return true;
        case Input.Keys.DOWN:
          moveSelection(1);
          return true;
        case Input.Keys.ENTER:
        case Input.Keys.NUMPAD_ENTER:
          loadSelectedSlot();
          return true;
        case Input.Keys.F:
          deleteSelectedSlot();
          return true;
        case Input.Keys.ESCAPE:
        case Input.Keys.Q:
          goBack();
          return true;
        default:
          return false;
      }
    }
  }

  /** Updates entities and draws the save browser. */
  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  /**
   * Updates the renderer viewport after the window size changes.
   *
   * @param width new window width in pixels
   * @param height new window height in pixels
   */
  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  /** Releases preview textures, UI resources, and services owned by this screen. */
  @Override
  public void dispose() {
    for (Texture texture : previewTextures) {
      texture.dispose();
    }
    skin.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}
