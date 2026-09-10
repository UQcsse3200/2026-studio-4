package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.FinalBossDamageControllerComponent;
import com.csse3200.game.components.boss.FinalBossHealthBarDisplay;
import com.csse3200.game.components.boss.FinalBossPhaseControllerComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Creates the Final Boss and its Stage 1 summons. */
public final class FinalBossFactory {
  private static final String PLACEHOLDER_SKIN = "images/bombEnemy.atlas";

  /**
   * Creates the basic Final Boss entity.
   *
   * <p>The target and summon spawner will be used when Stage 1 behaviour is added.
   */
  public static Entity createFinalBoss(Entity target, Consumer<Entity> summonSpawner) {
    FinalBossStageOneConfig config = new FinalBossStageOneConfig();
    config.validate();
    if (target == null || summonSpawner == null) {
      throw new IllegalArgumentException("Final Boss factory arguments must not be null");
    }

    TextureAtlas atlas =
        ServiceLocator.getResourceService().getAsset(PLACEHOLDER_SKIN, TextureAtlas.class);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("default", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("dieAnimation", 0.1f, Animation.PlayMode.NORMAL);

    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.bossHealth, 0))
            .addComponent(animator)
            .addComponent(new EnemyAnimationController())
            .addComponent(new FinalBossPhaseControllerComponent())
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(new FinalBossHealthBarDisplay());

    animator.scaleEntity();
    boss.scaleWidth(2f);
    animator.startAnimation("default");

    return boss;
  }

  private FinalBossFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
