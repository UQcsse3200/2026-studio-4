package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Uses the real burrow clock and damage path to check the short post-emergence window. */
@ExtendWith(GameExtension.class)
class SnakeWhackAMoleAchievementTest {
  private static final String NAME = "Whack-a-Mole";
  private final List<String> unlockedNames = new ArrayList<>();
  private GameTime time;
  private PhysicsService physics;
  private EntityService entities;
  private AchievementService achievements;
  private Achievement achievement;
  private SnakeMiniBossConfig config;
  private Entity player;
  private Entity snake;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent snakeStats;
  private SnakeBurrowComponent burrow;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(32);
    when(texture.getHeight()).thenReturn(32);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    AchievementConfig definition = new AchievementConfig();
    definition.type = "snake";
    definition.name = NAME;
    definition.bossAchievementId = SnakeAchievementComponent.WHACK_A_MOLE;
    achievement = AchievementsFactory.build(definition);
    achievements = spy(new AchievementService());
    achievements.register(achievement);
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
    ServiceLocator.registerAchievementService(achievements);

    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats);
    // Leave player uncreated so individual tests can attach a real weapon or spell.
    createSnake(false);
  }

  @AfterEach
  void cleanUp() {
    snake.dispose();
    physics.getPhysics().getWorld().dispose();
  }

  @ParameterizedTest
  @CsvSource({"false,sword", "false,knife", "false,bow", "true,sword", "true,knife", "true,bow"})
  void actualWeaponSourcesQualifyDuringEitherStage(boolean stageTwo, String kind) {
    reachExposed(stageTwo);
    WeaponComponent weapon =
        switch (kind) {
          case "sword" -> new SwordWeaponComponent();
          case "knife" -> new KnifeWeaponComponent();
          default -> new BowWeaponComponent();
        };
    player.addComponent(new WeaponStatsComponent(0.5f, 1f, 0f)).addComponent(weapon);
    player.create();
    assertTrue(weapon.attack(player.getCenterPosition(), new Vector2(1f, 0f)));
    ArgumentCaptor<Entity> attack = ArgumentCaptor.forClass(Entity.class);
    verify(entities).register(attack.capture());

    snakeStats.hit(attack.getValue().getComponent(CombatStatsComponent.class));

    assertUnlockedOnce();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void directPlayerHitQualifiesUntilTheEndOfThePause(boolean stageTwo) {
    reachExposed(stageTwo);
    tick(config.exposedDuration - 0.01f);

    snakeStats.takeDamage(1, player);

    assertUnlockedOnce();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  void hitsBeforeEmergenceDoNotQualifyEvenIfInvulnerabilityIsRemoved(int transitions) {
    if (transitions >= 1) tick(config.burrowDuration);
    if (transitions >= 2) tick(config.undergroundDuration);
    snakeStats.setInvulnerable(false);

    snakeStats.takeDamage(1, player);

    assertLocked();
  }

  @Test
  void stageTwoSpitWindupVolleysAndRecoveryAreOutsideTheWindow() {
    reachExposed(true);
    tick(config.exposedDuration);
    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    snakeStats.takeDamage(1, player); // No poison has been fired yet.
    tick(config.spitWindupDuration);
    snakeStats.takeDamage(1, player);
    for (int i = 1; i < config.poisonVolleyCount; i++) tick(config.spitVolleyInterval);
    snakeStats.takeDamage(1, player);
    assertLocked();

    tick(config.spitRecoveryDuration);
    reachExposed(true);
    snakeStats.takeDamage(1, player);
    assertUnlockedOnce();
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void lethalExposedHitSurvivesDeathAndSynchronousDisposal(
      boolean stageTwo, boolean disposeOnDeath) {
    if (disposeOnDeath) {
      snake.dispose();
      createSnake(true);
    }
    reachExposed(stageTwo);

    snakeStats.takeDamage(1000, player);

    assertTrue(snakeStats.isDead());
    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    assertUnlockedOnce();
  }

  @ParameterizedTest
  @ValueSource(strings = {"zero", "invulnerable", "multiplier", "floor"})
  void blockedAttemptsDoNotLeaveAQualifyingHitBehind(String blocker) {
    reachExposed(false);
    switch (blocker) {
      case "invulnerable" -> snakeStats.setInvulnerable(true);
      case "multiplier" -> snakeStats.setIncomingDamageMultiplier(0f);
      case "floor" -> snakeStats.setMinimumHealth(snakeStats.getHealth());
      default -> {
        // The zero-damage case needs no blocking effect.
      }
    }
    snakeStats.takeDamage("zero".equals(blocker) ? 0 : 10, player);
    assertLocked();
    snakeStats.setInvulnerable(false);
    snakeStats.setIncomingDamageMultiplier(1f);
    snakeStats.setMinimumHealth(0);
    snakeStats.takeDamage(10); // Unattributed status-effect damage is not the blocked attack.
    assertLocked();

    snakeStats.takeDamage(1, player);
    assertUnlockedOnce();
  }

  @Test
  void oneLethalHitCanAwardBothChallengesWhenTheFirstNotificationDisposesSnake() {
    AchievementConfig noShieldDefinition = new AchievementConfig();
    noShieldDefinition.type = "snake";
    noShieldDefinition.name = "What are those green things?";
    noShieldDefinition.bossAchievementId = SnakeAchievementComponent.NO_SHIELD_PICKUP;
    Achievement noShield = AchievementsFactory.build(noShieldDefinition);
    achievements.register(noShield);
    achievements.getEvents().<String>addListener("achievementUnlocked", name -> snake.dispose());
    reachExposed(false);

    snakeStats.takeDamage(1000, player);

    assertTrue(noShield.isUnlocked());
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(noShieldDefinition.name, NAME), unlockedNames);
  }

  @Test
  void healthSettersUnattributedDamageAndEnemyProjectilesDoNotQualify() {
    reachExposed(false);
    snakeStats.setHealth(140);
    snakeStats.takeDamage(1);
    snakeStats.takeDamage(1, new Entity());
    Entity hostile =
        new Entity()
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    snakeStats.takeDamage(1, hostile);

    assertLocked();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void realPlayerLightningCountsIncludingLethalDamage(boolean lethal) {
    reachExposed(true);
    castLightning(player, lethal ? 1000 : 1);

    assertUnlockedOnce();
  }

  @Test
  void lightningDuringSpittingAndOtherCastersDoNotQualify() {
    reachExposed(true);
    castLightning(new Entity(), 1);
    assertLocked();
    tick(config.exposedDuration);

    castLightning(player, 1);

    assertLocked();
  }

  @Test
  void blockedLightningDoesNotMisattributeLaterStatusDamage() {
    reachExposed(false);
    snakeStats.setInvulnerable(true);
    castLightning(player, 1);
    snakeStats.setInvulnerable(false);

    snakeStats.takeDamage(1);

    assertLocked();
  }

  @Test
  void disposalWhileAliveCancelsTheAttempt() {
    reachExposed(false);
    snake.dispose();

    snakeStats.takeDamage(1, player);

    assertLocked();
  }

  @Test
  void playerDeathPermanentlyCancelsTheAttemptEvenAfterRevival() {
    reachExposed(false);
    playerStats.setHealth(0);
    playerStats.setHealth(100);

    snakeStats.takeDamage(1, player);

    assertLocked();
  }

  @Test
  void playerDeathDuringTheDamageCallbackCancelsThePendingHit() {
    reachExposed(false);
    snake.getEvents().addListener("updateHealth", (Integer hp) -> playerStats.setHealth(0));

    snakeStats.takeDamage(1, player);

    assertLocked();
  }

  @Test
  void repeatedHitsReportOnlyOnce() {
    reachExposed(false);

    snakeStats.takeDamage(1, player);
    snakeStats.takeDamage(1, player);

    assertUnlockedOnce();
  }

  @Test
  void restoredAchievementDoesNotNotifyAgain() {
    achievements.restoreState(List.of(NAME), Map.of());
    reachExposed(false);

    snakeStats.takeDamage(1, player);

    assertTrue(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  private void createSnake(boolean disposeOnDeath) {
    config = new SnakeMiniBossConfig();
    snakeStats = new CombatStatsComponent(150, 10);
    burrow = new SnakeBurrowComponent(player, config);
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(burrow)
            .addComponent(mock(SnakeShieldPickupComponent.class))
            .addComponent(mock(SnakePoisonVolleyComponent.class))
            .addComponent(new SnakeAchievementComponent(player));
    if (disposeOnDeath) snake.getEvents().addListener("entityDied", snake::dispose);
    snake.create();
    tick(0f);
  }

  private void reachExposed(boolean stageTwo) {
    if (stageTwo) snakeStats.setHealth(75);
    tick(config.burrowDuration);
    tick(config.undergroundDuration);
    tick(config.warningDuration);
    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertEquals(stageTwo, burrow.isStageTwo());
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    burrow.update();
  }

  private void castLightning(Entity caster, int damage) {
    EnemyTargetingStrategy strategy = mock(EnemyTargetingStrategy.class);
    when(strategy.selectTargets(caster)).thenReturn(Array.with(snake));
    LightningSpellComponent lightning = new LightningSpellComponent(0f, damage, 100L, strategy);
    caster.addComponent(lightning);
    caster.create();
    assertTrue(lightning.cast());
  }

  private void assertLocked() {
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  private void assertUnlockedOnce() {
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(NAME), unlockedNames);
    verify(achievements, times(1))
        .update(
            argThat(ctx -> SnakeAchievementComponent.WHACK_A_MOLE.equals(ctx.bossAchievementId)));
  }
}
