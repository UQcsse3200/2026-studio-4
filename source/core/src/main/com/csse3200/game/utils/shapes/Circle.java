package com.csse3200.game.utils.shapes;

import com.badlogic.gdx.graphics.Color;

/** Circle */
public class Circle extends ShapeActor {
  public Circle(Color color) {
    super(color);
  }

  @Override
  protected void drawShape() {
    shape.circle(getX() + getWidth() / 2f, getY() + getHeight() / 2f, getHeight() / 2f);
  }
}
