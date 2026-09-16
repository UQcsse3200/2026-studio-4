package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.rendering.RenderService;
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

class CerberusAttackCoordinationTest {
  private GameTime time;
  private Entity player;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent leftStats;
  private CombatStatsComponent middleStats;

  private CerberusMistComponent mist;
  private CerberusBiteComponent bite;
  private CerberusProjectileComponent shooter;
  private CerberusPhaseComponent phase;

  private int windups;
  private int lunges;
  private int mistEntries;

  private final Queue<Runnable> pending = new ArrayDeque<>();
  private final List<Entity> spawned = new ArrayList<>();
  private MockedStatic<FloatingDemonProjectileFactory> projectileFactory;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));

    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    doAnswer(
            invocation -> {
              pending.add(invocation.getArgument(0, Runnable.class));
              return null;
            })
        .when(entityService)
        .schedule(any(Runnable.class));

    playerStats = new CombatStatsComponent(1000, 0);
    player = new Entity().addComponent(playerStats);
    player.setPosition(1f, 0f);
    player.getEvents().addListener(CerberusMistComponent.ENTERED, (Entity source) -> mistEntries++);

    leftStats = new CombatStatsComponent(125, 10);
    middleStats = new CombatStatsComponent(250, 20);

    mist = new CerberusMistComponent(player);
    bite = new CerberusBiteComponent(player, new Vector2(), 3f);
    shooter = new CerberusProjectileComponent(player, spawned::add);

    Entity left = new Entity().addComponent(leftStats).addComponent(mist);
    Entity right =
        new Entity().addComponent(new CombatStatsComponent(125, 10)).addComponent(shooter);

    phase = new CerberusPhaseComponent(left, right);
    Entity middle = new Entity().addComponent(middleStats).addComponent(phase).addComponent(bite);

    middle.getEvents().addListener("biteWindup", () -> windups++);
    middle.getEvents().addListener("attackStart", () -> lunges++);

    CerberusAttackCoordinator coordinator =
        new CerberusAttackCoordinator(left, middle, right, phase);

    mist.setAttackCoordinator(coordinator);
    bite.setAttackCoordinator(coordinator);
    shooter.setAttackCoordinator(coordinator);

    left.create();
    right.create();
    middle.create();

    projectileFactory = mockStatic(FloatingDemonProjectileFactory.class);
    projectileFactory
        .when(
            () ->
                FloatingDemonProjectileFactory.createHomingProjectile(
                    any(Vector2.class), same(player), eq(10)))
        .thenAnswer(invocation -> new Entity());
  }

  @AfterEach
  void tearDown() {
    try {
      if (projectileFactory != null) {
        projectileFactory.close();
      }
    } finally {
      ServiceLocator.clear();
    }
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    mist.update();
    bite.update();
    shooter.update();
  }

  private void runPending() {
    while (!pending.isEmpty()) {
      pending.remove().run();
    }
  }

  @Test
  void shouldReleaseMistThenBiteThenProjectileInPhaseOne() {
    tick(5f);

    assertEquals(1, phase.getCurrentPhase());
    assertTrue(mist.isMistActive());
    assertEquals(0, windups);
    assertEquals(0, pending.size());

    tick(2f);

    assertTrue(mist.isMistActive());
    assertEquals(0, windups);
    assertEquals(0, pending.size());
    assertEquals(1000, playerStats.getHealth());

    tick(1f);

    assertFalse(mist.isMistActive());
    assertEquals(1, windups);
    assertEquals(0, pending.size());

    tick(0.5f);

    assertEquals(1, lunges);
    assertEquals(0, pending.size());

    tick(0.35f);

    assertEquals(980, playerStats.getHealth());
    assertEquals(1, pending.size());
    assertEquals(0, spawned.size());

    runPending();

    assertEquals(1, spawned.size());
  }

  @Test
  void shouldAllowBiteAndProjectileDuringExistingMistInPhaseTwo() {
    tick(5f);

    assertTrue(mist.isMistActive());
    assertEquals(0, windups);
    assertEquals(0, pending.size());

    middleStats.setHealth(124);
    leftStats.setHealth(1);

    assertEquals(2, phase.getCurrentPhase());

    tick(0f);
    runPending();

    assertTrue(mist.isMistActive());
    assertEquals(1, mistEntries);
    assertEquals(1, windups);
    assertEquals(1, spawned.size());

    tick(0.5f);
    tick(0.1f);
    runPending();

    assertTrue(mist.isMistActive());
    assertEquals(1, lunges);
    assertEquals(980, playerStats.getHealth());

    assertEquals(1, mistEntries);
    assertEquals(1, windups);
    assertEquals(1, spawned.size());
  }

  @Test
  void shouldLetOtherSkillsContinueWhenLeftHeadDiesDuringMist() {
    tick(5f);

    assertTrue(mist.isMistActive());

    leftStats.setHealth(0);

    assertFalse(mist.isMistActive());
    assertEquals(1, phase.getCurrentPhase());

    tick(0f);

    assertEquals(1, windups);
    assertEquals(0, pending.size());

    tick(0.5f);
    tick(0.35f);
    runPending();

    assertEquals(1, lunges);
    assertEquals(1, spawned.size());
    assertFalse(mist.isMistActive());
  }

  @Test
  void shouldFireWhenMistIsCoolingDownAndBiteIsOutOfRange() {
    player.setPosition(5f, 0f);

    tick(2.5f);
    runPending();

    assertEquals(1, phase.getCurrentPhase());
    assertFalse(mist.isMistActive());
    assertEquals(0, windups);
    assertEquals(1, spawned.size());
  }
}
