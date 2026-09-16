package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CerberusProjectileComponentTest {
  private GameTime time;
  private Entity player;
  private Entity rightHead;
  private CombatStatsComponent rightStats;
  private CerberusProjectileComponent shooter;
  private Entity projectile;

  private final Queue<Runnable> pending = new ArrayDeque<>();
  private final List<Entity> spawned = new ArrayList<>();

  private MockedStatic<FloatingDemonProjectileFactory> projectileFactory;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    doAnswer(
            invocation -> {
              pending.add(invocation.getArgument(0, Runnable.class));
              return null;
            })
        .when(entityService)
        .schedule(any(Runnable.class));

    player = new Entity().addComponent(new CombatStatsComponent(100, 0));
    player.setPosition(1f, 0f);

    rightStats = new CombatStatsComponent(100, 10);
    shooter = new CerberusProjectileComponent(player, spawned::add);
    rightHead = new Entity().addComponent(rightStats).addComponent(shooter);
    rightHead.create();

    projectile = new Entity();
    projectileFactory = mockStatic(FloatingDemonProjectileFactory.class);
    projectileFactory
        .when(
            () ->
                FloatingDemonProjectileFactory.createHomingProjectile(
                    any(Vector2.class), same(player), eq(10)))
        .thenReturn(projectile);
  }

  @AfterEach
  void tearDown() {
    if (projectileFactory != null) {
      projectileFactory.close();
    }
    ServiceLocator.clear();
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    shooter.update();
  }

  private void runPending() {
    while (!pending.isEmpty()) {
      pending.remove().run();
    }
  }

  @Test
  void shouldFireOnceAfterCooldownAndDeferCreation() {
    tick(2f);
    assertEquals(0, pending.size());
    assertEquals(0, spawned.size());

    tick(0.5f);
    assertEquals(1, pending.size());
    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();

    runPending();

    assertEquals(List.of(projectile), spawned);
    projectileFactory.verify(
        () ->
            FloatingDemonProjectileFactory.createHomingProjectile(
                any(Vector2.class), same(player), eq(10)),
        times(1));

    tick(1f);
    runPending();
    assertEquals(1, spawned.size());

    tick(1.5f);
    runPending();
    assertEquals(2, spawned.size());
  }

  @Test
  void shouldWaitUntilPlayerReturnsToRange() {
    player.setPosition(20f, 0f);

    tick(3f);
    runPending();

    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();

    player.setPosition(1f, 0f);
    tick(0f);
    runPending();

    assertEquals(1, spawned.size());
  }

  @Test
  void shouldNotFireWhenRightHeadIsDead() {
    rightStats.setHealth(0);

    tick(3f);
    runPending();

    assertEquals(0, pending.size());
    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();
  }

  @Test
  void shouldCancelQueuedShotWhenRightHeadDies() {
    tick(2.5f);
    assertEquals(1, pending.size());

    rightStats.setHealth(0);
    runPending();

    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();
  }

  @Test
  void shouldCancelQueuedShotWhenComponentIsDisposed() {
    tick(2.5f);
    assertEquals(1, pending.size());

    shooter.dispose();
    runPending();

    tick(3f);
    runPending();

    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();
  }

  @Test
  void shouldCancelQueuedShotWhenPlayerDies() {
    tick(2.5f);

    player.getComponent(CombatStatsComponent.class).setHealth(0);
    runPending();

    assertEquals(0, spawned.size());
    projectileFactory.verifyNoInteractions();
  }

  @Test
  void shouldKeepFiringAfterMiddleHeadDies() {
    Entity leftHead = new Entity().addComponent(new CombatStatsComponent(100, 10));

    CombatStatsComponent middleStats = new CombatStatsComponent(100, 20);
    Entity middleHead =
        new Entity()
            .addComponent(middleStats)
            .addComponent(new CerberusDeathComponent(leftHead, rightHead));
    middleHead.create();

    middleStats.setHealth(0);
    runPending();

    tick(2.5f);
    runPending();

    assertEquals(1, spawned.size());
    assertEquals(100, rightStats.getHealth());
  }
}
