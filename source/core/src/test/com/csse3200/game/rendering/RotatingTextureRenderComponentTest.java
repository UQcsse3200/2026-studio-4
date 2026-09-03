package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class RotatingTextureRenderComponentTest {
  @Mock Texture texture;
  @Mock SpriteBatch spriteBatch;
  @Mock Entity entity;

  private TextureRegion region;
  private RotatingTextureRenderComponent component;

  @BeforeEach
  void beforeEach() {
    region = new TextureRegion(texture, 0, 0, 16, 16);
    component = new RotatingTextureRenderComponent(region);
    component.setEntity(entity);
  }

  @Test
  void shouldDrawCentredOnEntityCentreUsingEntityScale() {
    when(entity.getCenterPosition()).thenReturn(new Vector2(4f, 4f));
    when(entity.getScale()).thenReturn(new Vector2(2f, 1f));

    component.render(spriteBatch);

    // Centred on (4,4) at 2x1 -> bottom-left (3, 3.5), origin at the sprite's own centre.
    verify(spriteBatch).draw(region, 3f, 3.5f, 1f, 0.5f, 2f, 1f, 1f, 1f, 0f);
  }

  @Test
  void shouldPreferVisualScaleOverEntityScale() {
    when(entity.getCenterPosition()).thenReturn(new Vector2(4f, 4f));
    component.setVisualScale(new Vector2(1f, 1f));

    component.render(spriteBatch);

    // A square sprite keeps its shape even though the entity is not square.
    verify(spriteBatch).draw(region, 3.5f, 3.5f, 0.5f, 0.5f, 1f, 1f, 1f, 1f, 0f);
  }

  @Test
  void shouldAddRotationOffsetToRotation() {
    when(entity.getCenterPosition()).thenReturn(new Vector2(0f, 0f));
    component.setVisualScale(new Vector2(1f, 1f));
    component.setRotation(90f);
    component.setRotationOffset(-45f);

    component.render(spriteBatch);

    verify(spriteBatch).draw(region, -0.5f, -0.5f, 0.5f, 0.5f, 1f, 1f, 1f, 1f, 45f);
  }

  @Test
  void shouldShiftSpriteAlongFacingWithoutMovingTheEntity() {
    when(entity.getCenterPosition()).thenReturn(new Vector2(4f, 4f));
    component.setVisualScale(new Vector2(1f, 1f));
    component.setVisualOffset(new Vector2(-2f, 0f));
    component.setRotation(90f);

    component.render(spriteBatch);

    // Facing up, so a -2 offset along the facing pulls the sprite down to (4,2).
    verify(spriteBatch).draw(region, 3.5f, 1.5f, 0.5f, 0.5f, 1f, 1f, 1f, 1f, 90f);
  }

  @Test
  void shouldRotateOffsetByFacingOnlyNotBySpriteOffset() {
    when(entity.getCenterPosition()).thenReturn(new Vector2(0f, 0f));
    component.setVisualScale(new Vector2(1f, 1f));
    component.setVisualOffset(new Vector2(-1f, 0f));
    component.setRotation(0f);
    component.setRotationOffset(-135f);

    component.render(spriteBatch);

    // The sprite turns by -135, but the anchor still shifts along the 0-degree facing.
    verify(spriteBatch).draw(region, -1.5f, -0.5f, 0.5f, 0.5f, 1f, 1f, 1f, 1f, -135f);
  }

  @Test
  void shouldReportRotationWithoutOffset() {
    component.setRotation(30f);
    component.setRotationOffset(-45f);

    assertEquals(30f, component.getRotation());
    assertEquals(-45f, component.getRotationOffset());
  }

  @Test
  void shouldCopyVisualScaleOnSetAndGet() {
    Vector2 supplied = new Vector2(1f, 1f);
    component.setVisualScale(supplied);
    supplied.set(9f, 9f);

    assertEquals(new Vector2(1f, 1f), component.getVisualScale());
    assertNotSame(component.getVisualScale(), component.getVisualScale());
  }

  @Test
  void shouldFallBackToEntityScaleWhenVisualScaleCleared() {
    component.setVisualScale(new Vector2(1f, 1f));
    component.setVisualScale(null);

    assertNull(component.getVisualScale());
  }
}
