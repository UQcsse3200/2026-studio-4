package com.csse3200.game.entities.factories;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionIndicatorComponent;
import com.csse3200.game.components.tutorial.DamagePopupDisplay;
import com.csse3200.game.components.tutorial.FloatingIconComponent;
import com.csse3200.game.components.tutorial.TrainingDummyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

/** Builds the props of the training grounds: dummies, targets, signs, supply stands, cannons. */
public final class TutorialPropFactory {
  public static final String DUMMY_TEXTURE = "images/tutorial/training_dummy.png";
  public static final String TARGET_TEXTURE = "images/tutorial/archery_target.png";
  public static final String SIGN_TEXTURE = "images/tutorial/sign.png";
  public static final String PEDESTAL_TEXTURE = "images/tutorial/pedestal.png";
  public static final String CANNON_TEXTURE = "images/tutorial/dodge_cannon.png";
  public static final String ORB_TEXTURE = "images/tutorial/energy_orb.png";

  /**
   * Item art is far larger than it is ever drawn here, which shrinks it to a few scattered pixels,
   * so each lendable item has a small ready-made icon named after its id.
   */
  private static final String ICON_PATH = "images/tutorial/icons/%s.png";

  private static final String[] ICON_ITEM_IDS = {
    ItemIds.HEALTH_POTION,
    ItemIds.SHIELD,
    ItemIds.SPEED_POTION,
    ItemIds.STRENGTH_POTION,
    ItemIds.FREEZE_BOMB,
    ItemIds.BURN_VIAL,
    ItemIds.MAGNET_POTION
  };

  private static final String[] TEXTURES =
      Stream.concat(
              Stream.of(
                  DUMMY_TEXTURE,
                  TARGET_TEXTURE,
                  SIGN_TEXTURE,
                  PEDESTAL_TEXTURE,
                  CANNON_TEXTURE,
                  ORB_TEXTURE),
              Arrays.stream(ICON_ITEM_IDS).map(TutorialPropFactory::iconPath))
          .toArray(String[]::new);

  /** Far more than any hit can do; a target is topped up after each hit regardless. */
  private static final int TARGET_HEALTH = 9999;

  /** Lifts a stand's interaction marker clear of the item floating over it. */
  private static final float STAND_MARKER_LIFT = 0.55f;

  /**
   * @return every texture the props need, for the room's asset loader
   */
  public static String[] textures() {
    return TEXTURES.clone();
  }

  private TutorialPropFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** A straw dummy that soaks up melee hits. */
  public static Entity createTrainingDummy(String label) {
    return createTarget(DUMMY_TEXTURE, label, 1f, 1.5f);
  }

  /** An archery target that soaks up arrows. */
  public static Entity createArcheryTarget(String label) {
    return createTarget(TARGET_TEXTURE, label, 1f, 1.375f);
  }

  /** A notice board. The manager adds the interaction that reads it. */
  public static Entity createSign() {
    return createSolidProp(SIGN_TEXTURE, 1f, 1f, 0.5f, 0.3f, 0f);
  }

  /**
   * @param itemId a consumable from ItemIds with a stand icon
   * @return asset path of the icon shown above that item's stand
   */
  public static String iconPath(String itemId) {
    return String.format(ICON_PATH, itemId.toLowerCase(Locale.ROOT));
  }

  /** A pedestal showing a floating item. The manager adds the interaction that lends it. */
  public static Entity createSupplyStand(String itemId) {
    if (!Arrays.asList(TEXTURES).contains(iconPath(itemId))) {
      throw new IllegalArgumentException("No stand icon for item: " + itemId);
    }
    Entity stand = createSolidProp(PEDESTAL_TEXTURE, 1f, 0.75f, 0.8f, 0.5f, STAND_MARKER_LIFT);
    stand.addComponent(new FloatingIconComponent(iconPath(itemId)));
    return stand;
  }

  /** A cannon that is placed at the top of a lane and fires downwards. */
  public static Entity createDodgeCannon() {
    return createSolidProp(CANNON_TEXTURE, 1f, 1f, 0.8f, 0.5f, 0f);
  }

  /** A harmless orb. It has no physics: the cannon moves it and checks how close it gets. */
  public static Entity createOrb() {
    Entity orb = new Entity().addComponent(new TextureRenderComponent(ORB_TEXTURE));
    orb.setScale(0.4f, 0.4f);
    return orb;
  }

  /**
   * A hittable target. Its hitbox is on the NPC layer like an enemy's, so weapons, spells and
   * status effects all treat it as an enemy, but {@link TrainingDummyComponent} heals it back to
   * full after every hit. It must be a dynamic body for weapon hitboxes to register on it.
   */
  private static Entity createTarget(String texture, String label, float width, float height) {
    Entity target =
        new Entity()
            .addComponent(new TextureRenderComponent(texture))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setDensity(100f))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new CombatStatsComponent(TARGET_HEALTH, 0))
            .addComponent(new TrainingDummyComponent(label))
            .addComponent(new DamagePopupDisplay());
    target.setScale(width, height);
    PhysicsUtils.setScaledCollider(target, 0.5f, 0.2f);
    return target;
  }

  /** A static prop the player walks around, with an indicator ready for when it can be used. */
  private static Entity createSolidProp(
      String texture,
      float width,
      float height,
      float colliderWidth,
      float colliderHeight,
      float markerLift) {
    Entity prop =
        new Entity()
            .addComponent(new TextureRenderComponent(texture))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE))
            .addComponent(new NpcInteractionIndicatorComponent(markerLift));
    prop.setScale(width, height);
    PhysicsUtils.setScaledCollider(prop, colliderWidth, colliderHeight);
    return prop;
  }
}
