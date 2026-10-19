package com.csse3200.game.components.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DodgeCannonComponentTest {
  private static final float STEP_SECONDS = 0.1f;
  private static final float RANGE_TILES = 12f;
  private final List<Boolean> results = new ArrayList<>();
  private final List<Entity> orbs = new ArrayList<>();
  private Entity cannon;
  private Entity player;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(STEP_SECONDS);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerEntityService(new EntityService());
    player = new Entity();
    player.setScale(1f, 1f);
  }

  /** A cannon at the origin, firing straight down, with orbs that need no art. */
  private void buildCannon(float interval, float delay) {
    cannon =
        new Entity()
            .addComponent(
                new DodgeCannonComponent(
                    player,
                    interval,
                    delay,
                    RANGE_TILES,
                    () -> {
                      Entity orb = new Entity();
                      orb.setScale(0.4f, 0.4f);
                      orbs.add(orb);
                      return orb;
                    }));
    cannon.setScale(1f, 1f);
    cannon.setPosition(0f, 5f);
    cannon.create();
    cannon.getEvents().<Boolean>addListener(DodgeCannonComponent.RESULT, results::add);
  }

  private void run(int steps) {
    for (int i = 0; i < steps; i++) {
      cannon.update();
    }
  }

  @Test
  void shouldFireImmediatelyWhenThereIsNoDelay() {
    buildCannon(2.4f, 0f);

    run(1);

    assertEquals(1, orbs.size());
  }

  @Test
  void shouldWaitOutItsDelayBeforeTheFirstShot() {
    buildCannon(2.4f, 0.5f);

    run(4);
    assertTrue(orbs.isEmpty());

    run(2);
    assertEquals(1, orbs.size());
  }

  @Test
  void shouldFireAgainEveryInterval() {
    buildCannon(1f, 0f);

    run(25);

    assertEquals(3, orbs.size());
  }

  @Test
  void shouldSpawnOrbsAtTheCannonsMouthAndMoveThemDown() {
    buildCannon(100f, 0f);

    run(1);
    float startY = orbs.get(0).getPosition().y;
    run(5);

    assertTrue(orbs.get(0).getPosition().y < startY, "orbs fly down the lane");
    assertEquals(orbs.get(0).getPosition().x, 0.5f - 0.2f, 1e-4f, "centred under the cannon");
  }

  @Test
  void shouldReportAHitWhenAnOrbReachesThePlayer() {
    buildCannon(100f, 0f);
    // Dead centre in the lane, two units below the mouth.
    player.setPosition(0f, 2f);

    run(30);

    assertEquals(List.of(false), results);
  }

  @Test
  void shouldReportADodgeWhenAnOrbPassesCloseWithoutTouching() {
    buildCannon(100f, 0f);
    // Beside the lane, close enough to be dodging but far enough not to be clipped.
    player.setPosition(0.9f, 2f);

    run(60);

    assertEquals(List.of(true), results);
  }

  @Test
  void shouldIgnoreOrbsThatNeverComeNearThePlayer() {
    buildCannon(100f, 0f);
    player.setPosition(6f, 2f);

    run(60);

    assertTrue(results.isEmpty(), "walking about elsewhere in the room is not a dodge");
  }

  @Test
  void shouldNotFireWhileTimeIsStopped() {
    GameTime frozen = mock(GameTime.class);
    when(frozen.getDeltaTime()).thenReturn(0f);
    ServiceLocator.registerTimeSource(frozen);
    buildCannon(0.1f, 0f);

    run(10);

    assertTrue(orbs.isEmpty(), "a dialogue freezes the world, cannons included");
  }
}
