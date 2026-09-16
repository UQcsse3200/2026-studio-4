package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Factory for creating item entities. */
public final class ItemFactory {

  /**
   * Enum used to determine random drops.
   *
   * <p>To added a new drop to the random pool, add a new enum type and pass in a new item to its
   * constructor function into the enum type's constructor
   */
  enum DropTypes {
    STRENGTH_CHARM(StrengthCharm::new),
    SPEED_CHARM(SpeedCharm::new),
    ATKSPD_CHARM(AttackSpeedCharm::new);

    // A functional interface is used so that new instances are created
    // on drop request
    private final Supplier<Item> itemSupplier;
    private static final DropTypes[] VALUES = values();

    DropTypes(Supplier<Item> item) {
      this.itemSupplier = item;
    }

    public Item getItemSupplier() {
      return itemSupplier.get();
    }

    /** selects a random item type to drop */
    public static DropTypes randomDrop() {
      int idx = ThreadLocalRandom.current().nextInt(VALUES.length);
      return VALUES[idx];
    }
  }

  public static Entity createRandomDrop(Vector2 position) {
    Objects.requireNonNull(position, "position cannot be null");

    Entity item = createItem(DropTypes.randomDrop().getItemSupplier());
    item.setPosition(position);
    return item;
  }

  /**
   * Creates the requested item at a world position.
   *
   * @param position world position assigned to the item entity
   * @return a non-null, positioned, unregistered item entity for the room to spawn
   */
  public static Entity createDrop(Vector2 position) {
    Objects.requireNonNull(position, "position cannot be null");
    Entity item = createItem(new StrengthCharm());
    item.setPosition(position);
    return item;
  }

  /** Creates an item entity to be spawned into the game. */
  public static Entity createItem(Item item) {
    Entity itemEntity =
        new Entity()
            .addComponent(new TextureRenderComponent(item.getTexture()))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item));

    itemEntity.getComponent(TextureRenderComponent.class).scaleEntity();
    return itemEntity;
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
    return createItem(new StrengthCharm());
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createMovementSpeedCharm() {
    return createItem(new SpeedCharm());
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createAttackSpeedCharm() {
    return createItem(new AttackSpeedCharm());
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
