package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.splash.IntroDisplay;
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
 * Displays the intro animation between the studio splash and main menu.
 *
 * <p>The screen loads its own rendering and input services, then transitions to the main menu when
 * the intro display reports that its animation has finished.
 */
public class IntroScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(IntroScreen.class);
  private static final String INTRO_TEXTURE = "images/intro.png";

  private final GdxGame game;
  private final Renderer renderer;
  private IntroDisplay display;
  private boolean leaving;

  /**
   * Creates the intro screen, loads its background texture, and registers its display and input
   * handling.
   *
   * @param game game instance used to transition to the main menu
   */
  public IntroScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    renderer = RenderFactory.createRenderer();

    ServiceLocator.getResourceService().loadTextures(new String[] {INTRO_TEXTURE});
    ServiceLocator.getResourceService().loadAll();

    Stage stage = ServiceLocator.getRenderService().getStage();
    display = new IntroDisplay();
    Entity ui = new Entity();
    ui.addComponent(display).addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  /**
   * Updates the screen entities, renders the intro, and moves to the main menu once it finishes.
   *
   * @param delta time in seconds since the previous frame
   */
  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();

    // Change screens after the stage update has finished.
    if (!leaving && display.isFinished()) {
      leaving = true;
      game.setScreen(GdxGame.ScreenType.MAIN_MENU);
    }
  }

  /**
   * Updates the renderer's viewport after the window size changes.
   *
   * @param width new screen width in pixels
   * @param height new screen height in pixels
   */
  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  /** Releases the renderer, screen services, and loaded intro texture. */
  @Override
  public void dispose() {
    logger.debug("Disposing intro screen");
    renderer.dispose();
    ServiceLocator.getResourceService().unloadAssets(new String[] {INTRO_TEXTURE});
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }
}
