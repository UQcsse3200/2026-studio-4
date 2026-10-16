package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.splash.SplashDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The first screen shown on launch. Plays the studio logo sequence, then moves to the main menu
 * when it finishes or the player skips it.
 */
public class SplashScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);

  private final GdxGame game;
  private final Renderer renderer;
  private SplashDisplay display;
  private boolean leaving;

  public SplashScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising splash screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();

    loadAssets();
    createUI();
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();

    // Switch only after rendering, so this screen is never disposed while the stage is updating.
    if (!leaving && display.isFinished()) {
      leaving = true;
      game.setScreen(GdxGame.ScreenType.MAIN_MENU);
    }
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing splash screen");

    renderer.dispose();
    unloadAssets();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();

    ServiceLocator.clear();
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(SplashDisplay.getTextures());
    resourceService.loadAll();
  }

  private void unloadAssets() {
    logger.debug("Unloading assets");
    ServiceLocator.getResourceService().unloadAssets(SplashDisplay.getTextures());
  }

  private void createUI() {
    logger.debug("Creating ui");
    Stage stage = ServiceLocator.getRenderService().getStage();
    display = new SplashDisplay();
    Entity ui = new Entity();
    ui.addComponent(display).addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }
}
