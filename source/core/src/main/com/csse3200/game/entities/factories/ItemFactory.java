package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.Objects;

/** Factory for creating item entities. */
public final class ItemFactory {
  /**
   * Creates the requested item at a world position.
   *
   * <p>The returned entity is not registered; the requesting room owns that responsibility.
   *
   * @param itemType type of item to create
   * @param position world position assigned to the item entity
   * @return a non-null, positioned, unregistered item entity for the room to spawn
   */
  public static Entity createDrop(ItemType itemType, Vector2 position) {
    Objects.requireNonNull(itemType, "itemType cannot be null");
    Objects.requireNonNull(position, "position cannot be null");

    Entity item =
        switch (itemType) {
          case STRENGTH_CHARM -> createStrengthCharm();
          case HEALTH_POTION -> createHealthPotion();
          case SHIELD -> createShield();
          case SPEED_POTION -> createSpeedPotion();
          case STRENGTH_POTION -> createStrengthPotion();
          case GOLD_COIN -> createGoldCoin();
        };
    item.setPosition(position);
    return item;
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createStrengthCharm() {
    ItemType type = ItemType.STRENGTH_CHARM;
    return createWorldItem(
        new ItemComponent(new Charm(type.getDisplayName())), type.getTexturePath());
  }

  public static Entity createHealthPotion() {
    return createTypedItem(ItemType.HEALTH_POTION);
  }

  public static Entity createShield() {
    return createTypedItem(ItemType.SHIELD);
  }

  public static Entity createSpeedPotion() {
    return createTypedItem(ItemType.SPEED_POTION);
  }

  public static Entity createStrengthPotion() {
    return createTypedItem(ItemType.STRENGTH_POTION);
  }

  public static Entity createGoldCoin() {
    return createTypedItem(ItemType.GOLD_COIN);
  }

  private static Entity createTypedItem(ItemType itemType) {
    return createWorldItem(new ItemComponent(itemType), itemType.getTexturePath());
  }

  private static Entity createWorldItem(ItemComponent itemComponent, String texturePath) {
    Entity item =
        new Entity()
            .addComponent(new TextureRenderComponent(texturePath))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(itemComponent);
    item.getComponent(TextureRenderComponent.class).scaleEntity();
    return item;
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
