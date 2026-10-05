package com.csse3200.game.utils.shapes;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
public class ShapeActorTest {
  /** Stub shape concrete implementation */
  private static class TestShape extends ShapeActor {
    TestShape(Color color, boolean fill, ShapeRenderer renderer) {
      super(color, fill, renderer);
    }

    @Override
    protected void drawShape() {}
  }

  @Mock ShapeRenderer renderer;
  @Mock Batch batch;

  @Test
  void shouldStartAndFinishShapeRendering() {
    var shape = new TestShape(Color.WHITE, true, renderer);
    shape.draw(batch, 0f);
    verify(renderer).begin(any());
    verify(renderer).end();
  }

  @Test
  void shouldSetShapeColor() {
    var shape = new TestShape(Color.WHITE, true, renderer);
    shape.draw(batch, 0f);
    verify(renderer).setColor(Color.WHITE);
  }

  @Test
  void shouldSetRendererToBatchProjectionMatrix() {
    var matrix = new Matrix4();
    when(batch.getProjectionMatrix()).thenReturn(matrix);

    var shape = new TestShape(Color.WHITE, true, renderer);
    shape.draw(batch, 0f);

    verify(renderer).setProjectionMatrix(matrix);
  }

  @Test
  void shouldDrawShapeFillOption() {
    var unfilled = new TestShape(Color.WHITE, false, renderer);
    unfilled.draw(batch, 0f);
    verify(renderer).begin(ShapeType.Line);

    var filled = new TestShape(Color.WHITE, true, renderer);
    filled.draw(batch, 0f);
    verify(renderer).begin(ShapeType.Filled);
  }
}
