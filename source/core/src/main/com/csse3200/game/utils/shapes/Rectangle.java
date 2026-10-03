package com.csse3200.game.utils.shapes;

import com.badlogic.gdx.graphics.Color;

/** Rectangle */
public class Rectangle extends ShapeActor {
  public Rectangle(Color color) {
    super(color);
  }

  @Override
  protected void drawShape() {
    shape.rect(getX(), getY(), getWidth(), getHeight());
  }
}
