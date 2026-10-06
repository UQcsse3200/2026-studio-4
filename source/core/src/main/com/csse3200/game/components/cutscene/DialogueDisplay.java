package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.cutscene.DialogueScript.Line;
import com.csse3200.game.components.cutscene.DialogueScript.Speaker;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The dialogue box along the bottom of the screen: the words on the left, and on the right a framed
 * portrait of whoever is speaking with their name underneath.
 *
 * <p>The box is pixel art built from three nine-patch panels in {@code images/cutscene/}. Text is
 * typed out by a {@link DialogueRunner}; this class only draws it.
 */
public class DialogueDisplay extends UIComponent implements DialogueView {
  private static final Logger logger = LoggerFactory.getLogger(DialogueDisplay.class);

  private static final String TEXT_PANEL = "images/cutscene/panel_text.png";
  private static final String PORTRAIT_PANEL = "images/cutscene/panel_portrait.png";
  private static final String NAME_PANEL = "images/cutscene/panel_nameplate.png";

  /** Art is drawn at 1x then enlarged so the pixels stay chunky. */
  private static final float PIXEL_SCALE = 3f;

  /** The nameplate is smaller so its text fits between the borders. */
  private static final float NAME_PIXEL_SCALE = 2f;

  private static final int PANEL_BORDER = 8;
  private static final int NAME_BORDER = 7;
  private static final float BOX_HEIGHT = 230f;
  private static final float PORTRAIT_SIZE = 170f;
  private static final Color INK = new Color(0.23f, 0.11f, 0.03f, 1f);

  private Table root;
  private TypewriterText text;
  private PortraitActor portrait;
  private Label nameLabel;
  private Image continueHint;
  private Texture hintTexture;
  private final List<Texture> panelTextures = new ArrayList<>();
  private final List<String> loadedAtlases = new ArrayList<>();
  private final List<String> loadedTextures = new ArrayList<>();
  private DialogueRunner runner;
  private DialogueScript script;
  private boolean active;

  @Override
  public void create() {
    super.create();
    if (stage == null) {
      return;
    }
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(new String[] {TEXT_PANEL, PORTRAIT_PANEL, NAME_PANEL});
    resources.loadAll();

    NinePatchDrawable textBg = panel(resources, TEXT_PANEL, PANEL_BORDER, PIXEL_SCALE);
    NinePatchDrawable portraitBg = panel(resources, PORTRAIT_PANEL, PANEL_BORDER, PIXEL_SCALE);
    NinePatchDrawable nameBg = panel(resources, NAME_PANEL, NAME_BORDER, NAME_PIXEL_SCALE);

    text = new TypewriterText(skin.getFont("font_large"), INK);
    portrait = new PortraitActor();
    nameLabel = new Label("", new Label.LabelStyle(skin.getFont("font_small"), INK));
    continueHint = new Image(createHintTexture());
    continueHint.setVisible(false);
    continueHint.addAction(
        Actions.forever(
            Actions.sequence(Actions.moveBy(0f, -5f, 0.35f), Actions.moveBy(0f, 5f, 0.35f))));

    Table textPanel = new Table();
    textPanel.setBackground(textBg);
    textPanel.pad(PANEL_BORDER * PIXEL_SCALE + 6f);
    textPanel.add(text).grow().top().left();
    textPanel.row();
    textPanel.add(continueHint).right().bottom().size(28f, 16f);

    Table portraitFrame = new Table();
    portraitFrame.setBackground(portraitBg);
    portraitFrame.pad(PANEL_BORDER * PIXEL_SCALE * 0.6f);
    portraitFrame.add(portrait).grow();

    Table nameplate = new Table();
    nameplate.setBackground(nameBg);
    nameplate.add(nameLabel).center().pad(2f, 8f, 2f, 8f);

    Table portraitColumn = new Table();
    portraitColumn.add(portraitFrame).size(PORTRAIT_SIZE + 30f, PORTRAIT_SIZE + 20f);
    portraitColumn.row();
    portraitColumn.add(nameplate).growX().height(56f).padTop(-16f);

    Table box = new Table();
    box.add(textPanel).grow().padRight(-8f);
    box.add(portraitColumn).width(PORTRAIT_SIZE + 50f).fillY();

    root = new Table();
    root.setFillParent(true);
    root.bottom().padBottom(28f);
    root.add(box).width(Value.percentWidth(0.82f, root)).maxWidth(1100f).height(BOX_HEIGHT);
    root.setTouchable(Touchable.disabled);
    root.setVisible(false);
    stage.addActor(root);
  }

  private NinePatchDrawable panel(
      ResourceService resources, String path, int border, float pixelScale) {
    Texture texture = resources.getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    panelTextures.add(texture);
    NinePatch patch = new NinePatch(texture, border, border, border, border);
    patch.scale(pixelScale, pixelScale);
    return new NinePatchDrawable(patch);
  }

  /** A small downward triangle telling the player a line is finished. */
  private TextureRegionDrawable createHintTexture() {
    Pixmap pixmap = new Pixmap(7, 4, Pixmap.Format.RGBA8888);
    pixmap.setColor(INK);
    for (int row = 0; row < 4; row++) {
      pixmap.drawLine(row, row, 6 - row, row);
    }
    hintTexture = new Texture(pixmap);
    hintTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    pixmap.dispose();
    return new TextureRegionDrawable(new TextureRegion(hintTexture));
  }

  @Override
  public void show(DialogueScript newScript, DialogueRunner.Listener listener) {
    if (stage == null) {
      // Nothing to draw on (e.g. headless): finish so callers never wait forever.
      listener.onFinished();
      return;
    }
    if (active) {
      close();
    }
    script = newScript;
    loadSpeakerAssets(newScript);
    active = true;
    root.setVisible(true);
    root.toFront();
    runner =
        new DialogueRunner(
            newScript,
            new DialogueRunner.Listener() {
              @Override
              public void onLineShown(int index, Line line) {
                showLine(line);
                listener.onLineShown(index, line);
              }

              @Override
              public void onFinished() {
                listener.onFinished();
              }
            });
    runner.start();
  }

  private void showLine(Line line) {
    Speaker speaker = script.findSpeaker(line.speaker);
    nameLabel.setText(speaker == null || speaker.name == null ? "" : speaker.name);
    bindPortrait(speaker);
    text.setFullText(line.text);
    continueHint.setVisible(false);
  }

  private void bindPortrait(Speaker speaker) {
    portrait.clearPortrait();
    if (speaker == null) {
      return;
    }
    ResourceService resources = ServiceLocator.getResourceService();
    if (speaker.atlas != null && resources.containsAsset(speaker.atlas, TextureAtlas.class)) {
      TextureAtlas atlas = resources.getAsset(speaker.atlas, TextureAtlas.class);
      Array<TextureAtlas.AtlasRegion> frames =
          speaker.animation == null ? new Array<>() : atlas.findRegions(speaker.animation);
      if (frames.isEmpty()) {
        frames = atlas.getRegions();
      }
      if (!frames.isEmpty()) {
        portrait.setAnimation(frames, speaker.frameSeconds);
      }
    } else if (speaker.texture != null && resources.containsAsset(speaker.texture, Texture.class)) {
      portrait.setStill(new TextureRegion(resources.getAsset(speaker.texture, Texture.class)));
    }
  }

  /** Loads each speaker's portrait art, skipping (and logging) anything missing. */
  private void loadSpeakerAssets(DialogueScript newScript) {
    ResourceService resources = ServiceLocator.getResourceService();
    List<String> atlases = new ArrayList<>();
    List<String> textures = new ArrayList<>();
    for (Speaker speaker : newScript.speakers) {
      if (speaker == null) {
        continue;
      }
      collectIfPresent(speaker.atlas, atlases, newScript.id);
      collectIfPresent(speaker.texture, textures, newScript.id);
    }
    resources.loadTextureAtlases(atlases.toArray(new String[0]));
    resources.loadTextures(textures.toArray(new String[0]));
    resources.loadAll();
    loadedAtlases.addAll(atlases);
    loadedTextures.addAll(textures);
  }

  private void collectIfPresent(String path, List<String> into, String scriptId) {
    if (path == null || into.contains(path)) {
      return;
    }
    if (Gdx.files.internal(path).exists()) {
      into.add(path);
    } else {
      logger.warn("Dialogue '{}' references missing portrait asset '{}'", scriptId, path);
    }
  }

  @Override
  public void update() {
    if (!active || runner == null) {
      return;
    }
    runner.update(Gdx.graphics.getDeltaTime());
    text.setRevealed(runner.getVisibleText().length());
    continueHint.setVisible(runner.isLineComplete());
  }

  @Override
  public void advance() {
    if (active && runner != null) {
      runner.advance();
    }
  }

  @Override
  public void close() {
    active = false;
    runner = null;
    if (root != null) {
      root.setVisible(false);
      portrait.clearPortrait();
    }
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null) {
      resources.unloadAssets(loadedAtlases.toArray(new String[0]));
      resources.unloadAssets(loadedTextures.toArray(new String[0]));
    }
    loadedAtlases.clear();
    loadedTextures.clear();
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public float getZIndex() {
    // Drawn after the HUD, which pulls itself to the front every frame.
    return 1001f;
  }

  @Override
  protected void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
    // Scene2D draws the dialogue actors; keep them above the HUD.
    if (active && root != null) {
      root.toFront();
    }
  }

  @Override
  public void dispose() {
    close();
    if (root != null) {
      root.remove();
    }
    if (hintTexture != null) {
      hintTexture.dispose();
    }
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && !panelTextures.isEmpty()) {
      resources.unloadAssets(new String[] {TEXT_PANEL, PORTRAIT_PANEL, NAME_PANEL});
    }
    super.dispose();
  }
}
