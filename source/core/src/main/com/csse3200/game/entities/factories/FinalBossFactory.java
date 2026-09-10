package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.FinalBossDamageControllerComponent;
import com.csse3200.game.components.boss.FinalBossExplosiveSummonComponent;
import com.csse3200.game.components.boss.FinalBossHealthBarDisplay;
import com.csse3200.game.components.boss.FinalBossMovementComponent;
import com.csse3200.game.components.boss.FinalBossPhaseControllerComponent;
import com.csse3200.game.components.boss.FinalBossStageOneComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Creates the Final Boss and its Stage 1 summons. */
public final class FinalBossFactory {
  public static final String PLACEHOLDER_SKIN = "images/bombEnemy.atlas";
  private static final String DEFAULT_ANIMATION = "default";

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
    config.validate();

    AnimationRenderComponent animator = createAnimator(PLACEHOLDER_SKIN);

    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.bossHealth, 0))
            .addComponent(animator)
            .addComponent(new EnemyAnimationController())
            .addComponent(new FinalBossPhaseControllerComponent())
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(new FinalBossHealthBarDisplay())
            .addComponent(new FinalBossMovementComponent(target, config))
            .addComponent(new FinalBossStageOneComponent(target, summonSpawner, config));

    animator.scaleEntity();
    boss.scaleWidth(2f);
    animator.startAnimation(DEFAULT_ANIMATION);

    return boss;
  }

  /** Creates an unregistered explosive summon used during Final Boss Stage 1. */
  public static Entity createExplosiveSummon(
      Entity target, FinalBossStageOneConfig config, float movementSpeed, float warningDuration) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Summon target and config must not be null");
    }

    config.validate();

    AnimationRenderComponent animator = createAnimator(PLACEHOLDER_SKIN);

    Entity summon =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(config.summonHealth, 0))
            .addComponent(animator)
            .addComponent(new EnemyAnimationController())
            .addComponent(
                new FinalBossExplosiveSummonComponent(
                    target,
                    movementSpeed,
                    config.summonTriggerDistance,
                    config.summonExplosionRadius,
                    config.summonExplosionDamage,
                    warningDuration));

    animator.scaleEntity();
    summon.setScale(summon.getScale().scl(0.75f));
    animator.startAnimation(DEFAULT_ANIMATION);

    return summon;
  }

  /** Creates the animations shared by the temporary boss and summon sprites. */
  private static AnimationRenderComponent createAnimator(String skin) {
    TextureAtlas atlas = ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation(DEFAULT_ANIMATION, 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("chase", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("dieAnimation", 0.1f, Animation.PlayMode.NORMAL);

    return animator;
  }

  private FinalBossFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }
}
