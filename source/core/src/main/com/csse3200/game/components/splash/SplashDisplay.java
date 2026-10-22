package com.csse3200.game.components.splash;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays the studio logo sequence on the splash screen.
 *
 * <p>The combined S/4 monogram fades in zoomed in, then the S and the 4 slide apart to their places
 * in "STUDIO 4" while the "TUDIO" letters fade in between them and the whole logo zooms out. Any
 * key press, click or tap skips the sequence. The screen polls {@link #isFinished()} to know when
 * to move on, so the screen switch never happens in the middle of the stage updating.
 *
 * <p>All sizes are measured from the PNGs, so the logo is centred whatever size they are exported
 * at. The letter PNGs should share the same canvas height, with the letters on the same baseline,
 * and be cropped to the letter's width. The combined PNG is only used to measure how much the S and
 * 4 overlap in the monogram, so it must be exported at the same scale as the letters and cropped
 * the same way, with the 4 at its left edge and the S at its right edge.
 */
public class SplashDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(SplashDisplay.class);

  static final String COMBINED_TEXTURE = "images/splash/combined-512px.png";
  static final String S_TEXTURE = "images/splash/S-512px.png";
  static final String FOUR_TEXTURE = "images/splash/4-512px.png";

  /** The "TUDIO" letters, drawn left to right. */
  static final String[] TUDIO_TEXTURES = {
    "images/splash/t-512px.png",
    "images/splash/u-512px.png",
    "images/splash/d-512px.png",
    "images/splash/i-512px.png",
    "images/splash/o-512px.png"
  };

  /** Space between letters within "STUDIO". */
  private static final float LETTER_GAP = 24f;

  /** Space between "STUDIO" and "4". */
  private static final float WORD_GAP = 72f;

  // --- Size on screen, once the zoom has finished ---
  /** Letter height as a fraction of the screen height. */
  private static final float NAME_HEIGHT_FRACTION = 0.3f;

  /** The widest the full name may be, as a fraction of the screen width. */
  private static final float NAME_MAX_WIDTH_FRACTION = 0.85f;

  // Timing, in seconds
  private static final float FADE_IN = 0.6f;
  private static final float MONOGRAM_HOLD = 0.6f;
  private static final float SPLIT_DURATION = 0.8f;
  private static final float ZOOM_DURATION = 1.2f;
  private static final float START_SCALE = 1.4f;
  private static final float LETTER_DELAY = 0.1f;
  private static final float LETTER_STAGGER = 0.08f;
  private static final float LETTER_FADE = 0.25f;
  private static final float FINAL_HOLD = 2.0f;
  private static final float FADE_OUT = 0.5f;

  private Image background;
  private Group logo;
  private InputListener skipListener;
  private boolean finished;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    background = new Image(skin.getDrawable("black"));
    background.setFillParent(true);
    stage.addActor(background);

    ResourceService resources = ServiceLocator.getResourceService();
    Texture sTexture = loadTexture(resources, S_TEXTURE);
    Texture fourTexture = loadTexture(resources, FOUR_TEXTURE);
    Texture combinedTexture = loadTexture(resources, COMBINED_TEXTURE);
    Texture[] tudioTextures = new Texture[TUDIO_TEXTURES.length];
    for (int i = 0; i < TUDIO_TEXTURES.length; i++) {
      tudioTextures[i] = loadTexture(resources, TUDIO_TEXTURES[i]);
    }

    // Measure the full name in source pixels: S, the TUDIO letters, a word gap, then the 4.
    float sourceHeight = Math.max(sTexture.getHeight() * 1f, fourTexture.getHeight() * 1f);
    float sourceWidth = sTexture.getWidth() + WORD_GAP + fourTexture.getWidth();
    for (Texture letter : tudioTextures) {
      sourceWidth += LETTER_GAP + letter.getWidth();
      sourceHeight = Math.max(sourceHeight, letter.getHeight() * 1f);
    }

    // Scale to the target height, shrinking if needed so the full name fits across the screen.
    float scale =
        Math.min(
            stage.getHeight() * NAME_HEIGHT_FRACTION / sourceHeight,
            stage.getWidth() * NAME_MAX_WIDTH_FRACTION / sourceWidth);

    Image s = createImage(sTexture, scale);
    Image four = createImage(fourTexture, scale);
    Image[] tudio = new Image[tudioTextures.length];
    for (int i = 0; i < tudioTextures.length; i++) {
      tudio[i] = createImage(tudioTextures[i], scale);
    }

    // Final layout.
    float cursor = 0f;
    float sFinalX = cursor;
    cursor += s.getWidth() + LETTER_GAP * scale;
    for (Image letter : tudio) {
      letter.setPosition(cursor, 0f);
      letter.getColor().a = 0f;
      cursor += letter.getWidth() + LETTER_GAP * scale;
    }
    cursor += (WORD_GAP - LETTER_GAP) * scale;
    float fourFinalX = cursor;
    float wordWidth = fourFinalX + four.getWidth();

    // Starting layout: the monogram, centred within the final word. The overlap is however much
    // narrower the combined image is than the S and 4 side by side.
    float overlap =
        Math.max(
                0f,
                (float) sTexture.getWidth() + fourTexture.getWidth() - combinedTexture.getWidth())
            * scale;
    float monogramWidth = s.getWidth() + four.getWidth() - overlap;
    float monogramLeft = (wordWidth - monogramWidth) / 2f;
    four.setPosition(monogramLeft, 0f);
    s.setPosition(monogramLeft + four.getWidth() - overlap, 0f);

    logo = new Group();
    logo.setTransform(true);
    logo.setSize(wordWidth, sourceHeight * scale);
    logo.setOrigin(Align.center);
    logo.setScale(START_SCALE);
    logo.getColor().a = 0f;

    // Draw order: letters first, then the 4, then the S, so the S passes over the 4.
    for (Image letter : tudio) {
      logo.addActor(letter);
    }
    logo.addActor(four);
    logo.addActor(s);
    centreLogo();
    stage.addActor(logo);

    logo.addAction(
        sequence(
            fadeIn(FADE_IN),
            delay(MONOGRAM_HOLD),
            run(() -> startSplit(s, four, tudio, sFinalX, fourFinalX)),
            scaleTo(1f, 1f, ZOOM_DURATION, Interpolation.pow2Out),
            delay(FINAL_HOLD),
            fadeOut(FADE_OUT),
            run(this::finish)));

    skipListener =
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            skip();
            return true;
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            skip();
            return true;
          }
        };
    stage.addListener(skipListener);
  }

  private void startSplit(Image s, Image four, Image[] tudio, float sFinalX, float fourFinalX) {
    s.addAction(moveTo(sFinalX, 0f, SPLIT_DURATION, Interpolation.pow3Out));
    four.addAction(moveTo(fourFinalX, 0f, SPLIT_DURATION, Interpolation.pow3Out));
    for (int i = 0; i < tudio.length; i++) {
      tudio[i].addAction(sequence(delay(LETTER_DELAY + i * LETTER_STAGGER), fadeIn(LETTER_FADE)));
    }
  }

  private static Texture loadTexture(ResourceService resources, String path) {
    Texture texture = resources.getAsset(path, Texture.class);
    texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    return texture;
  }

  private static Image createImage(Texture texture, float scale) {
    Image image = new Image(texture);
    image.setSize(texture.getWidth() * scale, texture.getHeight() * scale);
    return image;
  }

  private void centreLogo() {
    logo.setPosition(stage.getWidth() / 2f, stage.getHeight() / 2f, Align.center);
  }

  private void skip() {
    if (!finished) {
      logger.debug("Splash screen skipped");
    }
    finish();
  }

  private void finish() {
    finished = true;
  }

  /**
   * @return true once the sequence has finished or been skipped
   */
  public boolean isFinished() {
    return finished;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The stage draws the actors. Re-centre every frame so the logo stays centred after a resize.
    centreLogo();
  }

  @Override
  public void dispose() {
    stage.removeListener(skipListener);
    logo.remove();
    background.remove();
    super.dispose();
  }

  private static String[] allTextures() {
    String[] textures = new String[TUDIO_TEXTURES.length + 3];
    textures[0] = COMBINED_TEXTURE;
    textures[1] = S_TEXTURE;
    textures[2] = FOUR_TEXTURE;
    System.arraycopy(TUDIO_TEXTURES, 0, textures, 3, TUDIO_TEXTURES.length);
    return textures;
  }

  public static String[] getTextures() {
    return allTextures();
  }
}
