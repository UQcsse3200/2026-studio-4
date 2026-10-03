package com.csse3200.game.files;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for reading Java objects from JSON files.
 *
 * <p>A generic method is provided already, but methods for reading specific classes can be added
 * for more control.
 */
public class FileLoader {
  private static final Logger logger = LoggerFactory.getLogger(FileLoader.class);
  static final Json json = new Json();
  private static final int SAVE_SLOT_COUNT = 3;

  private static String saveFileName(int slot) {
    validateSlot(slot);
    return "save_slot_" + slot + ".json";
  }

  private static String previewFileName(int slot) {
    validateSlot(slot);
    return "save_slot_" + slot + "_preview.png";
  }

  private static void validateSlot(int slot) {
    if (slot < 1 || slot > SAVE_SLOT_COUNT) {
      throw new IllegalArgumentException("Invalid save slot: " + slot);
    }
  }

  /**
   * Read generic Java classes from a JSON file. Properties in the JSON file will override class
   * defaults.
   *
   * @param type class type
   * @param filename file to read from
   * @param <T> Class type to read JSON into
   * @return instance of class, may be null
   */
  public static <T> T readClass(Class<T> type, String filename) {
    return readClass(type, filename, Location.INTERNAL);
  }

  /**
   * Read generic Java classes from a JSON file. Properties in the JSON file will override class
   * defaults.
   *
   * @param type class type
   * @param filename file to read from
   * @param location File storage type. See
   *     https://github.com/libgdx/libgdx/wiki/File-handling#file-storage-types
   * @param <T> Class type to read JSON into
   * @return instance of class, may be null
   */
  public static <T> T readClass(Class<T> type, String filename, Location location) {
    logger.debug("Reading class {} from {}", type.getSimpleName(), filename);
    FileHandle file = getFileHandle(filename, location);
    if (file == null) {
      logger.error("Failed to create file handle for {}", filename);
      return null;
    }

    T object;
    try {
      object = json.fromJson(type, file);
    } catch (Exception e) {
      logger.error(e.getMessage());
      return null;
    }
    if (object == null) {
      String path = file.path();
      logger.error("Error creating {} class instance from {}", type.getSimpleName(), path);
    }
    return object;
  }

  /**
   * Write generic Java classes to a JSON file.
   *
   * @param object Java object to write.
   * @param filename File to write to.
   */
  public static void writeClass(Object object, String filename) {
    writeClass(object, filename, Location.EXTERNAL);
  }

  /**
   * Write generic Java classes to a JSON file.
   *
   * @param object Java object to write.
   * @param filename File to write to.
   * @param location File storage type. See
   *     https://github.com/libgdx/libgdx/wiki/File-handling#file-storage-types
   */
  public static void writeClass(Object object, String filename, Location location) {
    logger.debug("Reading class {} from {}", object.getClass().getSimpleName(), filename);
    FileHandle file = getFileHandle(filename, location);
    assert file != null;
    file.writeString(json.prettyPrint(object), false);
  }

  private static FileHandle getFileHandle(String filename, Location location) {
    switch (location) {
      case CLASSPATH:
        return Gdx.files.classpath(filename);
      case INTERNAL:
        return Gdx.files.internal(filename);
      case LOCAL:
        return Gdx.files.local(filename);
      case EXTERNAL:
        return Gdx.files.external(filename);
      case ABSOLUTE:
        return Gdx.files.absolute(filename);
      default:
        return null;
    }
  }

  public enum Location {
    CLASSPATH,
    INTERNAL,
    LOCAL,
    EXTERNAL,
    ABSOLUTE
  }

  public static void save(GameSaveData saveData, int slot) {
    writeClass(saveData, saveFileName(slot), Location.LOCAL);
  }

  public static GameSaveData load(int slot) {
    return readClass(GameSaveData.class, saveFileName(slot), Location.LOCAL);
  }

  public static boolean saveExists(int slot) {
    return Gdx.files.local(saveFileName(slot)).exists();
  }

  public static FileHandle getSavePreview(int slot) {
    return Gdx.files.local(previewFileName(slot));
  }

  public static void deleteSaveSlot(int slot) {
    Gdx.files.local(saveFileName(slot)).delete();
    getSavePreview(slot).delete();
  }

  public static void savePreview(int slot) {
    int width = Gdx.graphics.getBackBufferWidth();
    int height = Gdx.graphics.getBackBufferHeight();
    Pixmap framebuffer = Pixmap.createFromFrameBuffer(0, 0, width, height);
    Pixmap upright = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    try {
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          upright.drawPixel(x, height - y - 1, framebuffer.getPixel(x, y));
        }
      }
      PixmapIO.writePNG(getSavePreview(slot), upright);
    } finally {
      framebuffer.dispose();
      upright.dispose();
    }
  }
}
