package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FollowingCameraComponentTest {
  private FollowingCameraComponent following;
  private OrthographicCamera camera;
  private Entity cameraEntity;
  private Entity player;

  @BeforeEach
  void setUp() {
    camera = new OrthographicCamera(10f, 8f);
    CameraComponent cameraComponent = new CameraComponent(camera);
    cameraEntity = new Entity().addComponent(cameraComponent);
    cameraEntity.setPosition(20f, 20f);
    cameraEntity.create();
    cameraComponent.update();
    player = new Entity();
    player.setPosition(80f, 50f);
    WallComponent walls = mock(WallComponent.class);
    when(walls.getWallBounds()).thenReturn(new Vector2(100f, 100f));
    following = new FollowingCameraComponent();
    following.setCamera(cameraComponent);
    following.setTarget(player);
    new Entity().addComponent(walls).addComponent(following).create();
  }

  @Test
  void lockKeepsVisibleCameraAndItsEntityFixedWhilePlayerMoves() {
    Object owner = new Object();
    assertTrue(following.lockPosition(owner));
    player.setPosition(90f, 70f);
    following.update();
    assertEquals(new Vector2(20f, 20f), cameraEntity.getPosition());
    assertEquals(20f, camera.position.x);
    assertEquals(20f, camera.position.y);
    cameraEntity.getComponent(CameraComponent.class).update();
    assertEquals(20f, camera.position.x);
  }

  @Test
  void releasesOnlyOwningLocksAndResumesFromTheFixedPosition() {
    Object first = new Object();
    Object second = new Object();
    following.lockPosition(first);
    following.lockPosition(first);
    following.lockPosition(second);
    following.unlockPosition(new Object());
    following.unlockPosition(first);
    following.update();
    assertEquals(new Vector2(20f, 20f), cameraEntity.getPosition());
    following.unlockPosition(second);
    following.update();
    assertEquals(26.05f, cameraEntity.getPosition().x, 0.0001f);
    assertEquals(23.05f, cameraEntity.getPosition().y, 0.0001f);
  }

  @Test
  void aTargetChangedWhileLockedIsStillFollowedAfterRelease() {
    Object owner = new Object();
    following.lockPosition(owner);
    Entity newTarget = new Entity();
    newTarget.setPosition(30f, 30f);
    following.setTarget(newTarget);
    following.unlockPosition(owner);
    following.update();
    assertEquals(new Vector2(21.05f, 21.05f), cameraEntity.getPosition());
  }

  @Test
  void missingCameraCannotAcquireLockAndNullOwnerIsRejected() {
    FollowingCameraComponent unconfigured = new FollowingCameraComponent();
    assertFalse(unconfigured.lockPosition(new Object()));
    assertThrows(IllegalArgumentException.class, () -> following.lockPosition(null));
  }
}
