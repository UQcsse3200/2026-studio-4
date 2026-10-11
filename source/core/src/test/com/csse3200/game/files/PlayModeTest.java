package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayModeTest {
  @Test
  void blankNameBecomesPlayer() {
    assertEquals(PlayMode.DEFAULT_NAME, PlayMode.cleanName(null));
    assertEquals(PlayMode.DEFAULT_NAME, PlayMode.cleanName("   "));
  }

  @Test
  void collapsesSpacesAndCutsLongNames() {
    assertEquals("Ada Lovelace", PlayMode.cleanName("  Ada   Lovelace  "));
    assertEquals("ABCDEFGHIJKLMNOP", PlayMode.cleanName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
  }

  @Test
  void summaryDescribesOfflineAndOnline() {
    assertEquals("Offline. This game stays on this computer.", PlayMode.summary(false, "Ada"));
    assertEquals("Online preference saved for Ada.", PlayMode.summary(true, "  Ada  "));
  }
}
