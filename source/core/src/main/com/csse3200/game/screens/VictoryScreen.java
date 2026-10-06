package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.GameProgress;
import com.csse3200.game.files.GameProgress.SaveData;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** End-of-run screen. Extra time/achievement lines follow Settings → Show victory stats. */
public class VictoryScreen extends ScreenAdapter {
  private final GdxGame game;
  private Renderer renderer;

  public VictoryScreen(GdxGame game) {
    this.game = game;
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    renderer = RenderFactory.createRenderer();
    createUI();
  }

  @Override
  public void render(float delta) {
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

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new VictoryDisplay(game)).addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  private static final class VictoryDisplay extends UIComponent {
    private final GdxGame game;
    private Table table;

    private VictoryDisplay(GdxGame game) {
      this.game = game;
    }

    @Override
    public void create() {
      super.create();
      Skin menuSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
      table = new Table();
      table.setFillParent(true);
      table.add(new Label("Victory", menuSkin, "title")).padBottom(20f);
      table.row();
      if (UserSettings.get().showVictoryStats) {
        SaveData save = GameProgress.get();
        table.add(new Label("Last run: " + GameProgress.formatTime(save.lastRunMs), menuSkin));
        table.row().padTop(8f);
        table.add(new Label("Best run: " + GameProgress.formatTime(save.bestRunMs), menuSkin));
        table.row().padTop(8f);
        table.add(new Label("Achievements: " + save.achievements.size(), menuSkin));
        table.row().padTop(20f);
      }
      TextButton menu = new TextButton("Main menu", menuSkin);
      menu.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              game.setScreen(GdxGame.ScreenType.MAIN_MENU);
            }
          });
      table.add(menu);
      stage.addActor(table);
    }

    @Override
    protected void draw(SpriteBatch batch) {
      // draw is handled by the stage
    }

    @Override
    public void dispose() {
      if (table != null) {
        table.remove();
      }
      super.dispose();
    }
  }
}
