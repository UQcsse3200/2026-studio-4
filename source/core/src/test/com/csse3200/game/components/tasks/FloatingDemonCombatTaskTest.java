package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
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
class FloatingDemonCombatTaskTest {
  private Entity player;
  private Entity demon;
  private PhysicsMovementComponent movement;
  private GameTime time;
  private FloatingDemonCombatTask task;
  private final List<Runnable> shots = new ArrayList<>();

  @BeforeEach
  void setup() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    EntityService entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    doAnswer(
            call -> {
              shots.add(call.getArgument(0));
              return null;
            })
        .when(entities)
        .runAfterUpdate(any(Runnable.class));
    player = new Entity();
    movement = mock(PhysicsMovementComponent.class);
    demon = new Entity().addComponent(movement).addComponent(new CombatStatsComponent(10, 7));
    task = makeTask(0);
  }

  private FloatingDemonCombatTask makeTask(int index) {
    FloatingDemonCombatTask result =
        new FloatingDemonCombatTask(player, shot -> {}, 9f, 12f, index);
    result.create(() -> demon);
    result.start();
    return result;
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    task.update();
  }

  private Vector2 destination() {
    ArgumentCaptor<Vector2> position = ArgumentCaptor.forClass(Vector2.class);
    verify(movement, atLeastOnce()).setTarget(position.capture());
    return position.getValue();
  }

  @Test
  void followsFarPlayerAndCirclesAtMediumDistance() {
    player.setPosition(8f, 0f);
    tick(0.1f);
    assertTrue(destination().x > 0f);
    assertEquals(0f, destination().y, 0.001f);
    player.setPosition(4f, 0f);
    tick(0.1f);
    assertEquals(0f, destination().x, 0.001f);
    assertTrue(destination().y > 0f);
  }

  @Test
  void closePlayerOnlyCausesAShortRetreat() {
    player.setPosition(2f, 0f);
    tick(0.6f);
    assertTrue(destination().x < 0f);
    tick(0.1f);
    assertEquals(0f, destination().x, 0.001f);
    assertTrue(destination().y > 0f);
  }

  @Test
  void warningLocksAimBeforePlayerDodgesAndCooldownAllowsMovement() {
    player.setPosition(5f, 0f);
    tick(1f);
    verify(movement).setMoving(false);
    assertTrue(shots.isEmpty());
    player.setPosition(5f, 3f);
    tick(0.2f);
    assertTrue(shots.isEmpty());
    tick(0.21f);
    assertEquals(3, shots.size());
    try (var factory = mockStatic(FloatingDemonProjectileFactory.class)) {
      shots.get(1).run();
      factory.verify(
          () ->
              FloatingDemonProjectileFactory.createProjectile(
                  new Vector2(0.5f, 0.5f), new Vector2(1f, 0f), 7));
    }
    clearInvocations(movement);
    tick(0.1f);
    verify(movement).setMoving(true);
    assertEquals(3, shots.size());
  }

  @Test
  void secondDemonDoesNotFireWithTheFirstOne() {
    player.setPosition(5f, 0f);
    FloatingDemonCombatTask second = makeTask(1);
    tick(1f);
    second.update();
    tick(0.4f);
    second.update();
    assertEquals(3, shots.size());
  }

  @Test
  void losesDistantOrConcealedPlayerAndCancelsWarning() {
    player.setPosition(5f, 0f);
    tick(1f);
    var effects = mock(StatusEffectsControllerComponent.class);
    player.addComponent(effects);
    when(effects.isConcealed()).thenReturn(true);
    assertEquals(-1, task.getPriority());
    tick(0.5f);
    assertTrue(shots.isEmpty());
    when(effects.isConcealed()).thenReturn(false);
    player.setPosition(13f, 0f);
    assertEquals(-1, task.getPriority());
  }

  @Test
  void dyingBeforeQueuedShotRunsPreventsProjectileCreation() {
    player.setPosition(5f, 0f);
    tick(1f);
    tick(0.4f);
    demon.getComponent(CombatStatsComponent.class).setHealth(0);
    try (var factory = mockStatic(FloatingDemonProjectileFactory.class)) {
      for (Runnable shot : shots) shot.run();
      factory.verifyNoInteractions();
    }
  }
}
