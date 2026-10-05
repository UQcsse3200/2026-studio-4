package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Displays the victory screen with an animated GIF, looping win music, and a button to return to
 * the main menu.
 */
public class WinScreen extends ScreenAdapter {
  private static final String WIN_GIF = "images/win_dance.gif";
  private static final float TITLE_FONT_SCALE = 4.0f;

  private final GdxGame game;
  private final Renderer renderer;
  private final List<GifFrame> frames;
  private Image gifImage;
  private int currentFrame;
  private float frameElapsed;
  private Music winMusic;

  /**
   * Creates the victory screen, starts its music, loads the animation, and builds its interface.
   *
   * @param game game instance used to return to the main menu
   */
  public WinScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    winMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/win_music.mp3"));
    winMusic.setLooping(true);
    winMusic.play();

    renderer = RenderFactory.createRenderer();
    frames = loadGifFrames();
    createUI();
  }

  private List<GifFrame> loadGifFrames() {
    List<GifFrame> decodedFrames = new ArrayList<>();
    ImageReader reader = null;

    try (InputStream input = Gdx.files.internal(WIN_GIF).read();
        ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
      if (imageInput == null) {
        throw new IOException("Unable to open GIF image stream");
      }
      reader = ImageIO.getImageReadersByFormatName("gif").next();
      reader.setInput(imageInput, false, false);

      int frameCount = reader.getNumImages(true);
      if (frameCount == 0) {
        throw new IOException("GIF contains no frames");
      }

      Node streamMetadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
      int width = getIntAttribute(streamMetadata, "LogicalScreenDescriptor", "logicalScreenWidth");
      int height =
          getIntAttribute(streamMetadata, "LogicalScreenDescriptor", "logicalScreenHeight");
      if (width <= 0 || height <= 0) {
        width = reader.getWidth(0);
        height = reader.getHeight(0);
      }

      BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      BufferedImage previousCanvas = null;
      Rectangle previousBounds = null;
      String previousDisposal = "none";

      for (int i = 0; i < frameCount; i++) {
        canvas =
            applyPreviousFrameDisposal(canvas, previousCanvas, previousBounds, previousDisposal);

        Node metadata = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
        int x = getIntAttribute(metadata, "ImageDescriptor", "imageLeftPosition");
        int y = getIntAttribute(metadata, "ImageDescriptor", "imageTopPosition");
        BufferedImage frame = reader.read(i);
        String disposal = getAttribute(metadata, "GraphicControlExtension", "disposalMethod");
        int delayCentiseconds = getIntAttribute(metadata, "GraphicControlExtension", "delayTime");

        previousCanvas = "restoreToPrevious".equals(disposal) ? copyImage(canvas) : null;

        Graphics2D graphics = canvas.createGraphics();
        graphics.drawImage(frame, x, y, null);
        graphics.dispose();

        decodedFrames.add(
            new GifFrame(createTexture(canvas), Math.max(2, delayCentiseconds) / 100f));
        previousBounds = new Rectangle(x, y, frame.getWidth(), frame.getHeight());
        previousDisposal = disposal;
      }
    } catch (IOException exception) {
      disposeFrames(decodedFrames);
      throw new IllegalStateException("Failed to load win screen GIF: " + WIN_GIF, exception);
    } finally {
      if (reader != null) {
        reader.dispose();
      }
    }

    return decodedFrames;
  }

  private BufferedImage applyPreviousFrameDisposal(
      BufferedImage canvas,
      BufferedImage previousCanvas,
      Rectangle previousBounds,
      String previousDisposal) {
    if (previousBounds == null) {
      return canvas;
    }
    if ("restoreToBackgroundColor".equals(previousDisposal)) {
      Graphics2D graphics = canvas.createGraphics();
      graphics.setComposite(AlphaComposite.Clear);
      graphics.fill(previousBounds);
      graphics.dispose();
    } else if ("restoreToPrevious".equals(previousDisposal) && previousCanvas != null) {
      return previousCanvas;
    }
    return canvas;
  }

  private Texture createTexture(BufferedImage image) {
    try {
      byte[] pngBytes;
      try (java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
        if (!ImageIO.write(image, "png", output)) {
          throw new IOException("Unable to encode GIF frame as PNG");
        }
        pngBytes = output.toByteArray();
      }

      Pixmap pixmap = new Pixmap(pngBytes, 0, pngBytes.length);
      try {
        return new Texture(pixmap);
      } finally {
        pixmap.dispose();
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to create win screen GIF frame texture", exception);
    }
  }

  private BufferedImage copyImage(BufferedImage image) {
    BufferedImage copy =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = copy.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return copy;
  }

  private int getIntAttribute(Node root, String nodeName, String attributeName) {
    String value = getAttribute(root, nodeName, attributeName);
    return value.isEmpty() ? 0 : Integer.parseInt(value);
  }

  private String getAttribute(Node root, String nodeName, String attributeName) {
    if (root == null) {
      return "";
    }
    if (nodeName.equals(root.getNodeName())) {
      NamedNodeMap attributes = root.getAttributes();
      Node attribute = attributes == null ? null : attributes.getNamedItem(attributeName);
      return attribute == null ? "" : attribute.getNodeValue();
    }
    for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
      String value = getAttribute(child, nodeName, attributeName);
      if (!value.isEmpty()) {
        return value;
      }
    }
    return "";
  }

  private void disposeFrames(List<GifFrame> gifFrames) {
    for (GifFrame frame : gifFrames) {
      frame.texture.dispose();
    }
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));

    Table table = new Table();
    table.setFillParent(true);
    table.pad(24f);

    Label title = new Label("YOU WIN!", skin);
    title.setFontScale(TITLE_FONT_SCALE);

    gifImage = new Image(new TextureRegionDrawable(new TextureRegion(frames.get(0).texture)));
    gifImage.setScaling(Scaling.fit);

    TextButton menuButton = new TextButton("Main Menu", skin);
    menuButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            game.setScreen(GdxGame.ScreenType.MAIN_MENU);
          }
        });

    table.add(gifImage).expand().fill().row();
    table.add(menuButton).bottom().padTop(16f).width(240f).height(64f);
    stage.addActor(table);
    Table titleOverlay = new Table();
    titleOverlay.setFillParent(true);
    titleOverlay.add(title).center();
    stage.addActor(titleOverlay);

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  /** Advances the GIF animation and renders the victory screen. */
  @Override
  public void render(float delta) {
    frameElapsed += delta;
    while (frameElapsed >= frames.get(currentFrame).duration) {
      frameElapsed -= frames.get(currentFrame).duration;
      currentFrame = (currentFrame + 1) % frames.size();
      gifImage.setDrawable(
          new TextureRegionDrawable(new TextureRegion(frames.get(currentFrame).texture)));
    }

    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  /**
   * Updates the renderer viewport after the window size changes.
   *
   * @param width new window width in pixels
   * @param height new window height in pixels
   */
  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  /** Stops and releases the win music and disposes of screen textures and services. */
  @Override
  public void dispose() {
    winMusic.stop();
    winMusic.dispose();

    disposeFrames(frames);
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }

  private static class GifFrame {
    private final Texture texture;
    private final float duration;

    private GifFrame(Texture texture, float duration) {
      this.texture = texture;
      this.duration = duration;
    }
  }
}
