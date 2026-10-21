package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.AudioLevels;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Screen shown when the player dies, with options to load a savepoint or return to the main menu.
 */
public class DeathScreen extends ScreenAdapter {
  private final GdxGame game;
  private Renderer renderer;
  private static final String[] DEATH_TEXTURES = {"images/game_over.png"};
  private static final String DEATH_SOUND_C = "sounds/death_sound.mp3";
  private static final String[] DEATH_SOUND = {DEATH_SOUND_C};

  /**
   * Creates the death screen, initializes its services, loads its assets, and builds its UI.
   *
   * @param game game instance used to load a savepoint or switch screens
   */
  public DeathScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(5f, 5f);
    loadAssets();
    createUI();
    playDeathSound();
  }

  /** Loads the death screen's background texture and sound effect. */
  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(DEATH_TEXTURES);
    resourceService.loadSounds(DEATH_SOUND);
    resourceService.loadAll();
  }

  /** Plays the death sound when the asset is available. */
  private void playDeathSound() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService != null && resourceService.containsAsset(DEATH_SOUND_C, Sound.class)) {
      AudioLevels.play(resourceService.getAsset(DEATH_SOUND_C, Sound.class));
    }
  }

  /** Creates the death screen UI and registers its input handler. */
  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    Table table = new Table();
    table.setFillParent(true);

    Image image =
        new Image(
            ServiceLocator.getResourceService().getAsset("images/game_over.png", Texture.class));

    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    TextButton loadButton = new TextButton("Load Savepoint", skin);
    loadButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            int slot = game.getActiveSaveSlot();
            if (!FileLoader.saveExists(slot)) {
              return;
            }
            GameSaveData save = FileLoader.load(slot);
            if (save != null) {
              game.startGameAtCheckpoint(save, slot);
            }
          }
        });
    TextButton menuButton = new TextButton("Main Menu", skin);
    menuButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    table.add(image).colspan(2).padBottom(20f).row();
    table.add(loadButton).padTop(20f).padRight(14f);
    table.add(menuButton).padTop(20f).padLeft(14f);
    stage.addActor(table);

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  /**
   * Clears the frame, updates game entities, and renders the screen.
   *
   * @param delta time in seconds since the previous frame
   */
  @Override
  public void render(float delta) {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  /**
   * Updates the renderer after the window dimensions change.
   *
   * @param width new screen width in pixels
   * @param height new screen height in pixels
   */
  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  /** Disposes of the renderer and screen services. */
  @Override
  public void dispose() {
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }
}
