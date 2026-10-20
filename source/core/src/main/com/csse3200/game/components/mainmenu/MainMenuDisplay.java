package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A ui component for displaying the Main menu. */
public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    Image background =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/main_game2.png", Texture.class));

    background.setFillParent(true);
    background.setScaling(Scaling.fill); // stretch/crop to fill screen, keeps aspect

    // Keep the skin's button shape, but recolour its states to match the parchment artwork.
    TextButton.TextButtonStyle buttonStyle =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    buttonStyle.up = skin.newDrawable(buttonStyle.up, Color.valueOf("D9B66D"));
    buttonStyle.down = skin.newDrawable(buttonStyle.down, Color.valueOf("C6A052"));
    buttonStyle.over = skin.newDrawable(buttonStyle.over, Color.valueOf("E8C875"));
    buttonStyle.fontColor = Color.valueOf("3A2115");

    TextButton startBtn = new TextButton("Start", buttonStyle);
    TextButton loadBtn = new TextButton("Load", buttonStyle);
    TextButton settingsBtn = new TextButton("Settings", buttonStyle);
    TextButton exitBtn = new TextButton("Exit", buttonStyle);

    // Triggers an event when the button is pressed
    startBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Start button clicked");
            entity.getEvents().trigger("start");
          }
        });

    loadBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Load button clicked");
            entity.getEvents().trigger("load");
          }
        });

    settingsBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Settings button clicked");
            entity.getEvents().trigger("settings");
          }
        });

    exitBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {

            logger.debug("Exit button clicked");
            entity.getEvents().trigger("exit");
          }
        });

    stage.addActor(background);
    stage.addActor(startBtn);
    stage.addActor(loadBtn);
    stage.addActor(settingsBtn);
    stage.addActor(exitBtn);

    // Coordinates are percentages of the background image. Y positions below are measured
    // from the top of the image, then converted because Scene2D's origin is bottom-left.
    float width = stage.getWidth();
    float height = stage.getHeight();
    placeFromTop(startBtn, 0.125f, 0.376f, 0.115f, 0.066f, width, height);
    placeFromTop(loadBtn, 0.125f, 0.481f, 0.115f, 0.066f, width, height);
    placeFromTop(settingsBtn, 0.125f, 0.585f, 0.115f, 0.066f, width, height);
    placeFromTop(exitBtn, 0.768f, 0.659f, 0.116f, 0.073f, width, height);
  }

  private void placeFromTop(
      Actor actor, float x, float top, float actorWidth, float actorHeight, float stageWidth,
      float stageHeight) {
    actor.setBounds(
        x * stageWidth,
        stageHeight - (top + actorHeight) * stageHeight,
        actorWidth * stageWidth,
        actorHeight * stageHeight);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }
}
