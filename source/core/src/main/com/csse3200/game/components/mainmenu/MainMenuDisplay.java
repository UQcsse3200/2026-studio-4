package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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

  /** Share of the button the text may fill. The rest is padding. */
  private static final float TEXT_FILL_WIDTH = 0.45f;

  private static final float TEXT_FILL_HEIGHT = 0.225f;

  /** Never shrink below this, so small windows stay readable. */
  private static final float MIN_TEXT_SCALE = 0.225f;

  /** true: image covers the whole screen (edges may be cropped). false: whole image visible. */
  private static final boolean COVER_SCREEN = true;

  private Texture backgroundTexture;
  private Image background;
  private TextButton startBtn;
  private TextButton loadBtn;
  private TextButton settingsBtn;
  private TextButton exitBtn;
  private float laidOutWidth = -1f;
  private float laidOutHeight = -1f;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    backgroundTexture =
        ServiceLocator.getResourceService().getAsset("images/main_game2.png", Texture.class);
    background = new Image(backgroundTexture);
    background.setScaling(Scaling.stretch); // bounds already match the image's aspect ratio

    startBtn = new TextButton("Start", skin);
    loadBtn = new TextButton("Load", skin);
    settingsBtn = new TextButton("Settings", skin);
    exitBtn = new TextButton("Exit", skin);

    addTrigger(startBtn, "start", "Start");
    addTrigger(loadBtn, "load", "Load");
    addTrigger(settingsBtn, "settings", "Settings");
    addTrigger(exitBtn, "exit", "Exit");

    stage.addActor(background);
    stage.addActor(startBtn);
    stage.addActor(loadBtn);
    stage.addActor(settingsBtn);
    stage.addActor(exitBtn);

    layoutMenu();
  }

  @Override
  public void draw(SpriteBatch batch) {
    // The stage draws the actors; this only re-lays them out after a resize or fullscreen toggle.
    if (stage.getWidth() != laidOutWidth || stage.getHeight() != laidOutHeight) {
      layoutMenu();
    }
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  private void addTrigger(TextButton button, String event, String name) {
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("{} button clicked", name);
            entity.getEvents().trigger(event);
          }
        });
  }

  /**
   * Scales a button's label to the largest size that still fits inside the button, so text neither
   * looks tiny on a big screen nor clips on a small one.
   *
   * @param button button whose bounds have already been set
   */
  private void fitText(TextButton button) {
    BitmapFont font = button.getLabel().getStyle().font;
    GlyphLayout layout = new GlyphLayout(font, button.getText());

    if (layout.width <= 0f || layout.height <= 0f) {
      return;
    }

    float byWidth = (button.getWidth() * TEXT_FILL_WIDTH) / layout.width;
    float byHeight = (button.getHeight() * TEXT_FILL_HEIGHT) / layout.height;
    float scale = Math.max(MIN_TEXT_SCALE, Math.min(byWidth, byHeight));

    button.getLabel().setFontScale(scale);
  }

  private void placeOnImage(
      Actor actor,
      float x,
      float top,
      float width,
      float height,
      float offsetX,
      float offsetY,
      float drawW,
      float drawH) {
    actor.setBounds(
        offsetX + x * drawW,
        offsetY + drawH - (top + height) * drawH,
        width * drawW,
        height * drawH);
  }

  /**
   * Sizes the background to the screen while keeping its aspect ratio, then places each button
   * relative to where the image is actually drawn. Called again whenever the stage size changes.
   */
  private void layoutMenu() {
    logger.info(
        "Menu layout: stage {}x{}, window {}x{}, backbuffer {}x{}, desktop {}x{}",
        stage.getWidth(),
        stage.getHeight(),
        Gdx.graphics.getWidth(),
        Gdx.graphics.getHeight(),
        Gdx.graphics.getBackBufferWidth(),
        Gdx.graphics.getBackBufferHeight(),
        Gdx.graphics.getDisplayMode().width,
        Gdx.graphics.getDisplayMode().height);
    float stageW = stage.getWidth();
    float stageH = stage.getHeight();
    laidOutWidth = stageW;
    laidOutHeight = stageH;

    float imageW = backgroundTexture.getWidth();
    float imageH = backgroundTexture.getHeight();
    float scale =
        COVER_SCREEN
            ? Math.max(stageW / imageW, stageH / imageH)
            : Math.min(stageW / imageW, stageH / imageH);
    float drawW = imageW * scale;
    float drawH = imageH * scale;
    float offsetX = (stageW - drawW) / 2f;
    float offsetY = (stageH - drawH) / 2f;

    background.setBounds(offsetX, offsetY, drawW, drawH);

    // Fractions of the image, with y measured from the top of the image.
    placeOnImage(startBtn, 0.13f, 0.432f, 0.13f, 0.075f, offsetX, offsetY, drawW, drawH);
    placeOnImage(loadBtn, 0.13f, 0.539f, 0.13f, 0.075f, offsetX, offsetY, drawW, drawH);
    placeOnImage(settingsBtn, 0.13f, 0.644f, 0.13f, 0.075f, offsetX, offsetY, drawW, drawH);
    placeOnImage(exitBtn, 0.775f, 0.675f, 0.12f, 0.078f, offsetX, offsetY, drawW, drawH);

    fitText(startBtn);
    fitText(loadBtn);
    fitText(settingsBtn);
    fitText(exitBtn);
  }
}
