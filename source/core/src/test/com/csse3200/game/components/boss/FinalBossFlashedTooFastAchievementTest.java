package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponAttackSourceComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Real weapon hitboxes and their TouchAttack collision path feeding the Stage 3 melee result. */
@ExtendWith(GameExtension.class)
class FinalBossFlashedTooFastAchievementTest {
  private static final String ACHIEVEMENT_NAME = "You flashed too fast";

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> hitPlayers = new ArrayList<>();
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossAchievementComponent tracker;
  private AchievementService achievements;
  private Achievement achievement;
  private Entity boss;
  private Entity player;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
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
    ServiceLocator.registerRenderService(mock(RenderService.class));
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    AchievementConfig definition = new AchievementConfig();
    definition.name = ACHIEVEMENT_NAME;
    definition.type = "finalBossStageThreeMeleeHit";
    achievement = AchievementsFactory.build(definition);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
    ServiceLocator.registerAchievementService(achievements);

    player = createWielder();
    playerStats = player.getComponent(CombatStatsComponent.class);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    FinalBossStageOneComponent stageOne = mock(FinalBossStageOneComponent.class);
    when(stageOne.getState()).thenReturn(FinalBossStageOneState.INTRO);
    config = new FinalBossStageThreeConfig();
    phases = new FinalBossPhaseControllerComponent();
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    tracker = new FinalBossAchievementComponent(player);
    bossStats = new CombatStatsComponent(100, 0);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stageOne)
            .addComponent(stage)
            .addComponent(tracker);
    boss.setPosition(4f, 0f);
    boss.create();
    bossStats.setHealth(60);
    boss.getEvents().<Entity>addListener(FinalBossEvents.STAGE_THREE_MELEE_HIT, hitPlayers::add);
  }

  @AfterEach
  void disposeEncounter() {
    stage.dispose();
    tracker.dispose();
    world.dispose();
  }

  @Test
  void shouldUnlockFromARealSwordHitboxContact() {
    enterWaveOne();
    Entity blade = attack(player, WeaponType.SWORD, false);
    assertSource(blade, player, WeaponType.SWORD);
    assertStillLocked();

    collideWithBoss(blade);

    assertEquals(50, bossStats.getHealth());
    assertEquals(List.of(player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldUnlockFromARealKnifeStab() {
    enterWaveOne();
    Entity blade = attack(player, WeaponType.DAGGER, false);
    assertSource(blade, player, WeaponType.DAGGER);

    collideWithBoss(blade);

    assertEquals(50, bossStats.getHealth());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldUnlockFromTheUpgradedSwordHeavySweep() {
    enterWaveOne();
    player.getComponent(WeaponUpgradeComponent.class).setUpgraded(SwordWeaponComponent.class, true);
    Entity blade = attack(player, WeaponType.SWORD, true);
    assertSource(blade, player, WeaponType.SWORD);

    collideWithBoss(blade);

    assertTrue(bossStats.getHealth() < 60);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldKeepThePlayerAndKnifeTypeOnEveryQueuedHeavyStrike() {
    enterWaveOne();
    player.getComponent(WeaponUpgradeComponent.class).setUpgraded(KnifeWeaponComponent.class, true);
    Entity firstSlash = attack(player, WeaponType.DAGGER, true);
    when(time.getDeltaTime()).thenReturn(0.15f);
    player.getComponent(KnifeWeaponComponent.class).update();
    Entity secondSlash = entities.getEntities().peek();
    player.getComponent(KnifeWeaponComponent.class).update();
    Entity finisher = entities.getEntities().peek();
    assertEquals(3, entities.getEntities().size);
    for (Entity strike : List.of(firstSlash, secondSlash, finisher)) {
      assertSource(strike, player, WeaponType.DAGGER);
    }
    assertStillLocked();

    // The first slash misses; a queued slash and the finisher still qualify as knife contacts.
    collideWithBoss(secondSlash);
    collideWithBoss(finisher);

    assertEquals(List.of(player, player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotUnlockFromSwingingWithoutAContact() {
    enterWaveOne();
    attack(player, WeaponType.SWORD, false);
    attack(player, WeaponType.DAGGER, false);

    assertEquals(60, bossStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldIgnoreAThrownDartEvenAfterThePlayerSwitchesToTheSword() {
    enterWaveOne();
    Entity dart = attack(player, WeaponType.BOW, false);
    assertNull(dart.getComponent(WeaponAttackSourceComponent.class));
    assertTrue(player.getComponent(WeaponSelectionComponent.class).equip(WeaponType.SWORD));

    collideWithBoss(dart);

    assertEquals(50, bossStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
    collideWithBoss(attack(player, WeaponType.SWORD, false));
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreAnotherWieldersRealSwordHitbox() {
    enterWaveOne();
    Entity otherPlayer = createWielder();
    Entity blade = attack(otherPlayer, WeaponType.SWORD, false);
    assertSource(blade, otherPlayer, WeaponType.SWORD);

    collideWithBoss(blade);

    assertEquals(50, bossStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
    collideWithBoss(attack(player, WeaponType.DAGGER, false));
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreUntypedDamageAndZeroDamageWeaponContacts() {
    enterWaveOne();
    bossStats.takeDamage(1, null);
    bossStats.takeDamage(1, player);
    bossStats.takeDamage(1, new Entity());
    playerStats.setBaseAttack(0);

    collideWithBoss(attack(player, WeaponType.SWORD, false));

    assertEquals(57, bossStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldIgnoreEarlierPhasesAndAnEarlyWaveStartDuringTheTransition() {
    Entity blade = attack(player, WeaponType.SWORD, false);
    collideWithBoss(blade);
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    collideWithBoss(blade);
    assertStillLocked();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    stage.startWaveOne();
    assertTrue(phases.isTransitioning());

    collideWithBoss(blade);

    assertStillLocked();
    finishTransition();
    collideWithBoss(blade);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreChargingAndWaveTwoEvenForAValidMeleeSource() {
    enterWaveOne();
    bossStats.setHealth(20);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    Entity blade = attack(player, WeaponType.SWORD, false);
    collideWithBoss(blade);
    assertStillLocked();
    when(time.getDeltaTime()).thenReturn(config.chargeDuration);
    stage.update();
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());

    collideWithBoss(blade);

    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldReportTheHitBeforeItEndsWaveOneAtTheHealthFloor() {
    enterWaveOne();
    bossStats.setHealth(21);
    List<Integer> healthAtUnlock = new ArrayList<>();
    achievements
        .getEvents()
        .addListener(
            "achievementUnlocked", (String name) -> healthAtUnlock.add(bossStats.getHealth()));

    collideWithBoss(attack(player, WeaponType.SWORD, false));

    assertEquals(List.of(21), healthAtUnlock);
    assertEquals(20, bossStats.getHealth());
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotifyOnlyOnceForRepeatedMeleeContacts() {
    enterWaveOne();

    collideWithBoss(attack(player, WeaponType.SWORD, false));
    collideWithBoss(attack(player, WeaponType.DAGGER, false));

    assertEquals(List.of(player, player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotNotifyAgainWhenTheAchievementWasRestored() {
    achievements.restoreState(List.of(ACHIEVEMENT_NAME), Map.of());
    enterWaveOne();

    collideWithBoss(attack(player, WeaponType.SWORD, false));

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(player), hitPlayers);
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldRejectShieldedHitsButAcceptAnUnshieldedContactWithoutWholeHealthLoss() {
    enterWaveOne();
    Entity blade = attack(player, WeaponType.SWORD, false);
    bossStats.setInvulnerable(true);
    collideWithBoss(blade);
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
    bossStats.setInvulnerable(false);
    bossStats.setIncomingDamageMultiplier(0f);

    collideWithBoss(blade);

    assertEquals(60, bossStats.getHealth());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreContactsWhileThePlayerOrBossIsDead() {
    enterWaveOne();
    Entity blade = attack(player, WeaponType.SWORD, false);
    playerStats.setHealth(0);
    collideWithBoss(blade);
    assertStillLocked();
    playerStats.setHealth(100);
    bossStats.setHealth(0);

    collideWithBoss(blade);

    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotUnlockAfterTheTrackerOrEncounterIsDisposed() {
    enterWaveOne();
    tracker.dispose();
    collideWithBoss(attack(player, WeaponType.SWORD, false));
    assertEquals(List.of(player), hitPlayers);
    assertStillLocked();
    stage.dispose();

    collideWithBoss(attack(player, WeaponType.DAGGER, false));

    assertEquals(List.of(player), hitPlayers);
    assertStillLocked();
  }

  private Entity createWielder() {
    Entity wielder =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions())
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 0f))
            .addComponent(new WeaponUpgradeComponent())
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(new WeaponSelectionComponent());
    wielder.create();
    return wielder;
  }

  private Entity attack(Entity wielder, WeaponType type, boolean heavy) {
    assertTrue(wielder.getComponent(WeaponSelectionComponent.class).equip(type));
    wielder.getComponent(WeaponStatsComponent.class).update(10f);
    int previousCount = entities.getEntities().size;
    wielder.getEvents().trigger(heavy ? "weaponHeavyAttack" : "weaponAttack", new Vector2(1f, 0f));
    assertEquals(previousCount + 1, entities.getEntities().size);
    return entities.getEntities().peek();
  }

  private void collideWithBoss(Entity hitbox) {
    hitbox
        .getEvents()
        .trigger(
            "collisionStart",
            hitbox.getComponent(HitboxComponent.class).getFixture(),
            boss.getComponent(HitboxComponent.class).getFixture());
  }

  private void assertSource(Entity hitbox, Entity owner, WeaponType type) {
    WeaponAttackSourceComponent source = hitbox.getComponent(WeaponAttackSourceComponent.class);
    assertNotNull(source);
    assertSame(owner, source.getOwner());
    assertEquals(type, source.getWeaponType());
  }

  private void enterWaveOne() {
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    finishTransition();
  }

  private void finishTransition() {
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stage.getState());
  }

  private void assertStillLocked() {
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }
}
