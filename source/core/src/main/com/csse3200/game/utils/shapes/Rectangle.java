package com.csse3200.game.utils.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Rectangle */
public class Rectangle extends ShapeActor {
  public Rectangle(Color color, boolean fill, ShapeRenderer shapeRenderer) {
    super(color, fill, shapeRenderer);
  }

  @Override
  protected void drawShape() {
    shape.rect(getX(), getY(), getWidth(), getHeight());
  }
}
