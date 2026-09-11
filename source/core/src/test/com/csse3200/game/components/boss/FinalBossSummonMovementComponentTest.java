package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossSummonMovementComponentTest {
  private Entity player;
  private Entity summon;
  private Vector2 position;
  private PhysicsMovementComponent movement;
  private FinalBossExplosiveSummonComponent explosive;
  private GameTime time;
  private List<Entity> activeSummons;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);

    player = new Entity();
    player.setPosition(10f, 0f);

    position = new Vector2();
    movement = mock(PhysicsMovementComponent.class);
    explosive = mock(FinalBossExplosiveSummonComponent.class);
    when(explosive.getTriggerDistance()).thenReturn(0.8f);
    summon = mock(Entity.class);

    when(summon.getPosition()).thenAnswer(invocation -> position.cpy());
    when(summon.getCenterPosition()).thenAnswer(invocation -> position.cpy().add(0.5f, 0.5f));
    when(summon.getComponent(PhysicsMovementComponent.class)).thenReturn(movement);
    when(summon.getComponent(FinalBossExplosiveSummonComponent.class)).thenReturn(explosive);

    activeSummons = new ArrayList<>();
    activeSummons.add(summon);
  }

  @Test
  void shouldScatterAlongAssignedDirection() {
    FinalBossSummonMovementComponent controller = createController(90f);

    controller.update();

    Vector2 destination = captureTarget();
    assertEquals(position.x, destination.x, 0.001f);
    assertTrue(destination.y > position.y);
  }

  @Test
  void shouldRouteAroundPlayerInsteadOfCuttingAcrossCentre() {
    player.setPosition(0f, 0f);
    position.set(3f, 0f);

    FinalBossSummonMovementComponent controller = createController(180f);
    when(time.getDeltaTime()).thenReturn(2f);

    controller.update();

    // The assigned position is on the opposite side, so movement should include
    // a sideways component around the ring instead of heading straight left.
    Vector2 destination = captureTarget();
    assertTrue(Math.abs(destination.y - position.y) > 0.1f);
  }

  @Test
  void shouldSeparateFromNearbySummon() {
    Entity neighbour = new Entity();
    neighbour.setPosition(0f, 0.2f);
    activeSummons.add(neighbour);

    FinalBossSummonMovementComponent controller = createController(0f);

    controller.update();

    Vector2 destination = captureTarget();
    assertTrue(destination.x > position.x);
    assertTrue(destination.y < position.y);
  }

  @Test
  void shouldNotRestartMovementDuringExplosionWarning() {
    FinalBossSummonMovementComponent controller = createController(0f);
    when(explosive.isWarningActive()).thenReturn(true);

    controller.update();

    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any(Vector2.class));
  }

  @Test
  void shouldNotMoveAfterDetonation() {
    FinalBossSummonMovementComponent controller = createController(0f);
    when(explosive.hasDetonated()).thenReturn(true);

    controller.update();

    verify(movement, never()).setMoving(true);
  }

  @Test
  void shouldNotMoveAfterDisposal() {
    FinalBossSummonMovementComponent controller = createController(0f);
    controller.update();
    clearInvocations(movement);

    controller.dispose();
    controller.update();

    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any(Vector2.class));
  }

  private FinalBossSummonMovementComponent createController(float angle) {
    FinalBossSummonMovementComponent controller =
        new FinalBossSummonMovementComponent(player, 1f, angle, activeSummons);
    controller.setEntity(summon);
    controller.create();
    return controller;
  }

  private Vector2 captureTarget() {
    ArgumentCaptor<Vector2> captor = ArgumentCaptor.forClass(Vector2.class);
    verify(movement).setTarget(captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldKeepFlankingAfterPreviousTimeout() {
    player.setPosition(0f, 0f);
    position.set(3f, 0f);

    FinalBossSummonMovementComponent controller = createController(180f);

    // Finish scattering.
    when(time.getDeltaTime()).thenReturn(2f);
    controller.update();
    clearInvocations(movement);

    // Passing the old timeout must not switch to chasing the player's centre.
    when(time.getDeltaTime()).thenReturn(20f);
    controller.update();

    Vector2 destination = captureTarget();
    assertTrue(Math.abs(destination.y - position.y) > 0.1f);
  }

  @Test
  void shouldFollowAssignedSideWhenPlayerMoves() {
    player.setPosition(0f, 0f);
    position.set(3f, 0f);

    FinalBossSummonMovementComponent controller = createController(0f);

    when(time.getDeltaTime()).thenReturn(2f);
    controller.update();
    clearInvocations(movement);

    // The player moves closer, but this summon must regain its right-side slot.
    player.setPosition(2f, 0f);
    when(time.getDeltaTime()).thenReturn(0.1f);
    controller.update();

    Vector2 destination = captureTarget();
    assertTrue(destination.x > position.x);
  }

  @Test
  void shouldKeepSpawnInsideCameraIncludingSpriteSize() {
    when(summon.getScale()).thenAnswer(invocation -> new Vector2(0.75f, 0.75f));

    FinalBossSummonMovementComponent controller = createController(0f);

    OrthographicCamera camera = new OrthographicCamera(4f, 4f);
    camera.position.set(0f, 0f, 0f);
    controller.setCamera(camera);

    Vector2 upperCorner = controller.clampSpawnPosition(new Vector2(10f, 10f));
    assertEquals(1f, upperCorner.x, 0.001f);
    assertEquals(1f, upperCorner.y, 0.001f);

    Vector2 lowerCorner = controller.clampSpawnPosition(new Vector2(-10f, -10f));
    assertEquals(-1.75f, lowerCorner.x, 0.001f);
    assertEquals(-1.75f, lowerCorner.y, 0.001f);

    camera.zoom = 0.5f;

    Vector2 zoomedCorner = controller.clampSpawnPosition(new Vector2(10f, 10f));
    assertEquals(0f, zoomedCorner.x, 0.001f);
    assertEquals(0f, zoomedCorner.y, 0.001f);
  }
}
