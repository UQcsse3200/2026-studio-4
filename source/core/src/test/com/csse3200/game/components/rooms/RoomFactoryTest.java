package com.csse3200.game.components.rooms;

import static com.csse3200.game.entities.factories.RoomFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.entities.factories.RoomFactory;
import org.junit.jupiter.api.Test;

public class RoomFactoryTest {
  @Test
  void getTileSet_dungeonOne_returnsDungeon1Tileset() {
    assertEquals(DUNGEON1_TILESET, RoomFactory.getTileSet("dungeonOne"));
  }

  @Test
  void getTileSet_dungeonTwo_returnsDungeon2Tileset() {
    assertEquals(DUNGEON2_TILESET, RoomFactory.getTileSet("dungeonTwo"));
  }

  @Test
  void getTileSet_dungeonThree_returnsDungeon3Tileset() {
    assertEquals(DUNGEON3_TILESET, RoomFactory.getTileSet("dungeonThree"));
  }

  @Test
  void getTileSet_dungeonFour_returnsDungeon4Tileset() {
    assertEquals(DUNGEON4_TILESET, RoomFactory.getTileSet("dungeonFour"));
  }

  @Test
  void getTileSet_finalDungeon_returnsDungeon5Tileset() {
    assertEquals(DUNGEON5_TILESET, RoomFactory.getTileSet("finalDungeon"));
  }

  @Test
  void getTileSet_nullDungeonId_returnsDefaultTileset() {
    assertEquals(DEFAULT_TILESET, RoomFactory.getTileSet(null));
  }

  @Test
  void getTileSet_unknownDungeonId_returnsDefaultTileset() {
    assertEquals(DEFAULT_TILESET, RoomFactory.getTileSet("unknownDungeon"));
  }
}
