package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossTornadoIntegrationTest {
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossPhaseControllerComponent phases;
  private Entity player;
  private GameTime time;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    ServiceLocator.registerRenderService(mock(RenderService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerActions());
    player.create();
    config = new FinalBossStageThreeConfig();
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    // The component also updates during earlier stages in the real factory.
    tick(0.1f);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    tick(0.1f);
    boss.getComponent(CombatStatsComponent.class).setHealth(20);
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  @Test
  void theFirstFourStatuesEachLeaveOneTornadoAndDuplicateHitsDoNotCreateMore() {
    for (int i = 0; i < 4; i++) {
      breakStatue(i);
      assertEquals(i + 1, stage.tornadoes.activeCount());
      stage.hitStatue(stage.statues.get(i));
      assertEquals(i + 1, stage.tornadoes.activeCount());
    }
    assertEquals(1, stage.getRemainingStatues());
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  @Test
  void theFinalStatueDissolvesAllFourAtTheirOwnLocationsWithoutAFifthSpawn() {
    for (int i = 0; i < 4; i++) breakStatue(i);
    List<Vector2> positions = stage.tornadoes.items.stream().map(t -> t.position.cpy()).toList();
    int burstsBefore = stage.bursts.size();
    breakStatue(4);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertEquals(0, stage.tornadoes.activeCount());
    assertEquals(4, stage.tornadoes.items.size());
    assertEquals(burstsBefore + 5, stage.bursts.size());
    for (int i = 0; i < 4; i++) {
      assertTrue(stage.tornadoes.items.get(i).dissolving);
      Vector2 centre = positions.get(i).cpy().add(0f, FinalBossTornadoController.HEIGHT * 0.5f);
      assertTrue(stage.bursts.stream().anyMatch(b -> b.grey && b.position.epsilonEquals(centre)));
    }
    tick(0.3f);
    for (int i = 0; i < 4; i++)
      assertEquals(positions.get(i), stage.tornadoes.items.get(i).position);
    tick(0.31f);
    assertTrue(stage.tornadoes.items.isEmpty());
  }

  @Test
  void playerDefeatAndBossDisposalClearTornadoes() {
    breakStatue(0);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(0.01f);
    assertTrue(stage.tornadoes.items.isEmpty());
    stage.dispose();
    assertTrue(stage.tornadoes.items.isEmpty());
  }

  @Test
  void leavingTheStageClearsTornadoesImmediatelyOnUpdate() {
    breakStatue(0);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    tick(0.01f);
    assertTrue(stage.tornadoes.items.isEmpty());
  }

  private void breakStatue(int index) {
    for (int hit = 0; hit < config.statueHits; hit++) stage.hitStatue(stage.statues.get(index));
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }
}
