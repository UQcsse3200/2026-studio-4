package com.csse3200.game.components;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.WallComponent;
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

  @Mock private WallComponent wall;

  @Mock private Entity room;

  @Test
  void shouldMoveCameraTowardPlayer() {
    when(room.getCenterPosition()).thenReturn(new Vector2(1f, 1f));
    when(player.getCenterPosition()).thenReturn(new Vector2(11f, 1f));
    when(camera.getEntity()).thenReturn(cameraEntity);
    when(camera.getCameraSize()).thenReturn(new Vector2(1f, 1f));
    when(room.getComponent(WallComponent.class)).thenReturn(wall);
    when(wall.getWallBounds()).thenReturn(new Vector2(100f, 100f));

    FollowingCameraComponent component = new FollowingCameraComponent();
    component.setEntity(room);
    component.setCamera(camera);
    component.setTarget(player);
    component.create();
    component.update();

    verify(cameraEntity).setPosition(new Vector2(2f, 1.5f));
  }
}
