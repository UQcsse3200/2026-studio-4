package com.csse3200.game.components.rooms;

public enum Direction {
  UP(0, 1),
  RIGHT(1, 0),
  DOWN(0, -1),
  LEFT(-1, 0),
  UP_RIGHT(1, 1),
  UP_LEFT(-1, 1),
  DOWN_RIGHT(1, -1),
  DOWN_LEFT(-1, -1);

  public final int dx;
  public final int dy;

  Direction(int dx, int dy) {
    this.dx = dx;
    this.dy = dy;
  }
}
