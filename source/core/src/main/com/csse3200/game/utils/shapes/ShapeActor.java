package com.csse3200.game.utils.shapes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.Actor;

public abstract class ShapeActor extends Actor {
  protected final ShapeRenderer shape;
  protected final Color shapeColor;
  protected boolean fill;

  /** Package private constructor for testing */
  ShapeActor(Color color, boolean fill, ShapeRenderer renderer) {
    this.shape = renderer;
    this.shapeColor = color;
    this.fill = fill;
  }

  protected ShapeActor(Color color, boolean fill) {
    this.shape = new ShapeRenderer();
    this.shapeColor = color;
    this.fill = fill;
  }

  protected ShapeActor(Color color) {
    this(color, true);
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    batch.end();

    // enable transparency
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

    shape.setProjectionMatrix(batch.getProjectionMatrix());

    shape.begin(fill ? ShapeType.Filled : ShapeType.Line);
    shape.setColor(shapeColor);

    drawShape();

    shape.end();

    Gdx.gl.glDisable(GL20.GL_BLEND);
    batch.begin();
  }

  /**
   * Different shapes should call different methods on {@link #shape}
   *
   * @see ShapeRenderer
   */
  protected abstract void drawShape();
}
