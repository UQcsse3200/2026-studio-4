package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;

class SlowTest {
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private Entity entity;

  @BeforeEach
  void setUp() {
    stats = new CombatStatsComponent(100, 10);
    stats.setMovementSpeed(3f);
    effects = new StatusEffectsControllerComponent();
    entity = new Entity().addComponent(stats).addComponent(effects);
    entity.create();
  }

  private Slow apply(float amount) {
    Slow slow = new Slow(60_000, stats, amount);
    effects.addStatusEffect(slow);
    return slow;
  }

  @ParameterizedTest
  @ValueSource(floats = {0f, 0.5f, 1.5f, 3f})
  void earlyRemovalRestoresOnlyTheSpeedActuallyTaken(float originalSpeed) {
    stats.setMovementSpeed(originalSpeed);
    Slow slow = apply(-1.5f);

    effects.removeStatusEffect(slow);

    assertEquals(originalSpeed, stats.getMovementSpeed());
    assertFalse(effects.hasStatusEffect(slow));
  }

  @Test
  void removalPreservesSpeedChangesMadeWhileSlowed() {
    Slow slow = apply(-1.5f);
    stats.addMovementSpeed(2f);

    effects.removeStatusEffect(slow);

    assertEquals(5f, stats.getMovementSpeed());
  }

  @Test
  void removingOneSlowLeavesTheOtherPenaltyInPlace() {
    Slow first = apply(-0.5f);
    Slow second = apply(-0.75f);
    assertEquals(1.75f, stats.getMovementSpeed());

    effects.removeStatusEffect(first);
    assertEquals(2.25f, stats.getMovementSpeed());

    effects.removeStatusEffect(second);
    assertEquals(3f, stats.getMovementSpeed());
  }

  @Test
  void repeatedRemovalNotifiesOnceAfterRestoringMovement() {
    Slow slow = apply(-1.5f);
    List<Float> endedSpeeds = new ArrayList<>();
    List<Float> movementUpdates = new ArrayList<>();
    slow.setOnEnded(() -> endedSpeeds.add(stats.getMovementSpeed()));
    entity
        .getEvents()
        .addListener("updateMovementSpeed", (Float speed) -> movementUpdates.add(speed));

    effects.removeStatusEffect(slow);
    effects.removeStatusEffect(slow);
    slow.onRemoved();

    assertEquals(List.of(3f), endedSpeeds);
    assertEquals(List.of(3f), movementUpdates);
    assertEquals(3f, stats.getMovementSpeed());
  }

  @Test
  void expiryAndSubsequentCleanupRestoreClampedSpeedOnce() {
    long[] now = {0L};
    try (MockedConstruction<GameTime> clocks =
        mockConstruction(
            GameTime.class,
            (clock, context) -> {
              when(clock.getTime()).thenAnswer(ignored -> now[0]);
              when(clock.getTimeSince(anyLong()))
                  .thenAnswer(call -> now[0] - (long) call.getArgument(0));
            })) {
      stats.setMovementSpeed(0.5f);
      Slow slow = apply(-1.5f);
      assertEquals(0f, stats.getMovementSpeed());

      now[0] = 60_001L;
      effects.update();
      assertEquals(0.5f, stats.getMovementSpeed());
      assertFalse(effects.hasStatusEffect(slow));

      slow.update();
      slow.onRemoved();
      effects.update();
      assertEquals(0.5f, stats.getMovementSpeed());
    }
  }

  @Test
  void deathAndDisposalReleaseActiveSlowStacks() {
    apply(-0.5f);
    apply(-1.5f);
    assertEquals(1f, stats.getMovementSpeed());

    stats.setHealth(0);
    assertEquals(3f, stats.getMovementSpeed());

    effects.dispose();
    assertEquals(3f, stats.getMovementSpeed());
  }

  @Test
  void disposingControllerRestoresActiveSlow() {
    apply(-1.5f);

    effects.dispose();

    assertEquals(3f, stats.getMovementSpeed());
  }
}
