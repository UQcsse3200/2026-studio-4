package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
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
  private Table table;

  public MainGameExitDisplay(
      Runnable saveAndExit, Runnable deleteSaveAndExit, Runnable stopMovement) {
    this.saveAndExit = saveAndExit;
    this.deleteSaveAndExit = deleteSaveAndExit;
    this.stopMovement = stopMovement;
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

    TextButton mainMenuBtn = new TextButton("Exit", skin);

    mainMenuBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Exit button clicked");
            showExitDialog();
          }
        });

    table.add(mainMenuBtn).padTop(10f).padRight(10f);

    stage.addActor(table);
  }

  private void showExitDialog() {
    stopMovement.run();
    Dialog dialog =
        new Dialog("Exit game?", skin) {
          @Override
          protected void result(Object object) {
            if ("save".equals(object)) {
              saveAndExit.run();
            } else if ("delete".equals(object)) {
              deleteSaveAndExit.run();
            }
          }
        };

    dialog.text("Do you want to save your progress before exiting?");
    dialog.button("Save and exit", "save");
    dialog.button("Delete save and exit", "delete");
    dialog.button("Cancel", "cancel");
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
