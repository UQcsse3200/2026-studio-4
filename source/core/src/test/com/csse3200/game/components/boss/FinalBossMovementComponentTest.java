package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossMovementComponentTest {
  private StatusEffectsControllerComponent effects;
  private Entity player;
  private PhysicsMovementComponent movement;
  private FinalBossMovementComponent bossMovement;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    effects = mock(StatusEffectsControllerComponent.class);
    player = new Entity().addComponent(effects);
    player.setPosition(2f, 0f);
    movement = mock(PhysicsMovementComponent.class);
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    config.movementTargetDistance = 3f;
    config.movementTargetRefreshInterval = 10f;
    config.bossVisibilityHalfWidth = 100f;
    config.bossVisibilityHalfHeight = 100f;
    bossMovement = new FinalBossMovementComponent(player, config);
    new Entity().addComponent(movement).addComponent(bossMovement).create();
  }

  @Test
  void shouldStopFleeingAndImmediatelyRetargetAfterVisibilityReturns() {
    bossMovement.setMode(FinalBossMovementComponent.Mode.FLEE_PLAYER);
    bossMovement.update();
    verify(movement).setTarget(new Vector2(-3f, 0f));
    clearInvocations(movement);

    when(effects.isConcealed()).thenReturn(true);
    player.setPosition(-2f, 0f);
    bossMovement.update();
    verify(movement).setMoving(false);
    verify(movement, never()).setTarget(any());
    assertEquals(FinalBossMovementComponent.Mode.FLEE_PLAYER, bossMovement.getMode());
    clearInvocations(movement);

    // No elapsed frame time: invisibility must reset the target refresh timer.
    when(effects.isConcealed()).thenReturn(false);
    bossMovement.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(new Vector2(3f, 0f));
  }

  @Test
  void shouldNotBeginFleeingFromInvisiblePlayer() {
    when(effects.isConcealed()).thenReturn(true);
    bossMovement.setMode(FinalBossMovementComponent.Mode.FLEE_PLAYER);
    bossMovement.update();
    verify(movement).setMoving(false);
    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }

  @Test
  void shouldContinueWanderingWhilePlayerIsInvisible() {
    when(effects.isConcealed()).thenReturn(true);
    bossMovement.setMode(FinalBossMovementComponent.Mode.WANDER_AVOID_SUMMONS);
    bossMovement.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(any(Vector2.class));
    verify(movement, never()).setMoving(false);
  }

  @Test
  void shouldStillAvoidSummonsWhilePlayerIsInvisible() {
    when(effects.isConcealed()).thenReturn(true);
    Entity summon = new Entity();
    summon.setPosition(0.1f, 0f);
    bossMovement.setActiveSummons(List.of(summon));
    bossMovement.setMode(FinalBossMovementComponent.Mode.WANDER_AVOID_SUMMONS);
    bossMovement.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(new Vector2(-3f, 0f));
  }

  @Test
  void shouldRemainStoppedWhenVisibilityChanges() {
    when(effects.isConcealed()).thenReturn(true);
    bossMovement.update();
    when(effects.isConcealed()).thenReturn(false);
    bossMovement.update();
    verify(movement, times(2)).setMoving(false);
    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }
}
