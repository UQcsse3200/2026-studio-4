package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.weapons.ProjectileComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossExplosiveSummonComponentTest {
  private final AtomicReference<Float> deltaTime = new AtomicReference<>(0f);

  private Entity player;
  private FinalBossStageOneConfig config;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerPhysicsService(new PhysicsService());

    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenAnswer(invocation -> deltaTime.get());
    ServiceLocator.registerTimeSource(time);

    ResourceService resources = new ResourceService();
    resources.loadTextureAtlases(new String[] {FinalBossFactory.PLACEHOLDER_SKIN});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);

    player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    player.setPosition(0f, 0f);
    player.create();

    config = new FinalBossStageOneConfig();
  }

  @Test
  void shouldDamageOnlyInsideExplosionRadiusAndReportRemovalOnce() {
    Entity summon = createSummon(0f);
    summon.setPosition(0f, 0f);

    int[] removals = {0};
    summon.getEvents().addListener(FinalBossEvents.SUMMON_REMOVED, ignored -> removals[0]++);

    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);

    explosive.detonate();
    explosive.detonate();

    assertEquals(90, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, removals[0]);

    Entity distantSummon = createSummon(0f);
    distantSummon.setPosition(10f, 10f);

    distantSummon.getComponent(FinalBossExplosiveSummonComponent.class).detonate();

    assertEquals(90, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldDetonateImmediatelyWhenHitByRangedProjectile() {
    Entity summon = createSummon(config.waveOneWarningDuration);
    summon.setPosition(0f, 0f);

    Entity projectile = new Entity().addComponent(new ProjectileComponent(new Vector2(1f, 0f), 1f));

    summon.getEvents().trigger("hitReaction", projectile);

    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);

    assertTrue(explosive.hasDetonated());
    assertEquals(90, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldUseConfiguredWarningBeforeProximityDetonation() {
    Entity summon = createSummon(1f);
    summon.setPosition(0f, 0f);

    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);

    deltaTime.set(0.5f);

    explosive.update();

    assertTrue(explosive.isWarningActive());
    assertFalse(explosive.hasDetonated());

    explosive.update();
    assertFalse(explosive.hasDetonated());

    explosive.update();
    assertTrue(explosive.hasDetonated());
  }

  @Test
  void shouldStopPursuitAndResumeAtPlayersNewPosition() {
    PlayerAbilitiesComponent abilities = mock(PlayerAbilitiesComponent.class);
    player = new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(abilities);
    player.create();
    PhysicsMovementComponent movement = mock(PhysicsMovementComponent.class);
    FinalBossExplosiveSummonComponent explosive =
        new FinalBossExplosiveSummonComponent(player, 2f, 1f, 2f, 10, 1f);
    new Entity()
        .addComponent(movement)
        .addComponent(new CombatStatsComponent(10, 0))
        .addComponent(explosive)
        .create();
    player.setPosition(5f, 0f);
    explosive.update();
    verify(movement).setMoving(true);
    clearInvocations(movement);

    when(abilities.isActive(Invisibility.class)).thenReturn(true);
    player.setPosition(0f, 0f);
    explosive.update();
    verify(movement).setMoving(false);
    verify(movement, never()).setTarget(any());
    assertFalse(explosive.isWarningActive());
    assertFalse(explosive.hasDetonated());
    clearInvocations(movement);

    when(abilities.isActive(Invisibility.class)).thenReturn(false);
    player.setPosition(4f, 0f);
    explosive.update();
    verify(movement).setMoving(true);
    verify(movement).setTarget(new Vector2(4f, 0f));
  }

  @Test
  void shouldPreventProximityWarningUntilInvisibilityExpires() {
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    player.create();
    assertTrue(abilities.tryActivate(Invisibility.class));
    Entity summon = createSummon(1f);
    summon.setPosition(0f, 0f);
    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);
    deltaTime.set(2f);
    explosive.update();
    explosive.update();
    assertFalse(explosive.isWarningActive());
    assertFalse(explosive.hasDetonated());

    when(ServiceLocator.getTimeSource().getTime()).thenReturn(Invisibility.DURATION_MS);
    explosive.update();
    assertTrue(explosive.isWarningActive());
    assertFalse(explosive.hasDetonated());
    explosive.update();
    assertTrue(explosive.hasDetonated());
    assertEquals(90, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldFinishArmedWarningWhileInvisibleWithoutDamagingPlayer() {
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    player.create();
    Entity summon = createSummon(1f);
    summon.setPosition(0f, 0f);
    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);
    int[] removals = {0};
    summon.getEvents().addListener(FinalBossEvents.SUMMON_REMOVED, ignored -> removals[0]++);
    explosive.update();
    assertTrue(explosive.isWarningActive());
    assertTrue(abilities.tryActivate(Invisibility.class));
    deltaTime.set(0.5f);
    explosive.update();
    assertFalse(explosive.hasDetonated());
    explosive.update();
    assertTrue(explosive.hasDetonated());
    assertFalse(explosive.isWarningActive());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    explosive.update();
    explosive.detonate();
    assertEquals(1, removals[0]);
  }

  @Test
  void shouldAllowProjectileTriggeredExplosionWhilePlayerIsInvisible() {
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    player.create();
    assertTrue(abilities.tryActivate(Invisibility.class));
    Entity summon = createSummon(1f);
    summon.setPosition(0f, 0f);
    Entity projectile = new Entity().addComponent(new ProjectileComponent(new Vector2(1f, 0f), 1f));
    summon.getEvents().trigger("hitReaction", projectile);
    assertTrue(summon.getComponent(FinalBossExplosiveSummonComponent.class).hasDetonated());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  private Entity createSummon(float warningDuration) {
    Entity summon =
        FinalBossFactory.createExplosiveSummon(
            player, config, config.waveOneSummonSpeed, warningDuration);

    summon.create();
    return summon;
  }
}
