package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.List;
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
    return createDrop(ItemDropSpec.single(itemType), position);
  }

  /**
   * Creates a caller-selected item and quantity at a world position.
   *
   * <p>Item code does not choose the drop or gold amount. The returned entity is unregistered so
   * the requesting Room or Enemy feature retains lifecycle ownership.
   */
  public static Entity createDrop(ItemDropSpec dropSpec, Vector2 position) {
    Objects.requireNonNull(dropSpec, "dropSpec cannot be null");
    Objects.requireNonNull(position, "position cannot be null");

    ItemType itemType = dropSpec.itemType();
    Entity item =
        switch (itemType) {
          case STRENGTH_CHARM ->
              createWorldItem(
                  new ItemComponent(new Charm(itemType.getDisplayName()), dropSpec.quantity()),
                  itemType.getTexturePath());
          case HEALTH_POTION, SHIELD, SPEED_POTION, STRENGTH_POTION, GOLD_COIN ->
              createTypedItem(dropSpec);
        };
    item.setPosition(position);
    return item;
  }

  /**
   * Creates every caller-selected drop at the supplied origin.
   *
   * <p>The list may contain multiple or repeated consumables. Callers may reposition the returned
   * entities before registering them when a spread-out drop presentation is desired.
   */
  public static List<Entity> createDrops(List<ItemDropSpec> dropSpecs, Vector2 position) {
    Objects.requireNonNull(dropSpecs, "dropSpecs cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
    return dropSpecs.stream().map(dropSpec -> createDrop(dropSpec, position)).toList();
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
    return createTypedItem(ItemDropSpec.single(ItemType.HEALTH_POTION));
  }

  public static Entity createShield() {
    return createTypedItem(ItemDropSpec.single(ItemType.SHIELD));
  }

  public static Entity createSpeedPotion() {
    return createTypedItem(ItemDropSpec.single(ItemType.SPEED_POTION));
  }

  public static Entity createStrengthPotion() {
    return createTypedItem(ItemDropSpec.single(ItemType.STRENGTH_POTION));
  }

  public static Entity createGoldCoin() {
    return createTypedItem(ItemDropSpec.single(ItemType.GOLD_COIN));
  }

  private static Entity createTypedItem(ItemDropSpec dropSpec) {
    return createWorldItem(new ItemComponent(dropSpec), dropSpec.itemType().getTexturePath());
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
