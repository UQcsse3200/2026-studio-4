package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Covers how the boss moves while the player it is steering by is concealed. */
@ExtendWith(GameExtension.class)
class FinalBossMovementComponentTest {
  private StatusEffectsControllerComponent effects;
  private Entity player;
  private PhysicsMovementComponent movement;
  private FinalBossMovementComponent bossMovement;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);

    effects = mock(StatusEffectsControllerComponent.class);
    player = new Entity().addComponent(effects);
    player.setPosition(5f, 0f);

    movement = mock(PhysicsMovementComponent.class);
    bossMovement =
        new FinalBossMovementComponent(
            player, new FinalBossStageOneConfig(), new FinalBossStageTwoConfig());
    new Entity().addComponent(movement).addComponent(bossMovement).create();
  }

  @Test
  void shouldHoldPositionWhileThePlayerIsConcealed() {
    bossMovement.setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
    when(effects.isConcealed()).thenReturn(true);

    bossMovement.update();

    verify(movement).setMoving(false);
    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }

  @Test
  void shouldStepAgainOnceThePlayerReappears() {
    bossMovement.setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
    when(effects.isConcealed()).thenReturn(true);
    bossMovement.update();
    clearInvocations(movement);

    when(effects.isConcealed()).thenReturn(false);
    bossMovement.update();

    verify(movement).setMoving(true);
    verify(movement).setTarget(any(Vector2.class));
  }

  @Test
  void shouldHoldPositionWhileFleeingFromAConcealedPlayer() {
    bossMovement.setCamera(mock(Camera.class));
    bossMovement.setMode(FinalBossMovementComponent.Mode.FLEE_ALONG_EDGE);
    when(effects.isConcealed()).thenReturn(true);

    bossMovement.update();

    verify(movement).setMoving(false);
    verify(movement, never()).setTarget(any());
    assertEquals(FinalBossMovementComponent.Mode.FLEE_ALONG_EDGE, bossMovement.getMode());
  }

  @Test
  void shouldRemainStoppedWhenConcealmentChanges() {
    when(effects.isConcealed()).thenReturn(true);
    bossMovement.update();
    when(effects.isConcealed()).thenReturn(false);
    bossMovement.update();

    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }
}
