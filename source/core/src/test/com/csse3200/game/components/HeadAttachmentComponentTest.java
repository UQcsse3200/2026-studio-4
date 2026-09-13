package com.csse3200.game.components;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeadAttachmentComponentTest {

  @Mock private Entity mainBody;

  @Mock private Entity entity;

  private HeadAttachmentComponent headAttachmentComponent;

  @BeforeEach
  void setUp() {
    Vector2 offset = new Vector2(2f, 0f);
    headAttachmentComponent = new HeadAttachmentComponent(mainBody, offset);
    headAttachmentComponent.setEntity(entity);
  }

  @Test
  void testSetEntity() {
    when(mainBody.getPosition()).thenReturn(new Vector2(5f, 5f));
    headAttachmentComponent.update();
    verify(entity).setPosition(new Vector2(7f, 5f));
  }

  @Test
  void shouldNotCrashWhenMainBodyPositionIsNull() {
    when(mainBody.getPosition()).thenReturn(null);
    headAttachmentComponent.update();
    verify(entity, never()).setPosition(any(Vector2.class));
  }
}
