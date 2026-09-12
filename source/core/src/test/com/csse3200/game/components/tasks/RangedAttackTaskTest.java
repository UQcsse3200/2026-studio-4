package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class RangedAttackTaskTest {
  private PlayerAbilitiesComponent abilities;
  private Entity player;
  private RangedAttackTask task;
  private GameTime time;
  private final List<Runnable> callbacks = new ArrayList<>();
  private final List<Entity> spawned = new ArrayList<>();
  private int attacks;

  @BeforeEach
  void setUp() {
    abilities = mock(PlayerAbilitiesComponent.class);
    player = new Entity().addComponent(abilities);
    player.setPosition(5f, 0f);
    Entity owner = new Entity().addComponent(mock(PhysicsMovementComponent.class));
    owner.getEvents().addListener("rangedAttack", () -> attacks++);
    task = new RangedAttackTask(player, 7, spawned::add);
    task.create(() -> owner);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    EntityService entities = mock(EntityService.class);
    doAnswer(
            invocation -> {
              callbacks.add(invocation.getArgument(0));
              return null;
            })
        .when(entities)
        .runAfterUpdate(any(Runnable.class));
    ServiceLocator.registerEntityService(entities);
  }

  @Test
  void shouldSuppressInactiveAndActivePriorityButRetainRangeHysteresis() {
    when(abilities.isInvisible()).thenReturn(true);
    assertEquals(-1, task.getPriority());
    task.start();
    assertEquals(-1, task.getPriority());
    player.setPosition(7f, 0f);
    when(abilities.isInvisible()).thenReturn(false);
    assertEquals(5, task.getPriority());
    task.stop();
    assertEquals(-1, task.getPriority());
    player.setPosition(6f, 0f);
    assertEquals(5, task.getPriority());
  }

  @Test
  void shouldNotQueueOrAnnounceAttacksWhileInvisibleAndResumeWhenVisible() {
    task.start();
    when(abilities.isInvisible()).thenReturn(true);
    when(time.getDeltaTime()).thenReturn(10f);
    task.update();
    task.update();
    assertEquals(0, callbacks.size());
    assertEquals(0, attacks);
    when(abilities.isInvisible()).thenReturn(false);
    task.update();
    assertEquals(3, callbacks.size());
    assertEquals(1, attacks);
  }

  @Test
  void shouldPauseExistingCooldownWhileInvisible() {
    task.start();
    task.update();
    when(abilities.isInvisible()).thenReturn(true);
    when(time.getDeltaTime()).thenReturn(10f);
    task.update();
    when(abilities.isInvisible()).thenReturn(false);
    when(time.getDeltaTime()).thenReturn(1f);
    task.update();
    assertEquals(3, callbacks.size());
    task.update();
    assertEquals(6, callbacks.size());
    assertEquals(2, attacks);
  }

  @Test
  void shouldRecheckInvisibilityForEveryDeferredProjectile() {
    try (MockedStatic<FloatingDemonProjectileFactory> factory =
        mockStatic(FloatingDemonProjectileFactory.class)) {
      Entity projectile = new Entity();
      factory
          .when(() -> FloatingDemonProjectileFactory.createProjectile(any(), any(), eq(7)))
          .thenReturn(projectile);
      task.start();
      task.update();
      assertEquals(3, callbacks.size());
      callbacks.get(0).run();
      when(abilities.isInvisible()).thenReturn(true);
      callbacks.get(1).run();
      callbacks.get(2).run();
      assertEquals(List.of(projectile), spawned);
      factory.verify(() -> FloatingDemonProjectileFactory.createProjectile(any(), any(), eq(7)));

      when(abilities.isInvisible()).thenReturn(false);
      when(time.getDeltaTime()).thenReturn(2f);
      task.update();
      callbacks.subList(3, 6).forEach(Runnable::run);
      assertEquals(4, spawned.size());
      factory.verify(
          () -> FloatingDemonProjectileFactory.createProjectile(any(), any(), eq(7)), times(4));
    }
  }

  @Test
  void shouldDiscardEntireQueuedVolleyWhenPlayerBecomesInvisible() {
    try (MockedStatic<FloatingDemonProjectileFactory> factory =
        mockStatic(FloatingDemonProjectileFactory.class)) {
      task.start();
      task.update();
      assertEquals(3, callbacks.size());
      when(abilities.isInvisible()).thenReturn(true);
      callbacks.forEach(Runnable::run);
      factory.verifyNoInteractions();
      assertEquals(0, spawned.size());
    }
  }

  @Test
  void shouldAttackOnlyWhenPlayerIsClose() {
    Entity rangedTarget = new Entity();
    RangedAttackTask attackTask = new RangedAttackTask(rangedTarget, 7);
    AITaskComponent ai = new AITaskComponent().addTask(attackTask);
    Entity demon = new Entity().addComponent(ai);

    demon.setPosition(0f, 0f);
    rangedTarget.setPosition(5f, 0f);
    assertEquals(5, attackTask.getPriority());

    rangedTarget.setPosition(8f, 0f);
    assertEquals(-1, attackTask.getPriority());
  }
}
