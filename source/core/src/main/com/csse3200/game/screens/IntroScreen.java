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

/** Intro animation shown between the studio splash and main menu. */
public class IntroScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(IntroScreen.class);
  private static final String INTRO_TEXTURE = "images/intro.png";

  private final GdxGame game;
  private final Renderer renderer;
  private IntroDisplay display;
  private boolean leaving;

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

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

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
