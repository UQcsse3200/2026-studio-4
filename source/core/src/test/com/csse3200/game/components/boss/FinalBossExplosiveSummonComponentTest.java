package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  private Entity createSummon(float warningDuration) {
    Entity summon =
        FinalBossFactory.createExplosiveSummon(
            player, config, config.waveOneSummonSpeed, warningDuration);

    summon.create();
    return summon;
  }
}
