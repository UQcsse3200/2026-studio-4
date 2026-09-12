package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.*;
import com.csse3200.game.components.npc.CerberusAnimationController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.*;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
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
   * Creates the base entity used by Cerberus parts.
   *
   * <p>Cerberus currently has no death animation, so EnemyDeathComponent is configured with false.
   *
   * @return base Cerberus entity
   */
  private static Entity createBaseCerberusPart() {
    Entity part =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new EnemyDeathComponent(false));

    PhysicsUtils.setScaledCollider(part, 0.9f, 0.4f);
    return part;
  }

  /**
   * Creates the base entity for Cerberus' main body / middle head.
   *
   * @return Cerberus mini-boss entity
   */
  private static Entity createBaseCerberusMiniBoss() {
    Entity miniBoss = createBaseCerberusPart();
    miniBoss.addComponent(new BossPhaseComponent());
    return miniBoss;
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

    Entity sideHead = createBaseCerberusPart();

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("lunge", 0.1f, Animation.PlayMode.NORMAL);

    sideHead
        .addComponent(new CombatStatsComponent(health, 10))
        .addComponent(new HeadAttachmentComponent(mainHead, offset))
        .addComponent(animator);

    animator.startAnimation("move");

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

    Entity mainHead = createBaseCerberusMiniBoss();

    BaseEntityConfig conf = configs.cerberus;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    animator.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idle", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("lunge", 0.1f, Animation.PlayMode.NORMAL);

    mainHead
        .addComponent(new CombatStatsComponent(conf.health, conf.baseAttack))
        .addComponent(animator)
        .addComponent(new ChainRestrictionComponent(anchorPoint, 15f))
        .addComponent(new CerberusAnimationController());

    Entity leftHead =
        createCerberusSideHead(mainHead, new Vector2(-0.55f, 0.25f), conf.health / 2, skin);

    Entity rightHead =
        createCerberusSideHead(mainHead, new Vector2(0.55f, 0.25f), conf.health / 2, skin);

    sideHeadSpawner.accept(leftHead);
    sideHeadSpawner.accept(rightHead);

    animator.startAnimation("move");

    return mainHead;
  }
}
