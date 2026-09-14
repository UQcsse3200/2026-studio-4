package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.TypedItem;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.List;
import java.util.Objects;

/** Factory for creating item entities. */
public final class ItemFactory {

  /** Creates the default Strength Charm drop at a world position. */
  public static Entity createDrop(Vector2 position) {
    return createDrop(ItemType.STRENGTH_CHARM, position);
  }

  /** Creates a single caller-selected item at a world position. */
  public static Entity createDrop(ItemType itemType, Vector2 position) {
    return createDrop(ItemDropSpec.single(itemType), position);
  }

  /** Creates a caller-selected item and quantity at a world position. */
  public static Entity createDrop(ItemDropSpec dropSpec, Vector2 position) {
    Objects.requireNonNull(dropSpec, "dropSpec cannot be null");
    Objects.requireNonNull(position, "position cannot be null");

    Entity item =
        dropSpec.itemType() == ItemType.STRENGTH_CHARM
            ? createItem(new StrengthCharm(), dropSpec.quantity())
            : createItem(new TypedItem(dropSpec));
    item.setPosition(position);
    return item;
  }

  /** Creates every caller-selected drop at the supplied origin. */
  public static List<Entity> createDrops(List<ItemDropSpec> dropSpecs, Vector2 position) {
    Objects.requireNonNull(dropSpecs, "dropSpecs cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
    return dropSpecs.stream().map(dropSpec -> createDrop(dropSpec, position)).toList();
  }

  /** Creates an item entity using the shared Item abstraction introduced on main. */
  public static Entity createItem(Item item) {
    return createItem(item, item instanceof TypedItem typedItem ? typedItem.getQuantity() : 1);
  }

  private static Entity createItem(Item item, int quantity) {
    Objects.requireNonNull(item, "item cannot be null");
    Entity itemEntity =
        new Entity()
            .addComponent(new TextureRenderComponent(item.getTexture()))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item, quantity));
    itemEntity.getComponent(TextureRenderComponent.class).scaleEntity();
    return itemEntity;
  }

  /** Creates an unregistered Strength Charm entity. */
  public static Entity createStrengthCharm() {
    return createItem(new StrengthCharm());
  }

  public static Entity createHealthPotion() {
    return createItem(new TypedItem(ItemDropSpec.single(ItemType.HEALTH_POTION)));
  }

  public static Entity createShield() {
    return createItem(new TypedItem(ItemDropSpec.single(ItemType.SHIELD)));
  }

  public static Entity createSpeedPotion() {
    return createItem(new TypedItem(ItemDropSpec.single(ItemType.SPEED_POTION)));
  }

  public static Entity createStrengthPotion() {
    return createItem(new TypedItem(ItemDropSpec.single(ItemType.STRENGTH_POTION)));
  }

  public static Entity createGoldCoin() {
    return createItem(new TypedItem(ItemDropSpec.single(ItemType.GOLD_COIN)));
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
