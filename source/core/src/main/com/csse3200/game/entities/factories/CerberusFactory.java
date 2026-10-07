package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.*;
import com.csse3200.game.components.miniboss.cerberus.CerberusAttackCoordinator;
import com.csse3200.game.components.miniboss.cerberus.CerberusBiteComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusChainComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusChainRenderComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusDeathComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusEnrageVisualComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Action;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Part;
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
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Factory for creating Cerberus and its individual heads. */
public class CerberusFactory {
  public static final String CHAIN_ATLAS_PATH = "images/cerberus/cerberus-chain.atlas";

  private static final float VISUAL_SIZE = 2f;
  private static final float SIDE_HEAD_WIDTH = VISUAL_SIZE * 0.28f;
  private static final float SIDE_HEAD_HEIGHT = VISUAL_SIZE * 0.35f;
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
   * Creates a side head with independent combat and collision handling.
   *
   * @param mainHead middle head / main body entity
   * @param offset positional offset relative to the main body
   * @param health independent health of this head
   * @return side head entity
   */
  private static Entity createCerberusSideHead(Entity mainHead, Vector2 offset, int health) {

    Entity sideHead = createBaseCerberusPart().addComponent(new EnemyDeathComponent(false));

    sideHead.setScale(SIDE_HEAD_WIDTH, SIDE_HEAD_HEIGHT);

    Vector2 headSize = new Vector2(SIDE_HEAD_WIDTH, SIDE_HEAD_HEIGHT);
    Vector2 localCentre = new Vector2(SIDE_HEAD_WIDTH / 2f, SIDE_HEAD_HEIGHT / 2f);

    sideHead.getComponent(ColliderComponent.class).setAsBox(headSize, localCentre).setSensor(true);

    sideHead.getComponent(HitboxComponent.class).setAsBox(headSize, localCentre);

    sideHead
        .addComponent(new CombatStatsComponent(health, 10))
        .addComponent(new HeadAttachmentComponent(mainHead, offset))
        .addComponent(new EnemyStatDisplay(1.5f));

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
    return createCerberus(target, anchorPoint, null, sideHeadSpawner, skin);
  }

  /**
   * Creates Cerberus with an optional visible chain.
   *
   * @param target player targeted by attacks
   * @param anchorPoint centre of the existing movement restriction
   * @param wallAnchor fixed chain attachment, or null to omit the visible chain
   * @param sideHeadSpawner callback for spawning the side heads
   * @param skin Cerberus animation atlas path
   * @return Cerberus main entity
   */
  public static Entity createCerberus(
      Entity target,
      Vector2 anchorPoint,
      Vector2 wallAnchor,
      Consumer<Entity> sideHeadSpawner,
      String skin) {
    Entity mainHead = createBaseCerberusPart();
    mainHead.setScale(VISUAL_SIZE, VISUAL_SIZE);
    mainHead
        .getComponent(ColliderComponent.class)
        .setAsBox(
            new Vector2(VISUAL_SIZE * 0.60f, VISUAL_SIZE * 0.25f),
            new Vector2(VISUAL_SIZE * 0.50f, VISUAL_SIZE * 0.20f));
    mainHead
        .getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(VISUAL_SIZE * 0.26f, VISUAL_SIZE * 0.32f),
            new Vector2(VISUAL_SIZE * 0.50f, VISUAL_SIZE * 0.73f));

    BaseEntityConfig conf = configs.cerberus;

    CerberusLayeredRenderComponent renderer =
        new CerberusLayeredRenderComponent(
            ServiceLocator.getResourceService().getAsset(skin, TextureAtlas.class));

    mainHead
        .addComponent(new CombatStatsComponent(conf.health, conf.baseAttack))
        .addComponent(renderer)
        .addComponent(new ChainRestrictionComponent(anchorPoint, 3f))
        .addComponent(new EnemyStatDisplay(2.0f));

    Vector2 leftOffset =
        new Vector2(
            VISUAL_SIZE * 0.28125f - SIDE_HEAD_WIDTH / 2f,
            VISUAL_SIZE * 0.65f - SIDE_HEAD_HEIGHT / 2f);

    Vector2 rightOffset =
        new Vector2(
            VISUAL_SIZE * 0.71875f - SIDE_HEAD_WIDTH / 2f,
            VISUAL_SIZE * 0.65f - SIDE_HEAD_HEIGHT / 2f);

    Entity leftHead = createCerberusSideHead(mainHead, leftOffset, conf.health / 2);

    Entity rightHead = createCerberusSideHead(mainHead, rightOffset, conf.health / 2);

    leftHead.setPosition(mainHead.getPosition().cpy().add(leftOffset));
    rightHead.setPosition(mainHead.getPosition().cpy().add(rightOffset));
    renderer.bindHeads(leftHead, mainHead, rightHead);

    mainHead
        .getEvents()
        .addListener(
            "attackStart",
            () -> {
              renderer.play(Part.BODY, Action.LUNGE);
              renderer.play(Part.MIDDLE, Action.LUNGE);
            });

    mainHead
        .getEvents()
        .addListener(
            "default",
            () -> {
              renderer.play(Part.BODY, Action.IDLE);
              renderer.play(Part.MIDDLE, Action.IDLE);
            });

    mainHead
        .getEvents()
        .addListener(
            "entityDied",
            () -> {
              renderer.setPartVisible(Part.MIDDLE, false);
              renderer.play(Part.BODY, Action.IDLE);
            });

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

    if (wallAnchor != null) {
      TextureAtlas chainAtlas =
          ServiceLocator.getResourceService().getAsset(CHAIN_ATLAS_PATH, TextureAtlas.class);

      // Account for the body's attachment offset and existing movement radius.
      Vector2 attachmentCentre = anchorPoint.cpy().add(VISUAL_SIZE * 0.5f, VISUAL_SIZE * 0.4f);
      float chainLength = wallAnchor.dst(attachmentCentre) + 3f + 0.1f;

      mainHead
          .addComponent(new CerberusChainComponent(wallAnchor))
          .addComponent(
              new CerberusChainRenderComponent(chainAtlas.findRegion("chain"), chainLength));
    }

    sideHeadSpawner.accept(leftHead);
    sideHeadSpawner.accept(rightHead);

    return mainHead;
  }
}
