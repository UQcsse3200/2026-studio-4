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
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

public class DeathScreen extends ScreenAdapter {
  private final GdxGame game;
  private Renderer renderer;
  private static final String[] DEATH_TEXTURES = {"images/game_over.png"};
  private static final String DEATH_SOUND_C = "sounds/death_sound.mp3";
  private static final String[] DEATH_SOUND = {DEATH_SOUND_C};

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

  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(DEATH_TEXTURES);
    resourceService.loadSounds(DEATH_SOUND);
    resourceService.loadAll();
  }

  private void playDeathSound() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService != null && resourceService.containsAsset(DEATH_SOUND_C, Sound.class)) {
      resourceService.getAsset(DEATH_SOUND_C, Sound.class).play();
    }
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    Table table = new Table();
    table.setFillParent(true);

    Image image =
        new Image(
            ServiceLocator.getResourceService().getAsset("images/game_over.png", Texture.class));

    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    TextButton retryButton = new TextButton("Retry", skin);
    retryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    table.add(image).padBottom(20f).row();
    table.add(retryButton).padTop(20f);
    stage.addActor(table);

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }
}
