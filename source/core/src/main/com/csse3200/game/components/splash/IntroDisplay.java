package com.csse3200.game.components.splash;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.ui.UIComponent;

/** Black intro screen with an idle book animation and a Space-triggered sequence. */
public class IntroDisplay extends UIComponent {

  private static final String BOOK = "pentagram"; // or "moon" / "chain"
  private static final float BOOK_SCALE = 4f;
  private static final float FRAME_DURATION = 1f / 8f;
  private static final float OPEN_BOOK_HOLD = 0.6f;
  private static final String CONTINUE_TEXT = "PRESS SPACE TO CONTINUE";

  private final TextureRegionDrawable bookDrawable = new TextureRegionDrawable();
  private Image book;
  private Image whiteOverlay;
  private Label continuePrompt;
  private AtlasRegion idleFrame;
  private Animation<AtlasRegion> openingAnimation;
  private float animationTime;
  private boolean triggered;
  private boolean finished;

  @Override
  public void create() {
    super.create();

    TextureAtlas atlas = hud.getAtlas();
    idleFrame = atlas.findRegion(BOOK + "_idle");
    Array<AtlasRegion> opening = atlas.findRegions(BOOK + "_open");
    if (idleFrame == null || opening.isEmpty()) {
      throw new IllegalStateException("Intro regions '" + BOOK + "_*' missing from hud skin atlas");
    }
    openingAnimation =
        new Animation<>(
            FRAME_DURATION, atlas.findRegions(BOOK + "_open"), Animation.PlayMode.NORMAL);

    Image background = new Image(skin.newDrawable("white", Color.BLACK));
    background.setFillParent(true);
    stage.addActor(background);

    bookDrawable.setRegion(idleFrame);
    book = new Image(bookDrawable);
    book.setSize(idleFrame.getRegionWidth() * BOOK_SCALE, idleFrame.getRegionHeight() * BOOK_SCALE);
    book.setOrigin(Align.center);
    stage.addActor(book);

    continuePrompt = new Label(CONTINUE_TEXT, skin);
    continuePrompt.setAlignment(Align.center);
    continuePrompt.setFontScale(1.4f);
    stage.addActor(continuePrompt);

    whiteOverlay = new Image(skin.newDrawable("white", Color.BLACK));
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
    if (triggered && !isRepeatPass()) animationTime += Gdx.graphics.getDeltaTime();
    bookDrawable.setRegion(triggered ? openingAnimation.getKeyFrame(animationTime) : idleFrame);

    // Re-centre every frame so window resizes are handled.
    book.setPosition(stage.getWidth() / 2f, stage.getHeight() / 2f, Align.center);
    continuePrompt.setBounds(
        0f, stage.getHeight() * 0.08f, stage.getWidth(), continuePrompt.getHeight());
  }

  private void startTriggeredAnimation() {
    triggered = true;
    animationTime = 0f;
    continuePrompt.setVisible(false);
    float open = openingAnimation.getAnimationDuration();
    book.addAction(
        sequence(
            Actions.delay(open),
            Actions.scaleTo(2f, 2f, OPEN_BOOK_HOLD + 0.5f, Interpolation.pow2In)));
    whiteOverlay.addAction(
        sequence(
            Actions.delay(open + OPEN_BOOK_HOLD),
            Actions.fadeIn(0.5f),
            run(() -> finished = true)));
  }

  public boolean isFinished() {
    return finished;
  }
}
