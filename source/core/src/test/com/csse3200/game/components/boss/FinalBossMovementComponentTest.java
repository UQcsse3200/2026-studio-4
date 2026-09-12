package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
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
  private PlayerAbilitiesComponent abilities;
  private Entity player;
  private PhysicsMovementComponent movement;
  private FinalBossMovementComponent bossMovement;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    abilities = mock(PlayerAbilitiesComponent.class);
    player = new Entity().addComponent(abilities);
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

    when(abilities.isActive(Invisibility.class)).thenReturn(true);
    player.setPosition(-2f, 0f);
    bossMovement.update();
    verify(movement).setMoving(false);
    verify(movement, never()).setTarget(any());
    assertEquals(FinalBossMovementComponent.Mode.FLEE_PLAYER, bossMovement.getMode());
    clearInvocations(movement);

    // No elapsed frame time: invisibility must reset the target refresh timer.
    when(abilities.isActive(Invisibility.class)).thenReturn(false);
    bossMovement.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(new Vector2(3f, 0f));
  }

  @Test
  void shouldNotBeginFleeingFromInvisiblePlayer() {
    when(abilities.isActive(Invisibility.class)).thenReturn(true);
    bossMovement.setMode(FinalBossMovementComponent.Mode.FLEE_PLAYER);
    bossMovement.update();
    verify(movement).setMoving(false);
    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }

  @Test
  void shouldContinueWanderingWhilePlayerIsInvisible() {
    when(abilities.isActive(Invisibility.class)).thenReturn(true);
    bossMovement.setMode(FinalBossMovementComponent.Mode.WANDER_AVOID_SUMMONS);
    bossMovement.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(any(Vector2.class));
    verify(movement, never()).setMoving(false);
  }

  @Test
  void shouldStillAvoidSummonsWhilePlayerIsInvisible() {
    when(abilities.isActive(Invisibility.class)).thenReturn(true);
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
    when(abilities.isActive(Invisibility.class)).thenReturn(true);
    bossMovement.update();
    when(abilities.isActive(Invisibility.class)).thenReturn(false);
    bossMovement.update();
    verify(movement, times(2)).setMoving(false);
    verify(movement, never()).setMoving(true);
    verify(movement, never()).setTarget(any());
  }
}
