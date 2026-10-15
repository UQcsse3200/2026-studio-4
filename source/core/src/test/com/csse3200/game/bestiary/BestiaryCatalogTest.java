package com.csse3200.game.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.util.List;
import org.junit.jupiter.api.Test;

class BestiaryCatalogTest {
  private static BestiaryEntry entry(EnemyType enemy) {
    return new BestiaryEntry(
        enemy, "Name", "Description", "Abilities", "Weakness", "images/test.png");
  }

  @Test
  void keepsOrderAndFindsByEnemy() {
    BestiaryEntry beetle = entry(EnemyType.BEETLE);
    BestiaryEntry wasp = entry(EnemyType.WASP);
    BestiaryCatalog catalog = new BestiaryCatalog(List.of(beetle, wasp));
    assertEquals(List.of(beetle, wasp), catalog.entries());
    assertSame(wasp, catalog.find(EnemyType.WASP));
    assertTrue(catalog.contains(EnemyType.BEETLE));
    assertEquals(2, catalog.size());
  }

  @Test
  void unknownEnemyHasNoEntry() {
    BestiaryCatalog catalog = new BestiaryCatalog(List.of(entry(EnemyType.BEETLE)));
    assertNull(catalog.find(EnemyType.GOLEM));
    assertFalse(catalog.contains(EnemyType.GOLEM));
  }

  @Test
  void rejectsDuplicateAndEmptyCatalogues() {
    List<BestiaryEntry> duplicates = List.of(entry(EnemyType.CRAB), entry(EnemyType.CRAB));
    assertThrows(IllegalArgumentException.class, () -> new BestiaryCatalog(duplicates));
    List<BestiaryEntry> empty = List.of();
    assertThrows(IllegalArgumentException.class, () -> new BestiaryCatalog(empty));
  }

  @Test
  void rejectsEntriesWithMissingFields() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry(null, "Name", "Desc", "Abilities", "Weakness", "img.png"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry(EnemyType.CRAB, " ", "Desc", "Abilities", "Weakness", "img.png"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BestiaryEntry(EnemyType.CRAB, "Name", "Desc", "Abilities", null, "img.png"));
  }
}
