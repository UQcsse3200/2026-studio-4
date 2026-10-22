package com.csse3200.game.areas.attic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Checks the prop catalogue against the real sheets (tests run from core/assets). */
class AtticPropTest {
  private final Map<String, BufferedImage> sheets = new HashMap<>();

  private BufferedImage sheet(String path) throws IOException {
    BufferedImage image = sheets.get(path);
    if (image == null) {
      image = ImageIO.read(new File(path));
      sheets.put(path, image);
    }
    return image;
  }

  @Test
  void everySheetExistsAndIsTileAligned() throws IOException {
    for (String path : AtticSprites.paths()) {
      BufferedImage image = sheet(path);
      assertNotNull(image, path);
      assertEquals(0, image.getWidth() % AtticSprites.TILE_PIXELS, path);
      assertEquals(0, image.getHeight() % AtticSprites.TILE_PIXELS, path);
    }
  }

  @Test
  void everyPropRectangleIsInsideItsSheetAndNotBlank() throws IOException {
    for (AtticProp prop : AtticProp.values()) {
      BufferedImage image = sheet(prop.getSheet());
      assertTrue(prop.getX() >= 0 && prop.getY() >= 0, prop.name());
      assertTrue(prop.getX() + prop.getWidth() <= image.getWidth(), prop + " overflows width");
      assertTrue(prop.getY() + prop.getHeight() <= image.getHeight(), prop + " overflows height");
      assertTrue(hasOpaquePixel(image, prop), prop + " points at an empty part of the sheet");
    }
  }

  private static boolean hasOpaquePixel(BufferedImage image, AtticProp prop) {
    for (int y = prop.getY(); y < prop.getY() + prop.getHeight(); y++) {
      for (int x = prop.getX(); x < prop.getX() + prop.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 0) {
          return true;
        }
      }
    }
    return false;
  }

  @Test
  void tomesGlowAndRugsLieFlat() {
    for (AtticProp prop : AtticProp.values()) {
      boolean tome = prop.name().startsWith("TOME_");
      assertEquals(tome, prop.getGlow() != null, prop.name());
      if (prop.isFloorCovering()) {
        assertFalse(prop.isSolid(), prop + " is a rug but blocks movement");
      }
    }
  }

  @Test
  void stretchableTableUsesRequestedLength() {
    assertEquals(48, AtticProp.READING_TABLE.drawnWidth(0));
    assertEquals(9 * 16, AtticProp.READING_TABLE.drawnWidth(9));
    assertEquals(32, AtticProp.READING_TABLE.drawnWidth(1)); // never shorter than both caps
    assertEquals(48, AtticProp.BOOKSHELF_TALL.drawnWidth(9)); // fixed-size props ignore length
  }

  @Test
  void unknownNamesAreReportedClearly() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> AtticProp.fromName("BEANBAG"));
    assertTrue(e.getMessage().contains("BEANBAG"));
    assertThrows(IllegalArgumentException.class, () -> AtticProp.fromName(null));
    assertEquals(AtticProp.TOME_FROST, AtticProp.fromName("TOME_FROST"));
  }
}
