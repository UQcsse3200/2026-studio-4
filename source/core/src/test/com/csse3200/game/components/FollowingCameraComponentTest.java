package com.csse3200.game.components;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class FollowingCameraComponentTest {
  @Mock private Entity player;
  @Mock private CameraComponent camera;
  @Mock private Entity cameraEntity;

  @Test
  void shouldMoveCameraTowardPlayer() {
    when(player.getPosition()).thenReturn(new Vector2(1f, 1f), new Vector2(11f, 1f));
    when(camera.getEntity()).thenReturn(cameraEntity);

    FollowingCameraComponent component = new FollowingCameraComponent();
    component.setEntity(player);
    component.setCamera(camera);
    component.create();
    component.update();

    verify(cameraEntity).setPosition(new Vector2(2f, 1f));
  }
}
