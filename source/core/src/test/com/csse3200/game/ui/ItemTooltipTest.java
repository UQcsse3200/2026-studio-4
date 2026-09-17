package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.StrengthCharm;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests which lines an item's tooltip shows.
 *
 * <p>{@link ItemTooltip#lines} is tested rather than the table built from it, so the tests stay
 * free of fonts, skins and a running libGDX application. Building the table itself is a plain
 * scene2d layout over these same lines.
 */
class ItemTooltipTest {

  @Test
  void shouldShowNameEffectAndDescription() {
    StrengthCharm charm = new StrengthCharm();

    List<String> lines = ItemTooltip.lines(charm);

    assertEquals(3, lines.size());
    assertEquals(charm.getName(), lines.get(0));
    assertEquals(charm.getEffectSummary(), lines.get(1));
    assertEquals(charm.getDescription(), lines.get(2));
  }

  @Test
  void shouldOmitEffectLineForItemWithNoEffect() {
    Item plainItem = new PlainItem();

    List<String> lines = ItemTooltip.lines(plainItem);

    assertTrue(plainItem.getEffectSummary().isEmpty());
    assertEquals(2, lines.size());
    assertEquals("Lantern", lines.get(0));
    assertEquals("Lights the way.", lines.get(1));
  }

  /** An item with no stat effect, used to check the effect line is left out. */
  private static class PlainItem extends Item {
    PlainItem() {
      super("Lantern", "Lights the way.", "images/heart.png");
    }

    @Override
    public void pickUp(Entity player) {
      // nothing to do
    }

    @Override
    public void drop(Entity player) {
      // nothing to do
    }
  }
}
