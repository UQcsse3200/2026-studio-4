package com.csse3200.game.utils.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Circle */
public class Circle extends ShapeActor {
  public Circle(Color color, boolean fill, ShapeRenderer shapeRenderer) {
    super(color, fill, shapeRenderer);
  }

  @Override
  protected void drawShape() {
    shape.circle(getX() + getWidth() / 2f, getY() + getHeight() / 2f, getHeight() / 2f);
  }
}
