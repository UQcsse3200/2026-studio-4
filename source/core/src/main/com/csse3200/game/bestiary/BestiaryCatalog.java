package com.csse3200.game.bestiary;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All bestiary entries, in display order, looked up by enemy type. */
public class BestiaryCatalog {
  private final List<BestiaryEntry> entries;
  private final Map<EnemyType, BestiaryEntry> byEnemy;

  public BestiaryCatalog(List<BestiaryEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      throw new IllegalArgumentException("Bestiary needs at least one entry");
    }
    Map<EnemyType, BestiaryEntry> indexed = new LinkedHashMap<>();
    for (BestiaryEntry entry : entries) {
      if (entry == null) {
        throw new IllegalArgumentException("Bestiary entry cannot be null");
      }
      if (indexed.putIfAbsent(entry.enemy(), entry) != null) {
        throw new IllegalArgumentException("Duplicate bestiary entry: " + entry.enemy());
      }
    }
    this.entries = List.copyOf(entries);
    this.byEnemy = Collections.unmodifiableMap(indexed);
  }

  /** All entries in the order they appear in the bestiary. */
  public List<BestiaryEntry> entries() {
    return entries;
  }

  /** The entry for an enemy type, or null if the bestiary has no page for it. */
  public BestiaryEntry find(EnemyType enemy) {
    return byEnemy.get(enemy);
  }

  public boolean contains(EnemyType enemy) {
    return byEnemy.containsKey(enemy);
  }

  public int size() {
    return entries.size();
  }

  /** Loads and checks the bestiary from a JSON file in the game's assets. */
  public static BestiaryCatalog load(String assetPath) {
    CatalogConfig config = FileLoader.readClass(CatalogConfig.class, assetPath);
    if (config == null || config.entries == null) {
      throw new IllegalArgumentException("Invalid bestiary file: " + assetPath);
    }
    List<BestiaryEntry> loaded = new ArrayList<>();
    for (EntryConfig entry : config.entries) {
      if (entry == null) {
        throw new IllegalArgumentException("Empty entry in bestiary file: " + assetPath);
      }
      loaded.add(
          new BestiaryEntry(
              entry.enemy,
              entry.name,
              entry.description,
              entry.abilities,
              entry.weakness,
              entry.texture));
    }
    return new BestiaryCatalog(loaded);
  }

  /** JSON layout of the bestiary file. */
  public static class CatalogConfig {
    public EntryConfig[] entries;
  }

  /** JSON layout of one entry. */
  public static class EntryConfig {
    public EnemyType enemy;
    public String name;
    public String description;
    public String abilities;
    public String weakness;
    public String texture;
  }
}
