package com.csse3200.game.areas.attic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.attic.AtticTerrain.WallPiece;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtticTerrainTest {
  // Row 0 is the bottom of the room.
  private static final List<String> ROOM =
      List.of(
          " ##### ", //
          " #wpw# ", //
          " Wwlw# ", //
          " ##### ");

  @Test
  void classifiesCharacters() {
    assertTrue(AtticTerrain.isWall('#'));
    assertTrue(AtticTerrain.isWall('W'));
    assertFalse(AtticTerrain.isWall('w'));
    assertTrue(AtticTerrain.isFloor('w'));
    assertTrue(AtticTerrain.isFloor('p'));
    assertTrue(AtticTerrain.isFloor('l'));
    assertTrue(AtticTerrain.isFloor('d'));
    assertFalse(AtticTerrain.isFloor(' '));
    assertFalse(AtticTerrain.isFloor('#'));
  }

  @Test
  void wallFacesTheFloorItBorders() {
    // Bottom wall row: floor above it.
    assertEquals(WallPiece.BOTTOM, AtticTerrain.wallPieceAt(ROOM, 3, 0));
    // Top wall row: floor below it.
    assertEquals(WallPiece.TOP, AtticTerrain.wallPieceAt(ROOM, 3, 3));
    // Left wall (plain and windowed): floor to its right.
    assertEquals(WallPiece.LEFT, AtticTerrain.wallPieceAt(ROOM, 1, 1));
    assertEquals(WallPiece.LEFT, AtticTerrain.wallPieceAt(ROOM, 1, 2));
    // Right wall: floor to its left.
    assertEquals(WallPiece.RIGHT, AtticTerrain.wallPieceAt(ROOM, 5, 1));
  }

  @Test
  void cornersOnlyTouchFloorDiagonally() {
    assertEquals(WallPiece.CORNER_BOTTOM_LEFT, AtticTerrain.wallPieceAt(ROOM, 1, 0));
    assertEquals(WallPiece.CORNER_BOTTOM_RIGHT, AtticTerrain.wallPieceAt(ROOM, 5, 0));
    assertEquals(WallPiece.CORNER_TOP_LEFT, AtticTerrain.wallPieceAt(ROOM, 1, 3));
    assertEquals(WallPiece.CORNER_TOP_RIGHT, AtticTerrain.wallPieceAt(ROOM, 5, 3));
  }

  @Test
  void wallAwayFromFloorHasNoPiece() {
    assertNull(AtticTerrain.wallPieceAt(List.of("###", "###", "###"), 1, 1));
  }

  @Test
  void floorBlocksStayInsideTheFloorSheet() {
    for (char c : new char[] {'w', 'p', 'l', 'd'}) {
      int[] block = AtticTerrain.floorBlockOrigin(c);
      // floors.png is 288x144 with 16px tiles.
      assertTrue(block[0] + block[2] * 16 <= 288, "block " + c + " overflows horizontally");
      assertTrue(block[1] + block[3] * 16 <= 144, "block " + c + " overflows vertically");
    }
  }
}
