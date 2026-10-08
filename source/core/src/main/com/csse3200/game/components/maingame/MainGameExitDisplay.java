package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays a button to exit the Main Game screen to the Main Menu screen. */
public class MainGameExitDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainGameExitDisplay.class);
  private static final float Z_INDEX = 2f;
  private final Runnable saveAndExit;
  private final Runnable deleteSaveAndExit;
  private final Runnable stopMovement;
  private final Runnable openSettings;
  private Table table;

  public MainGameExitDisplay(
      Runnable saveAndExit,
      Runnable deleteSaveAndExit,
      Runnable stopMovement,
      Runnable openSettings) {
    this.saveAndExit = saveAndExit;
    this.deleteSaveAndExit = deleteSaveAndExit;
    this.stopMovement = stopMovement;
    this.openSettings = openSettings;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    table = new Table();
    table.top().right();
    table.setFillParent(true);

    TextButton settingsBtn = new TextButton("Settings", skin);
    TextButton mainMenuBtn = new TextButton("Exit", skin);

    settingsBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Settings button clicked");
            openSettings.run();
          }
        });

    mainMenuBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Exit button clicked");
            showExitDialog();
          }
        });

    table.add(settingsBtn).padTop(10f).padRight(8f);
    table.add(mainMenuBtn).padTop(10f).padRight(10f);

    stage.addActor(table);
  }

  private void showExitDialog() {
    stopMovement.run();
    ExitSaveDialog dialog = new ExitSaveDialog(skin, saveAndExit, deleteSaveAndExit);
    dialog.show(stage);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    table.clear();
    super.dispose();
  }
}
