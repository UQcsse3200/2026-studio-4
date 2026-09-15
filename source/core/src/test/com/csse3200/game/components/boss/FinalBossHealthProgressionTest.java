package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossHealthProgressionTest {
  @Test
  void shouldPreserveSixtyPercentDuringStageThreeTransitionAfterOversizedHit() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    FinalBossPhaseControllerComponent phases = new FinalBossPhaseControllerComponent();
    FinalBossStageThreeComponent stageThree =
        new FinalBossStageThreeComponent(player, Entity::create, new FinalBossStageThreeConfig());
    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(1000, 0))
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(phases)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(new FinalBossStageTwoComponent(new FinalBossStageTwoConfig()))
            .addComponent(stageThree);
    boss.create();
    AtomicInteger deaths = new AtomicInteger();
    boss.getEvents().addListener("entityDied", deaths::incrementAndGet);
    CombatStatsComponent stats = boss.getComponent(CombatStatsComponent.class);

    // Stage 1 hands over at 80%; the first transition must preserve that health.
    stats.setHealth(800);
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    stats.takeDamage(10000, player);
    assertEquals(800, stats.getHealth());
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertEquals(600, stats.getMinimumHealth());
    assertFalse(stats.isInvulnerable());

    stats.takeDamage(199, player);
    assertEquals(601, stats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    stats.takeDamage(10000, player);
    assertEquals(600, stats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertTrue(stats.isInvulnerable());

    when(time.getDeltaTime()).thenReturn(2.9f);
    phases.update();
    stats.takeDamage(10000, player);
    assertEquals(600, stats.getHealth());
    assertTrue(phases.isTransitioning());

    when(time.getDeltaTime()).thenReturn(0.11f);
    phases.update();
    assertEquals(600, stats.getHealth());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stageThree.getState());
    assertEquals(200, stats.getMinimumHealth());
    assertFalse(stats.isInvulnerable());

    // The next attack belongs to Stage 3 and must stop at its separate 20% floor.
    stats.takeDamage(10000, player);
    assertEquals(200, stats.getHealth());
    assertEquals(FinalBossStageThreeState.CHARGING, stageThree.getState());
    assertTrue(stats.isInvulnerable());
    assertEquals(0, deaths.get());
  }
}
