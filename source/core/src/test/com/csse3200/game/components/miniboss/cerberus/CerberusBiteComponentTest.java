package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerberusBiteComponentTest {
  private Entity boss;
  private Entity player;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private CerberusBiteComponent bite;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    playerStats = new CombatStatsComponent(100, 0);
    player = new Entity().addComponent(playerStats);
    player.setPosition(1f, 0f);

    bossStats = new CombatStatsComponent(100, 20);
    bite = new CerberusBiteComponent(player, new Vector2(), 3f);
    boss = new Entity().addComponent(bossStats).addComponent(bite);
    boss.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    bite.update();
  }

  @Test
  void shouldWaitBeforeDealingDamageAndHitOnlyOnce() {
    tick(0f);
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());

    tick(0.25f);
    tick(0.1f);
    assertEquals(80, playerStats.getHealth());

    tick(0.1f);
    tick(0.2f);
    assertEquals(80, playerStats.getHealth());
  }

  @Test
  void shouldMissWhenPlayerDodgesDuringWindup() {
    tick(0f);
    player.setPosition(10f, 0f);

    tick(0.5f);
    tick(0.35f);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void shouldCancelWhenMiddleHeadDiesDuringWindup() {
    tick(0f);
    bossStats.setHealth(0);

    tick(0.5f);
    tick(0.35f);

    assertEquals(100, playerStats.getHealth());

    PhysicsMovementComponent movement = mock(PhysicsMovementComponent.class);
    assertTrue(bite.controlMovement(movement));
    verify(movement).setMoving(false);
  }

  @Test
  void shouldNotAttackAgainDuringCooldown() {
    tick(0f);
    tick(0.5f);
    tick(0.35f);
    assertEquals(80, playerStats.getHealth());

    tick(1f);
    tick(0.5f);
    assertEquals(80, playerStats.getHealth());
  }

  @Test
  void shouldStopForWindupThenLungeAtLockedDestination() {
    tick(0f);

    PhysicsMovementComponent movement = mock(PhysicsMovementComponent.class);
    assertTrue(bite.controlMovement(movement));
    verify(movement).setMoving(false);

    player.setPosition(2f, 0f);
    tick(0.5f);

    clearInvocations(movement);
    assertTrue(bite.controlMovement(movement));
    verify(movement).setTarget(new Vector2(1f, 0f));
    verify(movement).setMaxSpeed(new Vector2(6f, 6f));
    verify(movement).setMoving(true);
  }

  @Test
  void shouldWaitForCoordinatorBeforeStartingWindup() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    bite.setAttackCoordinator(coordinator);

    int[] windups = {0};
    boss.getEvents().addListener("biteWindup", () -> windups[0]++);

    when(coordinator.tryStart(boss)).thenReturn(false);

    tick(0f);
    tick(0.5f);
    tick(0.35f);

    assertEquals(0, windups[0]);
    assertEquals(100, playerStats.getHealth());

    when(coordinator.tryStart(boss)).thenReturn(true);

    tick(0f);

    assertEquals(1, windups[0]);

    tick(0.5f);
    tick(0.35f);

    assertEquals(80, playerStats.getHealth());
  }

  @Test
  void shouldReleaseAttackAfterLungeBeforeCooldownEnds() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    bite.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(boss)).thenReturn(true);

    tick(0f);
    tick(0.5f);

    verify(coordinator, never()).finish(boss);

    tick(0.35f);

    verify(coordinator).finish(boss);
    assertEquals(80, playerStats.getHealth());
    tick(1f);

    verify(coordinator, times(1)).tryStart(boss);
  }

  @Test
  void shouldReleaseAttackWhenHeadDiesDuringWindup() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    bite.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(boss)).thenReturn(true);

    tick(0f);

    verify(coordinator, never()).finish(boss);

    bossStats.setHealth(0);
    verify(coordinator).finish(boss);

    tick(0.5f);
    tick(0.35f);

    assertEquals(100, playerStats.getHealth());
    verify(coordinator, times(1)).tryStart(boss);
  }

  @Test
  void shouldReleaseAttackWhenDisposedDuringWindup() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    bite.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(boss)).thenReturn(true);

    tick(0f);

    verify(coordinator, never()).finish(boss);

    bite.dispose();

    verify(coordinator).finish(boss);
    assertEquals(100, playerStats.getHealth());
  }
}
