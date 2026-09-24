package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ThunderOrbFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DragonThunderOrbComponentTest {
  private final List<Runnable> queuedTasks = new ArrayList<>();
  private final List<Entity> spawnedOrbs = new ArrayList<>();

  private Entity dragon;
  private Entity target;
  private CombatStatsComponent dragonStats;
  private CombatStatsComponent targetStats;
  private DragonThunderOrbComponent attack;

  @BeforeEach
  void setUp() {
    EntityService entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    lenient()
        .doAnswer(
            invocation -> {
              queuedTasks.add(invocation.getArgument(0));
              return null;
            })
        .when(entities)
        .schedule(any(Runnable.class));

    dragonStats = new CombatStatsComponent(500, 20);
    targetStats = new CombatStatsComponent(100, 10);

    target = new Entity().addComponent(targetStats);
    attack = new DragonThunderOrbComponent(target, spawnedOrbs::add);
    dragon = new Entity().addComponent(dragonStats).addComponent(attack);

    attack.create();
  }

  private void runQueuedTasks() {
    List<Runnable> tasks = new ArrayList<>(queuedTasks);
    queuedTasks.clear();
    tasks.forEach(Runnable::run);
  }

  @Test
  void shouldDeferCreationAndSpawnAtDragonCentre() {
    Entity orb = new Entity();

    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      factory
          .when(() -> ThunderOrbFactory.createThunderOrb(any(Vector2.class), eq(target)))
          .thenReturn(orb);

      assertTrue(attack.tryAttack());
      assertFalse(attack.tryAttack());
      assertTrue(spawnedOrbs.isEmpty());
      factory.verifyNoInteractions();

      runQueuedTasks();

      assertTrue(spawnedOrbs.contains(orb));
      factory.verify(
          () -> ThunderOrbFactory.createThunderOrb(dragon.getCenterPosition(), target), times(1));
    }
  }

  @Test
  void shouldRequireCooldownBeforeAnotherShot() {
    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      factory
          .when(() -> ThunderOrbFactory.createThunderOrb(any(Vector2.class), eq(target)))
          .thenAnswer(invocation -> new Entity());

      assertTrue(attack.tryAttack());
      runQueuedTasks();

      attack.update(2f);
      assertFalse(attack.tryAttack());

      attack.update(0.5f);
      assertTrue(attack.tryAttack());
    }
  }

  @Test
  void shouldCancelQueuedShotWhenDragonDies() {
    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      assertTrue(attack.tryAttack());

      dragonStats.setHealth(0);
      runQueuedTasks();

      factory.verifyNoInteractions();
      assertTrue(spawnedOrbs.isEmpty());

      attack.update(10f);
      assertFalse(attack.tryAttack());
    }
  }

  @Test
  void shouldCancelQueuedShotWhenTargetDies() {
    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      assertTrue(attack.tryAttack());

      targetStats.setHealth(0);
      runQueuedTasks();

      factory.verifyNoInteractions();
      assertTrue(spawnedOrbs.isEmpty());
    }
  }

  @Test
  void shouldCancelOwnedOrbOnlyOnceOnDeathAndDisposal() {
    ThunderOrbHitComponent hit = mock(ThunderOrbHitComponent.class);
    Entity orb = new Entity().addComponent(hit);

    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      factory
          .when(() -> ThunderOrbFactory.createThunderOrb(any(Vector2.class), eq(target)))
          .thenReturn(orb);

      assertTrue(attack.tryAttack());
      runQueuedTasks();

      dragonStats.setHealth(0);
      attack.dispose();

      verify(hit, times(1)).cancel();
    }
  }

  @Test
  void shouldForgetOrbAfterItIsDisposed() {
    ThunderOrbHitComponent hit = mock(ThunderOrbHitComponent.class);
    Entity orb = new Entity().addComponent(hit);

    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      factory
          .when(() -> ThunderOrbFactory.createThunderOrb(any(Vector2.class), eq(target)))
          .thenReturn(orb);

      assertTrue(attack.tryAttack());
      runQueuedTasks();

      orb.create();
      orb.dispose();

      attack.stop();

      verify(hit, never()).cancel();
    }
  }

  @Test
  void shouldCancelQueuedShotWhenComponentIsDisposed() {
    try (MockedStatic<ThunderOrbFactory> factory = mockStatic(ThunderOrbFactory.class)) {
      assertTrue(attack.tryAttack());

      attack.dispose();
      runQueuedTasks();

      factory.verifyNoInteractions();
      assertFalse(attack.tryAttack());
    }
  }

  @Test
  void shouldNotFireAutomaticallyWhenCooldownElapses() {
    attack.update(10f);

    assertTrue(queuedTasks.isEmpty());
    assertTrue(spawnedOrbs.isEmpty());
  }
}
