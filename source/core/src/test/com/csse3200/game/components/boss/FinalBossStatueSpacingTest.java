package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStatueSpacingTest {
  private static final float STEP = 1f / 60f;
  private static final float EPSILON = 0.02f;
  private static final float MIN_SPACING = 3f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossMovementComponent bossMovement;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;
  private Rectangle arena;

  @BeforeEach
  void setup() {
    MathUtils.random.setSeed(2026L);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(STEP);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    player.create();
    player.getComponent(CombatStatsComponent.class).setInvulnerable(true);
    config = new FinalBossStageThreeConfig();
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    bossMovement = mock(FinalBossMovementComponent.class);
    setArena(16f, 8f);
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(bossMovement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void fiveStatuesSpawnThreeUnitsApartWithJumpHeadroomInAShortArena() {
    startStatues();
    assertEquals(5, stage.statues.size());
    assertEquals(MIN_SPACING, config.statueMinSpacing);
    assertStatuesAreSpaced();
    assertInsideArenaWithJumpHeadroom();
  }

  @Test
  void blockedSpawnUsesSeparatedWallFreeAlternativesWhenThereIsRoom() {
    Rectangle wallBounds = new Rectangle(4.8f, -1f, 1.6f, 2f);
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent());
    wall.setPosition(wallBounds.x, wallBounds.y);
    wall.setScale(wallBounds.width, wallBounds.height);
    wall.create();

    startStatues();

    assertEquals(5, stage.statues.size());
    assertStatuesAreSpaced();
    assertInsideArenaWithJumpHeadroom();
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      Vector2 position = statue.entity.getPosition();
      Vector2 size = statue.entity.getScale();
      assertFalse(
          wallBounds.overlaps(new Rectangle(position.x, position.y, size.x, size.y)),
          "Statues must use the available space around the wall");
    }
  }

  @Test
  void everyStatueWalksAndKeepsItsSpacingWithSeedSeven() {
    assertWalkingAndSpacingForTwentySeconds(7L);
  }

  @Test
  void everyStatueWalksAndKeepsItsSpacingWithSeedThirtyOne() {
    assertWalkingAndSpacingForTwentySeconds(31L);
  }

  @Test
  void everyStatueWalksAndKeepsItsSpacingWithSeedTwentyTwentySix() {
    assertWalkingAndSpacingForTwentySeconds(2026L);
  }

  private void assertWalkingAndSpacingForTwentySeconds(long seed) {
    MathUtils.random.setSeed(seed);
    startStatues();
    List<Vector2> initialPositions =
        stage.statues.stream().map(statue -> statue.entity.getCenterPosition()).toList();
    float[] maxDisplacement = new float[stage.statues.size()];
    for (int frame = 0; frame < 1200; frame++) {
      tickPhysics();
      assertStatuesAreSpaced();
      assertInsideArenaWithJumpHeadroom();
      for (int i = 0; i < stage.statues.size(); i++) {
        maxDisplacement[i] =
            Math.max(
                maxDisplacement[i],
                initialPositions.get(i).dst(stage.statues.get(i).entity.getCenterPosition()));
      }
    }
    for (int i = 0; i < maxDisplacement.length; i++) {
      assertTrue(maxDisplacement[i] > 0.2f, "Statue " + i + " must actually walk, seed " + seed);
    }
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    assertEquals(5, stage.getRemainingStatues());
  }

  @Test
  void convergingRoutesAreRevalidatedBeforeStatuesGetTooClose() {
    assertConflictingRoutesAreRevalidated(false);
  }

  @Test
  void crossingRoutesAreRevalidatedBeforeStatuesGetTooClose() {
    assertConflictingRoutesAreRevalidated(true);
  }

  private void assertConflictingRoutesAreRevalidated(boolean crossing) {
    setArena(24f, 16f);
    startStatues();
    Vector2[] centres = {
      new Vector2(-3f, -2f),
      new Vector2(-3f, 2f),
      new Vector2(-9f, -5f),
      new Vector2(9f, -5f),
      new Vector2(0f, 5f)
    };
    for (int i = 0; i < centres.length; i++) {
      FinalBossStageThreeComponent.Statue statue = stage.statues.get(i);
      statue.entity.setPosition(centres[i].cpy().sub(statue.entity.getScale().scl(0.5f)));
      statue.pauseRemaining = i < 2 ? 0f : 100f;
      statue.destination = null;
      statue.entity.getComponent(PhysicsMovementComponent.class).setMoving(false);
    }
    FinalBossStageThreeComponent.Statue first = stage.statues.get(0);
    FinalBossStageThreeComponent.Statue second = stage.statues.get(1);
    Vector2 firstDestination = new Vector2(3f, crossing ? 2f : 0f);
    Vector2 secondDestination = new Vector2(3f, crossing ? -2f : 0f);
    first.destination = firstDestination.cpy();
    second.destination = secondDestination.cpy();
    first.moveRemaining = 20f;
    second.moveRemaining = 20f;
    assertStatuesAreSpaced();

    tickPhysics();

    assertTrue(
        routeWasChangedOrStopped(first, firstDestination)
            || routeWasChangedOrStopped(second, secondDestination),
        "At least one unsafe route must be changed or stopped before physics advances");
    for (int frame = 0; frame < 600; frame++) {
      tickPhysics();
      assertStatuesAreSpaced();
    }
  }

  private boolean routeWasChangedOrStopped(
      FinalBossStageThreeComponent.Statue statue, Vector2 oldDestination) {
    return statue.destination == null
        || !statue.destination.epsilonEquals(oldDestination, EPSILON)
        || !statue.entity.getComponent(PhysicsMovementComponent.class).getMoving();
  }

  private void setArena(float width, float height) {
    when(bossMovement.getCamera()).thenReturn(new OrthographicCamera(width, height));
    arena = new Rectangle(-width / 2f, -height / 2f, width, height);
  }

  private void startStatues() {
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    when(time.getDeltaTime()).thenReturn(config.chargeDuration);
    stage.update();
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) statue.slamCooldown = 100f;
    when(time.getDeltaTime()).thenReturn(STEP);
  }

  private void tickPhysics() {
    stage.update();
    for (FinalBossStageThreeComponent.Statue statue : stage.statues)
      statue.entity.getComponent(PhysicsMovementComponent.class).update();
    world.step(STEP, 6, 2);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues)
      statue.entity.getComponent(PhysicsComponent.class).earlyUpdate();
    player.getComponent(PhysicsComponent.class).earlyUpdate();
  }

  private void assertStatuesAreSpaced() {
    for (int i = 0; i < stage.statues.size(); i++) {
      for (int j = 0; j < i; j++) {
        float distance =
            stage
                .statues
                .get(i)
                .entity
                .getCenterPosition()
                .dst(stage.statues.get(j).entity.getCenterPosition());
        assertTrue(
            distance >= MIN_SPACING - EPSILON,
            "Statues " + i + " and " + j + " are only " + distance + " units apart");
      }
    }
  }

  private void assertInsideArenaWithJumpHeadroom() {
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      Vector2 position = statue.entity.getPosition();
      Vector2 size = statue.entity.getScale();
      assertTrue(position.x >= arena.x - EPSILON);
      assertTrue(position.x + size.x <= arena.x + arena.width + EPSILON);
      assertTrue(position.y >= arena.y - EPSILON);
      assertTrue(
          position.y + size.y + config.statueJumpHeight + 0.32f
              <= arena.y + arena.height + EPSILON);
    }
  }
}
