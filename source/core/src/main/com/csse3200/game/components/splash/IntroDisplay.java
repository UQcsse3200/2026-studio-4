package com.csse3200.game.components.splash;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** Black intro screen with an idle book animation and a Space-triggered sequence. */
public class IntroDisplay extends UIComponent {
  private static final String INTRO_TEXTURE = "images/intro.png";
  private static final int ROWS = 3;
  // The supplied sheet appears to have 24 frames per row. Change this if your sheet differs.
  private static final int COLUMNS = 24;
  private static final int BOTTOM_ROW = 2;
  private static final int OPENING_FRAME_COUNT = 8;
  private static final float FRAME_DURATION = 1f / 8f;
  private static final String CONTINUE_TEXT = "PRESS SPACE TO CONTINUE";

  private Image background;
  private Image book;
  private Image whiteOverlay;
  private Label continuePrompt;
  private Animation<TextureRegion> triggeredAnimation;
  private float triggeredAnimationTime;
  private boolean triggered;
  private boolean finished;

  @Override
  public void create() {
    super.create();

    Texture texture = ServiceLocator.getResourceService().getAsset(INTRO_TEXTURE, Texture.class);
    TextureRegion[] bottom = getBottomRow(texture);
    TextureRegion[] openingFrames = new TextureRegion[OPENING_FRAME_COUNT];
    System.arraycopy(bottom, 0, openingFrames, 0, OPENING_FRAME_COUNT);
    triggeredAnimation = new Animation<>(FRAME_DURATION, openingFrames);
    triggeredAnimation.setPlayMode(Animation.PlayMode.NORMAL);

    background = new Image(skin.newDrawable("white", Color.BLACK));
    background.setFillParent(true);
    stage.addActor(background);

    book = new Image(bottom[0]);
    book.setSize(bottom[0].getRegionWidth() * 4f, bottom[0].getRegionHeight() * 4f);
    book.setOrigin(Align.center);
    centreBook();
    stage.addActor(book);

    continuePrompt = new Label(CONTINUE_TEXT, skin);
    continuePrompt.setColor(Color.WHITE);
    continuePrompt.setAlignment(Align.center);
    continuePrompt.setPosition(0f, stage.getHeight() * 0.08f);
    continuePrompt.setWidth(stage.getWidth());
    continuePrompt.setFontScale(1.4f);
    stage.addActor(continuePrompt);

    whiteOverlay = new Image(skin.newDrawable("white", Color.WHITE));
    whiteOverlay.setFillParent(true);
    whiteOverlay.getColor().a = 0f;
    stage.addActor(whiteOverlay);

    stage.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == Input.Keys.SPACE && !triggered) {
              startTriggeredAnimation();
              return true;
            }
            return false;
          }
        });
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (triggered && !isRepeatPass()) {
      triggeredAnimationTime += Gdx.graphics.getDeltaTime();
    }
    TextureRegion frame =
        triggered
            ? triggeredAnimation.getKeyFrame(triggeredAnimationTime, false)
            : triggeredAnimation.getKeyFrame(0f);
    book.setDrawable(new TextureRegionDrawable(frame));
    // Keep the sprite centred horizontally after a resize while preserving its float offset.
    book.setX((stage.getWidth() - book.getWidth()) / 2f);
    continuePrompt.setWidth(stage.getWidth());
    continuePrompt.setX(0f);
    continuePrompt.setY(stage.getHeight() * 0.08f);
  }

  private void centreBook() {
    book.setPosition(stage.getWidth() / 2f, stage.getHeight() / 2f, Align.center);
  }

  private void startTriggeredAnimation() {
    triggered = true;
    triggeredAnimationTime = 0f;
    continuePrompt.setVisible(false);
    book.clearActions();
    book.setPosition(stage.getWidth() / 2f, stage.getHeight() / 2f, Align.center);
    float duration = triggeredAnimation.getAnimationDuration();
    whiteOverlay.addAction(
        sequence(
            Actions.delay(duration),
            Actions.fadeIn(0.6f),
            run(() -> finished = true)));
  }

  /** Splits the bottom row using proportional bounds, including any remainder pixels. */
  private static TextureRegion[] getBottomRow(Texture texture) {
    TextureRegion[] frames = new TextureRegion[COLUMNS];
    int rowHeight = texture.getHeight() / ROWS;
    int y = (ROWS - 1 - BOTTOM_ROW) * rowHeight;
    int height = BOTTOM_ROW == 0 ? texture.getHeight() - y : rowHeight;

    for (int column = 0; column < COLUMNS; column++) {
      int x0 = column * texture.getWidth() / COLUMNS;
      int x1 = (column + 1) * texture.getWidth() / COLUMNS;
      frames[column] = new TextureRegion(texture, x0, y, x1 - x0, height);
    }
    return frames;
  }

  public boolean isFinished() {
    return finished;
  }

  @Override
  public void dispose() {
    background.remove();
    book.remove();
    whiteOverlay.remove();
    continuePrompt.remove();
    super.dispose();
  }
}
