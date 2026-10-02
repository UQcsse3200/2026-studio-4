package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
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

/** Stage 2 timing, slow movement and the health handover to Stage 3. */
@ExtendWith(GameExtension.class)
class FinalBossStageTwoComponentTest {
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossMovementComponent sharedMovement;
  private PhysicsMovementComponent physicsMovement;
  private FinalBossStageTwoArenaComponent arena;
  private CombatStatsComponent stats;
  private Entity boss;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    config = new FinalBossStageTwoConfig();
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    sharedMovement = mock(FinalBossMovementComponent.class);
    physicsMovement = mock(PhysicsMovementComponent.class);
    arena = mock(FinalBossStageTwoArenaComponent.class);
    stats = new CombatStatsComponent(1000, 0);
    stageTwo = new FinalBossStageTwoComponent(config);
    boss =
        new Entity()
            .addComponent(stats)
            .addComponent(phases)
            .addComponent(sharedMovement)
            .addComponent(physicsMovement)
            .addComponent(arena)
            .addComponent(stageTwo);
    boss.setPosition(8f, 4f);
    when(arena.getMovementBounds(boss)).thenReturn(new Rectangle(0f, 0f, 16f, 8f));
    boss.create();
    stats.setHealth(800);
  }

  @Test
  void waitsForTheEncounterToStartBeforeMoving() {
    advance(30f);

    verify(physicsMovement, never()).setMoving(true);
    verify(physicsMovement, never()).setTarget(any());
  }

  @Test
  void transitionTimeDoesNotConsumeTheFirstAttackCycle() {
    stageTwo.startEncounter();
    when(phases.isTransitioning()).thenReturn(true);
    advance(3f);
    verify(physicsMovement, never()).setMoving(true);

    when(phases.isTransitioning()).thenReturn(false);
    advance(config.attackDuration - 0.25f);
    assertTrue(stageTwo.isAttacking());
    advance(0.25f);
    assertFalse(stageTwo.isAttacking());
  }

  @Test
  void pausesForThreeSecondsThenStartsAnotherAttackCycle() {
    stageTwo.startEncounter();
    advance(config.attackDuration);
    assertFalse(stageTwo.isAttacking());

    advance(2.75f);
    assertFalse(stageTwo.isAttacking());
    advance(0.25f);
    assertTrue(stageTwo.isAttacking());
  }

  @Test
  void keepsElapsedRemaindersWhenAFrameCrossesCycleBoundaries() {
    stageTwo.startEncounter();
    advance(config.attackDuration + 1.25f);
    assertFalse(stageTwo.isAttacking());
    advance(1.5f);
    assertFalse(stageTwo.isAttacking());
    advance(0.25f);
    assertTrue(stageTwo.isAttacking());

    float wholeCycle = config.attackDuration + config.pauseDuration;
    advance(wholeCycle * 2f + config.attackDuration + 0.5f);
    assertFalse(stageTwo.isAttacking());
    advance(config.pauseDuration - 0.5f);
    assertTrue(stageTwo.isAttacking());
  }

  @Test
  void continuesRoamingDuringThePauseWithoutEnablingCharges() {
    stageTwo.startEncounter();
    advance(config.attackDuration);
    assertFalse(stageTwo.isAttacking());
    clearInvocations(physicsMovement);

    advance(0.1f);

    verify(physicsMovement).setMoving(true);
    verify(physicsMovement).setTarget(any(Vector2.class));
    verify(sharedMovement, never()).enableChargeAttacks();
  }

  @Test
  void choosesAnArenaDestinationAndUsesTheConfiguredSlowSpeed() {
    stageTwo.startEncounter();
    advance(0.1f);

    ArgumentCaptor<Vector2> target = ArgumentCaptor.forClass(Vector2.class);
    ArgumentCaptor<Vector2> speed = ArgumentCaptor.forClass(Vector2.class);
    verify(physicsMovement).setTarget(target.capture());
    verify(physicsMovement).setMaxSpeed(speed.capture());
    assertTrue(new Rectangle(0f, 0f, 16f, 8f).contains(target.getValue()));
    assertTrue(speed.getValue().x > 0f && speed.getValue().x <= config.bossMoveSpeed);
    assertTrue(speed.getValue().y > 0f && speed.getValue().y <= config.bossMoveSpeed);
  }

  @Test
  void stopsWhenNoArenaBoundsAreAvailable() {
    stageTwo.startEncounter();
    when(arena.getMovementBounds(boss)).thenReturn(null);
    clearInvocations(physicsMovement);

    advance(0.1f);

    verify(physicsMovement).setMoving(false);
    verify(physicsMovement, never()).setMoving(true);
  }

  @Test
  void stopsRoamingWhenTheBossDies() {
    stageTwo.startEncounter();
    advance(0.1f);
    clearInvocations(physicsMovement);
    stats.setHealth(0);

    advance(0.1f);

    verify(physicsMovement).setMoving(false);
    verify(physicsMovement, never()).setMoving(true);
  }

  @Test
  void aNewEncounterResetsTheCycleAndRestoresItsHealthFloor() {
    stageTwo.startEncounter();
    advance(config.attackDuration + 0.5f);
    assertFalse(stageTwo.isAttacking());
    stats.setMinimumHealth(0);

    stageTwo.startEncounter();

    assertTrue(stageTwo.isAttacking());
    assertEquals(600, stats.getMinimumHealth());
    advance(config.attackDuration - 0.25f);
    assertTrue(stageTwo.isAttacking());
  }

  @Test
  void oversizedDamageStopsAtSixtyPercentAndCompletesTheStageOnlyOnce() {
    List<FinalBossPhase> completed = new ArrayList<>();
    boss.getEvents()
        .addListener(
            FinalBossEvents.STAGE_COMPLETED, (FinalBossPhase phase) -> completed.add(phase));
    stageTwo.startEncounter();

    stats.takeDamage(10000);
    stats.takeDamage(10000);

    assertEquals(600, stats.getHealth());
    assertEquals(List.of(FinalBossPhase.STAGE_TWO), completed);
    assertFalse(stats.isDead());
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageTwo.update();
  }
}
