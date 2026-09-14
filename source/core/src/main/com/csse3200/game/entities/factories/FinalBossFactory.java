package com.csse3200.game.entities.factories;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.FinalBossDamageControllerComponent;
import com.csse3200.game.components.boss.FinalBossExplosiveSummonComponent;
import com.csse3200.game.components.boss.FinalBossMovementComponent;
import com.csse3200.game.components.boss.FinalBossPetrificationComponent;
import com.csse3200.game.components.boss.FinalBossPetrificationVisualComponent;
import com.csse3200.game.components.boss.FinalBossPetrificationWarningRenderComponent;
import com.csse3200.game.components.boss.FinalBossPhaseControllerComponent;
import com.csse3200.game.components.boss.FinalBossProximityDamageComponent;
import com.csse3200.game.components.boss.FinalBossStageOneComponent;
import com.csse3200.game.components.boss.FinalBossStageTwoComponent;
import com.csse3200.game.components.boss.FinalBossStageTwoContactDamageComponent;
import com.csse3200.game.components.boss.FinalBossSummonVisualComponent;
import com.csse3200.game.components.boss.FinalBossVisualComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.function.Consumer;

/** Creates the Final Boss and its Stage 1 summons. */
public final class FinalBossFactory {
  public static final String PLACEHOLDER_SKIN = "images/bombEnemy.atlas";

  /**
   * Creates the Final Boss with its shared phase framework and Stage 1 behaviour.
   *
   * @param target the player targeted by the boss and its summons
   * @param summonSpawner callback used to register spawned summons in the room
   * @return the unregistered Final Boss entity
   */
  public static Entity createFinalBoss(Entity target, Consumer<Entity> summonSpawner) {
    if (target == null || summonSpawner == null) {
      throw new IllegalArgumentException("Final Boss factory arguments must not be null");
    }

    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    FinalBossStageTwoConfig stageTwoConfig = new FinalBossStageTwoConfig();
    config.validate();
    stageTwoConfig.validate();

    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.bossHealth, 0))
            .addComponent(new FinalBossVisualComponent(target, config))
            .addComponent(new FinalBossPhaseControllerComponent())
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(new FinalBossMovementComponent(target, config, stageTwoConfig))
            .addComponent(new FinalBossStageOneComponent(target, summonSpawner, config))
            .addComponent(new FinalBossStageTwoComponent(stageTwoConfig))
            .addComponent(new FinalBossStageTwoContactDamageComponent(target, stageTwoConfig))
            .addComponent(new FinalBossProximityDamageComponent(target, config))
            .addComponent(new FinalBossPetrificationWarningRenderComponent())
            .addComponent(new FinalBossPetrificationComponent(target, config))
            .addComponent(new FinalBossPetrificationVisualComponent(target, config));

    boss.setScale(2f, 2f);

    return boss;
  }

  /** Creates an unregistered explosive summon used during Final Boss Stage 1. */
  public static Entity createExplosiveSummon(
      Entity target, FinalBossStageOneConfig config, float movementSpeed, float warningDuration) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Summon target and config must not be null");
    }

    config.validate();

    Entity summon =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.summonHealth, 0))
            .addComponent(new FinalBossSummonVisualComponent())
            .addComponent(
                new FinalBossExplosiveSummonComponent(
                    target,
                    movementSpeed,
                    config.summonTriggerDistance,
                    config.summonExplosionRadius,
                    config.summonExplosionDamage,
                    warningDuration));

    summon.setScale(0.75f, 0.75f);

    return summon;
  }

  private FinalBossFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
