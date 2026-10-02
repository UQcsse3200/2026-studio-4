package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Player ice input, reserve handover, real collision geometry and the shielded Stage 3 handover.
 */
@ExtendWith(GameExtension.class)
class FinalBossStageTwoPlayerIceIntegrationTest {
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossStageTwoPlayerIceController weapon;
  private FinalBossStageTwoPickupController energy;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossDamageControllerComponent protection;
  private FinalBossStageTwoArenaComponent arena;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private StatusEffectsControllerComponent playerEffects;
  private PlayerActions playerActions;
  private Entity player;
  private Entity boss;
  private EntityService entities;
  private InputService inputs;
  private Input previousInput;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    previousInput = Gdx.input;
    Gdx.input = mock(Input.class);
    inputs = new InputService();
    ServiceLocator.registerInputService(inputs);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    config = new FinalBossStageTwoConfig();
    config.iceCoverCount = 0;
    config.icePickupCount = 0;
    config.fireballInitialDelay = 1000f;
    playerStats = new CombatStatsComponent(100, 10);
    playerActions = new PlayerActions();
    playerEffects = mock(StatusEffectsControllerComponent.class);
    when(playerEffects.modifyIncomingDamage(anyInt())).thenAnswer(call -> call.getArgument(0));
    when(playerEffects.getStatMultiplier(Stat.ATTACK)).thenReturn(1f);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(new PhysicsComponent())
            .addComponent(playerActions)
            .addComponent(playerEffects);
    player.setPosition(9.5f, 9.5f);
    player.create();
    phases = new FinalBossPhaseControllerComponent();
    protection = new FinalBossDamageControllerComponent();
    arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(-100f, -100f, 200f, 200f));
    bossStats = new CombatStatsComponent(1000, 0);
    stageTwo = new FinalBossStageTwoComponent(player, config);
    boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(protection)
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(arena)
            .addComponent(stageTwo);
    boss.setPosition(19.5f, 9.5f);
    boss.create();
    bossStats.setHealth(800);
    weapon = stageTwo.getPlayerIceController();
    energy = stageTwo.getPickupController();
  }

  @AfterEach
  void disposeWorldAndRestoreInput() {
    stageTwo.dispose();
    world.dispose();
    Gdx.input = previousInput;
  }

  @Test
  void homingIceDamagesTheBossWhileOrdinaryMeleeProjectileAndNullSourcesStayBlocked() {
    config.iceProjectileDamage = 1f; // Isolate homing and source protection from fractional damage.
    startEncounter();
    grantCharge();
    bossStats.hit(playerStats);
    bossStats.takeDamage(30, new Entity());
    bossStats.takeDamage(30);
    assertEquals(800, bossStats.getHealth());
    assertTrue(protection.isShielded());
    assertTrue(bossStats.isInvulnerable());
    holdJ();
    advance(0.01f);
    releaseJ();
    assertEquals(1, weapon.shots.size());
    boss.setPosition(19.5f, 11.5f);

    advance(0.05f);
    assertTrue(weapon.shots.getFirst().velocity.y > 0f);
    for (int frame = 0; frame < 70 && bossStats.getHealth() == 800; frame++) advance(0.05f);

    assertEquals(800 - config.iceProjectileDamage, bossStats.getHealth());
    assertTrue(weapon.shots.isEmpty());
    assertTrue(protection.isShielded());
    assertTrue(bossStats.isInvulnerable());
  }

  @Test
  void releasingJPreservesTheRemainingEnergyWhileTheExistingShotKeepsMoving() {
    startEncounter();
    grantCharge();
    holdJ();
    advance(0.01f);
    releaseJ();
    float remaining = energy.getChargeFraction(0);
    FinalBossStageTwoPlayerIceController.Shot shot = weapon.shots.getFirst();
    Vector2 position = shot.position.cpy();

    advance(config.iceFireInterval + 0.01f);

    assertEquals(remaining, energy.getChargeFraction(0), 0.0001f);
    assertEquals(1, weapon.shots.size());
    assertTrue(shot.position.dst(position) > 0f);
    assertEquals(0f, stageTwo.getIceBuffEndRemaining());
  }

  @Test
  void heldJContinuesAcrossTwoReservesAndEndsTheBuffOnlyWhenBothAreEmpty() {
    startEncounter();
    grantCharge();
    grantCharge();
    holdJ();
    for (int shot = 0; shot < config.iceShotsPerCharge; shot++)
      advance(config.iceFireInterval + 0.001f);

    assertEquals(1, energy.getChargeCount());
    assertEquals(0f, energy.getChargeFraction(0));
    assertEquals(1f, energy.getChargeFraction(1), 0.0001f);
    assertEquals(0f, stageTwo.getIceBuffEndRemaining());
    assertTrue(stageTwo.getIceInput().isHeld());
    advance(config.iceFireInterval + 0.001f);
    assertEquals(1f - 1f / config.iceShotsPerCharge, energy.getChargeFraction(1), 0.0001f);
    for (int shot = 1; shot < config.iceShotsPerCharge; shot++)
      advance(config.iceFireInterval + 0.001f);

    assertEquals(0, energy.getChargeCount());
    assertEquals(stageTwo.getIceBuffEndDuration(), stageTwo.getIceBuffEndRemaining(), 0.0001f);
    advance(0.1f);
    assertTrue(stageTwo.getIceBuffEndRemaining() > 0f);
    assertTrue(stageTwo.getIceBuffEndRemaining() < stageTwo.getIceBuffEndDuration());
  }

  @Test
  void collectingAnotherReserveCancelsAnAlreadyPlayingBuffEndEffect() {
    startEncounter();
    grantCharge();
    energy.consumeEnergy(1f - 1f / config.iceShotsPerCharge);
    holdJ();
    advance(0.01f);
    releaseJ();
    assertEquals(0, energy.getChargeCount());
    assertTrue(stageTwo.getIceBuffEndRemaining() > 0f);

    grantCharge();

    assertEquals(1, energy.getChargeCount());
    assertEquals(0f, stageTwo.getIceBuffEndRemaining());
  }

  @Test
  void controlLockAndImmobilisationStopNewShotsWithoutRemovingOrFreezingExistingShots() {
    startEncounter();
    grantCharge();
    holdJ();
    advance(0.01f);
    FinalBossStageTwoPlayerIceController.Shot shot = weapon.shots.getFirst();
    Vector2 before = shot.position.cpy();
    float remaining = energy.getChargeFraction(0);
    Object dialogue = new Object();
    playerActions.setControlsLocked(dialogue, true);

    advance(config.iceFireInterval + 0.01f);

    assertEquals(1, weapon.shots.size());
    assertEquals(remaining, energy.getChargeFraction(0), 0.0001f);
    assertTrue(shot.position.dst(before) > 0f);
    before.set(shot.position);
    playerActions.setControlsLocked(dialogue, false);
    when(playerEffects.isImmobilised()).thenReturn(true);
    advance(config.iceFireInterval + 0.01f);
    assertEquals(1, weapon.shots.size());
    assertEquals(remaining, energy.getChargeFraction(0), 0.0001f);
    assertTrue(shot.position.dst(before) > 0f);

    when(playerEffects.isImmobilised()).thenReturn(false);
    advance(config.iceFireInterval + 0.01f);
    assertEquals(2, weapon.shots.size());
    assertTrue(energy.getChargeFraction(0) < remaining);
  }

  @Test
  void holdingJWithoutAnyReserveDoesNotCreateFreeIceShotsOrAnEndEffect() {
    startEncounter();
    holdJ();

    advance(1f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(0, energy.getChargeCount());
    assertEquals(0f, stageTwo.getIceBuffEndRemaining());
  }

  @Test
  void realStaticWallConsumesTheIceShotBeforeItCanReachTheBoss() {
    startEncounter();
    addWall(15f, 10f);
    grantCharge();
    holdJ();
    advance(0.01f);
    releaseJ();
    assertEquals(1, weapon.shots.size());

    advance(1.1f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(1, weapon.impacts.size());
    assertEquals(800, bossStats.getHealth());
  }

  @Test
  void realIceCoverBlocksPlayerIceWithoutLosingAnyFireballDurability() {
    config.iceCoverCount = 1;
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = stageTwo.getIceController().covers.getFirst();
    when(arena.getBounds()).thenReturn(new Rectangle(-200f, -200f, 400f, 400f));
    float y = cover.bounds.y + cover.bounds.height / 2f;
    player.setPosition(cover.bounds.x - 2.5f, y - 0.5f);
    boss.setPosition(cover.bounds.x + cover.bounds.width + 2.5f, y - 0.5f);
    advance(0.001f);
    grantCharge();
    holdJ();
    advance(0.01f);
    releaseJ();
    assertEquals(1, weapon.shots.size());

    advance(0.6f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(config.iceCoverHits, cover.hitsRemaining);
    assertEquals(1, stageTwo.getIceController().covers.size());
    assertTrue(stageTwo.getIceController().shatters.isEmpty());
    assertEquals(800, bossStats.getHealth());
  }

  @Test
  void reachingTheHealthFloorFromAnIceShotClearsTheEncounterInsideTheHitCallback() {
    config.iceProjectileDamage = 1f; // One hit must trigger this synchronous-cleanup fixture.
    config.iceCoverCount = 1;
    startEncounter();
    grantCharge();
    grantCharge();
    bossStats.setHealth(601);
    Vector2 beforeBoss = boss.getCenterPosition().add(-1f, 0f);
    weapon.shots.add(
        new FinalBossStageTwoPlayerIceController.Shot(900L, beforeBoss, new Vector2(6f, 0f)));
    weapon.shots.add(
        new FinalBossStageTwoPlayerIceController.Shot(901L, beforeBoss, new Vector2(6f, 0f)));
    weapon.impacts.add(new FinalBossStageTwoPlayerIceController.Impact(new Vector2(0f, 0f)));
    stageTwo
        .getFireController()
        .fireballs
        .add(
            new FinalBossStageTwoFireController.Fireball(
                902L, new Vector2(-80f, -80f), new Vector2(1f, 0f)));
    energy.pickups.add(new FinalBossStageTwoPickupController.Pickup(new Vector2(80f, 80f)));
    energy.disappearances.add(new FinalBossStageTwoPickupController.Burst(new Vector2(0f, 0f)));
    holdJ();

    advance(0.2f);

    assertEquals(600, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertTrue(bossStats.isInvulnerable());
    assertTrue(protection.isShielded());
    assertWeaponResourcesCleared();
    assertTrue(stageTwo.getFireController().fireballs.isEmpty());
    assertTrue(stageTwo.getIceController().covers.isEmpty());
    bossStats.takeDamage(10000, player);
    protection.takeStageTwoIceDamage(10000, player);
    assertEquals(600, bossStats.getHealth());
  }

  @Test
  void localPlayerInvulnerabilityStillBlocksFireDamageAndAllowsIceDamageToTheBoss() {
    config.iceProjectileDamage = 1f;
    playerStats.setIncomingDamageMultiplier(0f);
    startEncounter();
    grantCharge();
    holdJ();
    advance(0.01f);
    releaseJ();
    stageTwo
        .getFireController()
        .fireballs
        .add(
            new FinalBossStageTwoFireController.Fireball(
                903L, new Vector2(9f, 10f), new Vector2(6f, 0f)));

    advance(0.25f);
    advance(2f);

    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, playerStats.getIncomingDamageMultiplier());
    assertEquals(800 - config.iceProjectileDamage, bossStats.getHealth());
  }

  @Test
  void oneReserveFiresEightQuarterDamageShotsAndRemovesTwoBossHealth() {
    bossStats.setMaxHealth(100);
    bossStats.setHealth(80);
    startEncounter();
    grantCharge();
    holdJ();
    Set<Long> emitted = new HashSet<>();

    for (int shot = 0; shot < 8; shot++) {
      advance(config.iceFireInterval + 0.001f);
      weapon.shots.forEach(projectile -> emitted.add(projectile.id));
    }
    assertEquals(8, emitted.size());
    assertEquals(0, energy.getChargeCount());
    // Every emitted shot can reach the stationary boss before its four-second lifetime ends.
    advance(2f);

    assertEquals(78, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertTrue(weapon.shots.isEmpty());
    assertTrue(protection.isShielded());
    assertTrue(bossStats.isInvulnerable());
  }

  @Test
  void eightiethQuarterDamageImpactReachesSixtyPercentAndStartsTheShieldedStageThreeTransition() {
    bossStats.setMaxHealth(100);
    bossStats.setHealth(80);
    startEncounter();
    for (int reserve = 0; reserve < 9; reserve++) {
      grantCharge();
      holdJ();
      for (int shot = 0; shot < 8; shot++) advance(config.iceFireInterval + 0.001f);
      releaseJ();
      assertEquals(0, energy.getChargeCount());
    }
    grantCharge();
    holdJ();
    for (int shot = 0; shot < 7; shot++) advance(config.iceFireInterval + 0.001f);
    releaseJ();
    advance(2f);
    assertTrue(weapon.shots.isEmpty());
    assertEquals(61, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertEquals(1f / 8f, energy.getChargeFraction(0), 0.0001f);
    bossStats.takeDamage(10000, player);
    assertEquals(61, bossStats.getHealth());

    holdJ();
    advance(config.iceFireInterval + 0.001f);
    releaseJ();
    for (int frame = 0;
        frame < 50 && phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO;
        frame++) {
      advance(0.05f);
    }

    assertEquals(60, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertTrue(protection.isShielded());
    assertTrue(bossStats.isInvulnerable());
    assertWeaponResourcesCleared();
  }

  @Test
  void playerDeathClearsHeldInputShotsEnergyAndEndEffects() {
    startEncounter();
    grantCharge();
    holdJ();
    advance(0.01f);
    assertFalse(weapon.shots.isEmpty());

    playerStats.setHealth(0);
    advance(0.01f);

    assertWeaponResourcesCleared();
  }

  @Test
  void disposingTheBossClearsHeldInputShotsEnergyAndEndEffects() {
    startEncounter();
    grantCharge();
    holdJ();
    advance(0.01f);
    assertFalse(weapon.shots.isEmpty());

    stageTwo.dispose();

    assertWeaponResourcesCleared();
  }

  private void startEncounter() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertFalse(phases.isTransitioning());
    advance(0.01f);
  }

  private void grantCharge() {
    energy.pickups.add(new FinalBossStageTwoPickupController.Pickup(player.getCenterPosition()));
    advance(0.001f);
  }

  private void holdJ() {
    when(Gdx.input.isKeyPressed(Input.Keys.J)).thenReturn(true);
    inputs.keyDown(Input.Keys.J);
  }

  private void releaseJ() {
    when(Gdx.input.isKeyPressed(Input.Keys.J)).thenReturn(false);
    inputs.keyUp(Input.Keys.J);
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageTwo.update();
    entities.update();
  }

  private void assertWeaponResourcesCleared() {
    assertTrue(weapon.shots.isEmpty());
    assertTrue(weapon.impacts.isEmpty());
    assertEquals(0, energy.getChargeCount());
    assertTrue(energy.pickups.isEmpty());
    assertTrue(energy.disappearances.isEmpty());
    assertEquals(0f, stageTwo.getIceBuffEndRemaining());
    assertFalse(stageTwo.getIceInput().isHeld());
  }

  private void addWall(float x, float y) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.2f, 4f);
    world.createBody(definition).createFixture(shape, 0f);
    shape.dispose();
  }
}
