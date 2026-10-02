package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Pickup integration with actual stage transitions, player physics and static obstacles. */
@ExtendWith(GameExtension.class)
class FinalBossStageTwoPickupIntegrationTest {
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossStageTwoPickupController pickups;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossStageTwoArenaComponent arena;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private Entity player;
  private Entity boss;
  private EntityService entities;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    config = new FinalBossStageTwoConfig();
    config.iceCoverCount = 0;
    config.fireballInitialDelay = 1000f;
    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats).addComponent(new PhysicsComponent());
    player.setPosition(24.5f, 24.5f);
    player.create();
    phases = new FinalBossPhaseControllerComponent();
    arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(0f, 0f, 40f, 40f));
    bossStats = new CombatStatsComponent(1000, 0);
    stageTwo = new FinalBossStageTwoComponent(player, config);
    boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(arena)
            .addComponent(stageTwo);
    boss.setPosition(14.5f, 14.5f);
    boss.create();
    bossStats.setHealth(800);
    pickups = stageTwo.getPickupController();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void transformationDoesNotSpawnPickupsBeforeTheEncounterStarts() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);

    advance(3f);

    assertTrue(phases.isTransitioning());
    assertTrue(pickups.pickups.isEmpty());
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    advance(0.01f);
    assertFalse(phases.isTransitioning());
    assertEquals(1, pickups.pickups.size());
    assertEquals(0, pickups.getChargeCount());
  }

  @Test
  void aDashUsesFreshPhysicsPositionsAndCollectsAGemBetweenItsEndpoints() {
    startEncounter();
    Vector2 gem = onlyPickupPosition();
    Vector2 approach = player.getCenterPosition().sub(gem).nor().scl(2f);
    Vector2 before = gem.cpy().add(approach);
    player.setPosition(before.x - 0.5f, before.y - 0.5f);
    advance(0.01f);
    assertEquals(0, pickups.getChargeCount());
    Vector2 after = gem.cpy().sub(approach);
    // The player entity has not synced its physics position yet, as can happen during a dash.
    player
        .getComponent(PhysicsComponent.class)
        .getBody()
        .setTransform(after.x - 0.5f, after.y - 0.5f, 0f);

    advance(0.01f);

    assertEquals(1, pickups.getChargeCount());
    assertEquals(1f, pickups.getChargeFraction(0), 0.0001f);
    assertTrue(pickups.pickups.isEmpty());
    assertFalse(pickups.bursts.isEmpty());
  }

  @Test
  void uncollectedGemExpiresAfterSevenSecondsEvenDuringTheFiringPause() {
    config.attackDuration = 1f;
    config.pauseDuration = 10f;
    config.icePickupSpawnInterval = 100f;
    startEncounter();
    assertEquals(1, pickups.pickups.size());

    advance(6.9f);
    assertFalse(stageTwo.isAttacking());
    assertEquals(1, pickups.pickups.size());
    advance(0.11f);

    assertFalse(stageTwo.isAttacking());
    assertTrue(pickups.pickups.isEmpty());
    assertEquals(0, pickups.getChargeCount());
    assertTrue(pickups.bursts.isEmpty());
  }

  @Test
  void staticWallFixturesPreventSpawningUntilTheSpaceIsClear() {
    Body wall = addWall(new Rectangle(-1f, -1f, 42f, 42f));
    startEncounter();
    assertTrue(pickups.pickups.isEmpty());

    world.destroyBody(wall);
    advance(config.icePickupSpawnInterval + 0.01f);

    assertEquals(1, pickups.pickups.size());
  }

  @Test
  void anExistingIceFixtureExcludesGemPlacementUntilTheIceIsRemoved() {
    config.iceCoverCount = 1;
    startEncounter();
    FinalBossStageTwoIceController ice = stageTwo.getIceController();
    assertEquals(1, ice.covers.size());
    FinalBossStageTwoIceController.Cover cover = ice.covers.getFirst();
    boss.setPosition(-90f, -90f);
    player.setPosition(90f, 90f);
    pickups.clear();
    // Every allowed pickup clearance rectangle intersects this real central ice fixture.
    when(arena.getBounds())
        .thenReturn(
            new Rectangle(
                cover.bounds.x - 1.5f,
                cover.bounds.y - 1.5f,
                cover.bounds.width + 3f,
                cover.bounds.height + 3f));

    advance(0.01f);

    assertEquals(1, ice.covers.size());
    assertTrue(pickups.pickups.isEmpty());
    for (int hit = 0; hit < config.iceCoverHits; hit++) {
      ice.hitByFire(cover.entity.getComponent(ColliderComponent.class).getFixture());
    }
    config.iceCoverCount = 0;
    pickups.clear();
    advance(0.01f);
    assertTrue(ice.covers.isEmpty());
    assertEquals(1, pickups.pickups.size());
  }

  @Test
  void laterIceCoverCannotSpawnOnTopOfAnExistingGem() {
    startEncounter();
    pickups.clear();
    pickups.pickups.add(new FinalBossStageTwoPickupController.Pickup(new Vector2(20f, 20f)));
    config.iceCoverCount = 1;
    boss.setPosition(-90f, -90f);
    player.setPosition(90f, 90f);
    float width = config.iceCoverWidth + 2f * config.iceCoverGap + 0.02f;
    float height = config.iceCoverHeight + 2f * config.iceCoverGap + 0.02f;
    when(arena.getBounds())
        .thenReturn(new Rectangle(20f - width / 2f, 20f - height / 2f, width, height));

    advance(config.iceCoverRespawnInterval + 0.01f);

    assertTrue(stageTwo.getIceController().covers.isEmpty());
    assertEquals(1, pickups.pickups.size());
    pickups.clear();
    advance(config.iceCoverRespawnInterval + 0.01f);
    assertEquals(1, stageTwo.getIceController().covers.size());
  }

  @Test
  void localPlayerInvulnerabilityStillAllowsNormalPickupCollection() {
    playerStats.setIncomingDamageMultiplier(0f);
    startEncounter();

    collectFirstPickup();

    assertEquals(1, pickups.getChargeCount());
    assertTrue(pickups.pickups.isEmpty());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void stageThreeHandoverClearsChargesGroundGemsAndCollectionEffects() {
    prepareHeldChargeGroundGemAndBurst();

    boss.getComponent(FinalBossDamageControllerComponent.class)
        .takeStageTwoIceDamage(10000, player);

    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertPickupResourcesCleared();
    advance(1f);
    assertPickupResourcesCleared();
  }

  @Test
  void playerDeathClearsChargesGroundGemsAndCollectionEffects() {
    prepareHeldChargeGroundGemAndBurst();

    playerStats.setHealth(0);
    advance(0.01f);

    assertPickupResourcesCleared();
  }

  @Test
  void bossDeathClearsChargesGroundGemsAndCollectionEffects() {
    prepareHeldChargeGroundGemAndBurst();

    bossStats.setHealth(0);

    assertPickupResourcesCleared();
  }

  @Test
  void disposalClearsChargesGroundGemsAndCollectionEffects() {
    prepareHeldChargeGroundGemAndBurst();

    stageTwo.dispose();

    assertPickupResourcesCleared();
    advance(1f);
    assertPickupResourcesCleared();
  }

  @Test
  void losingTheArenaClearsChargesGroundGemsAndCollectionEffects() {
    prepareHeldChargeGroundGemAndBurst();
    when(arena.getBounds()).thenReturn(null);

    advance(0.01f);

    assertPickupResourcesCleared();
  }

  private void prepareHeldChargeGroundGemAndBurst() {
    startEncounter();
    collectFirstPickup();
    pickups.pickups.add(new FinalBossStageTwoPickupController.Pickup(new Vector2(10f, 10f)));
    assertEquals(1, pickups.getChargeCount());
    assertEquals(1, pickups.pickups.size());
    assertFalse(pickups.bursts.isEmpty());
  }

  private void collectFirstPickup() {
    Vector2 position = onlyPickupPosition();
    player
        .getComponent(PhysicsComponent.class)
        .getBody()
        .setTransform(position.x - 0.5f, position.y - 0.5f, 0f);
    advance(0.01f);
  }

  private Vector2 onlyPickupPosition() {
    assertEquals(1, pickups.pickups.size());
    return pickups.pickups.getFirst().position.cpy();
  }

  private void startEncounter() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertFalse(phases.isTransitioning());
    advance(0.01f);
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageTwo.update();
    entities.update();
  }

  private void assertPickupResourcesCleared() {
    assertEquals(0, pickups.getChargeCount());
    assertTrue(pickups.pickups.isEmpty());
    assertTrue(pickups.bursts.isEmpty());
  }

  private Body addWall(Rectangle bounds) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
    Body wall = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(bounds.width / 2f, bounds.height / 2f);
    wall.createFixture(shape, 0f);
    shape.dispose();
    return wall;
  }
}
