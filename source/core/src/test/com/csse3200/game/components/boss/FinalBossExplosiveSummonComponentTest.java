package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.weapons.ProjectileComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

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
    resources.loadTextures(FinalBossVisualAssets.paths());
    resources.loadTextures(FinalBossStageThreeAssets.paths());
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
  void shouldShowImmediateBlastAndDisposeOnlyAfterPlayback() {
    EntityService service = spy(new EntityService());
    ServiceLocator.registerEntityService(service);
    Entity summon = createSummon(0f);
    FinalBossExplosiveSummonComponent explosive =
        summon.getComponent(FinalBossExplosiveSummonComponent.class);
    FinalBossSummonVisualComponent visual =
        summon.getComponent(FinalBossSummonVisualComponent.class);
    explosive.detonate();
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch).draw(frame.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    assertEquals(450, frame.getValue().getRegionX());
    deltaTime.set(0.15f);
    visual.update();
    verify(service, never()).scheduleDisposal(summon);
    deltaTime.set(0.18f);
    visual.update();
    visual.update();
    explosive.detonate();
    verify(service, times(1)).scheduleDisposal(summon);
    assertEquals(90, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldCreateAndRenderBossWithSelectedAssets() {
    Entity boss = FinalBossFactory.createFinalBoss(player, Entity::create);
    boss.create();
    FinalBossStageOneComponent stage = boss.getComponent(FinalBossStageOneComponent.class);
    assertEquals(FinalBossStageOneState.INTRO, stage.getState());
    boss.getComponent(FinalBossVisualComponent.class).render(mock(SpriteBatch.class));
    deltaTime.set(config.bossIntroDuration);
    stage.update();
    deltaTime.set(config.bossTransformDuration);
    stage.update();
    deltaTime.set(config.bossSummonCastDuration);
    stage.update();
    assertEquals(config.waveOneSummonCount, stage.getActiveSummonCount());
    boss.getComponent(FinalBossVisualComponent.class).render(mock(SpriteBatch.class));
  }

  private Entity createSummon(float warningDuration) {
    Entity summon =
        FinalBossFactory.createExplosiveSummon(
            player, config, config.waveOneSummonSpeed, warningDuration);

    summon.create();
    return summon;
  }
}
