package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.*;
import com.csse3200.game.components.miniboss.cerberus.CerberusAnimationController;
import com.csse3200.game.components.miniboss.cerberus.CerberusAttackCoordinator;
import com.csse3200.game.components.miniboss.cerberus.CerberusBiteComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusDeathComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusEnrageVisualComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMistComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMovementComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusPhaseComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusProjectileComponent;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.*;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Factory for creating Cerberus and its individual heads. */
public class CerberusFactory {
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  private CerberusFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /**
   * Creates the shared physics components for a Cerberus part.
   *
   * <p>Built from the shared NPC base rather than an identical copy of it, so a Cerberus part is an
   * enemy in every way the rest of the game recognises. Its own base had drifted by omitting the
   * status effects controller, which left spells and other effects silently doing nothing to it.
   *
   * @return base Cerberus entity
   */
  private static Entity createBaseCerberusPart() {
    return NPCFactory.createBaseNPC();
  }

  /**
   * Creates the base entity for Cerberus' main body / middle head.
   *
   * @return Cerberus mini-boss entity
   */
  private static Entity createBaseCerberusMiniBoss() {
    return createBaseCerberusPart();
  }

  /**
   * Creates one of Cerberus' side heads.
   *
   * @param mainHead middle head / main body entity
   * @param offset positional offset relative to the main body
   * @param health independent health of this head
   * @param skin animation atlas path
   * @return side head entity
   */
  private static Entity createCerberusSideHead(
      Entity mainHead, Vector2 offset, int health, String skin) {

    Entity sideHead = createBaseCerberusPart().addComponent(new EnemyDeathComponent(false));
    sideHead.getComponent(ColliderComponent.class).setSensor(true);
    sideHead.getComponent(HitboxComponent.class).setAsBox(new Vector2(0.35f, 0.35f));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("lunge", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("idle", 0.1f, Animation.PlayMode.LOOP);

    sideHead
        .addComponent(new CombatStatsComponent(health, 10))
        .addComponent(new HeadAttachmentComponent(mainHead, offset))
        .addComponent(new EnemyStatDisplay(1.5f))
        .addComponent(animator);

    animator.startAnimation("idle");

    return sideHead;
  }

  /**
   * Creates Cerberus, consisting of the middle head/body and two independent side heads.
   *
   * @param anchorPoint center point used by the chain restriction
   * @param sideHeadSpawner callback used to spawn and track the side heads
   * @param skin animation atlas path
   * @return Cerberus main entity
   */
  public static Entity createCerberus(
      Vector2 anchorPoint, Consumer<Entity> sideHeadSpawner, String skin) {
    return createCerberus(null, anchorPoint, sideHeadSpawner, skin);
  }

  public static Entity createCerberus(
      Entity target, Vector2 anchorPoint, Consumer<Entity> sideHeadSpawner, String skin) {
    Entity mainHead = createBaseCerberusMiniBoss();
    mainHead.getComponent(HitboxComponent.class).setAsBox(new Vector2(0.4f, 0.4f));

    BaseEntityConfig conf = configs.cerberus;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idle", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("lunge", 0.1f, Animation.PlayMode.NORMAL);

    mainHead
        .addComponent(new CombatStatsComponent(conf.health, conf.baseAttack))
        .addComponent(animator)
        .addComponent(new ChainRestrictionComponent(anchorPoint, 3f))
        .addComponent(new CerberusAnimationController())
        .addComponent(new EnemyStatDisplay(2.0f));

    Entity leftHead =
        createCerberusSideHead(mainHead, new Vector2(-0.55f, 0.15f), conf.health / 2, skin);

    Entity rightHead =
        createCerberusSideHead(mainHead, new Vector2(0.55f, 0.15f), conf.health / 2, skin);

    CerberusPhaseComponent phase = new CerberusPhaseComponent(leftHead, rightHead);

    mainHead
        .addComponent(new CerberusDeathComponent(leftHead, rightHead))
        .addComponent(phase)
        .addComponent(new CerberusEnrageVisualComponent(phase));

    leftHead.addComponent(new CerberusEnrageVisualComponent(phase));
    rightHead.addComponent(new CerberusEnrageVisualComponent(phase));

    if (target != null) {
      mainHead
          .addComponent(new CerberusMovementComponent(target, anchorPoint, 3f))
          .addComponent(new CerberusBiteComponent(target, anchorPoint, 3f));

      rightHead.addComponent(
          new CerberusProjectileComponent(
              target,
              projectile -> mainHead.getEvents().trigger("cerberusProjectileSpawned", projectile)));

      leftHead.addComponent(new CerberusMistComponent(target));

      CerberusAttackCoordinator coordinator =
          new CerberusAttackCoordinator(
              leftHead, mainHead, rightHead, mainHead.getComponent(CerberusPhaseComponent.class));

      leftHead.getComponent(CerberusMistComponent.class).setAttackCoordinator(coordinator);
      mainHead.getComponent(CerberusBiteComponent.class).setAttackCoordinator(coordinator);
      rightHead.getComponent(CerberusProjectileComponent.class).setAttackCoordinator(coordinator);
    }

    sideHeadSpawner.accept(leftHead);
    sideHeadSpawner.accept(rightHead);

    animator.startAnimation("idle");

    return mainHead;
  }
}
