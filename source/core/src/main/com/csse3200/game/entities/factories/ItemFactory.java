package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemSpinComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.TypedItem;
import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/** Factory for creating item entities. */
public final class ItemFactory {
  private static final String NULL_POSITION_MESSAGE = "position cannot be null";

  /**
   * Enum used to determine random drops.
   *
   * <p>To added a new drop to the random pool, add a new enum type and pass in a new item to its
   * constructor function into the enum type's constructor
   */
  enum DropTypes {
    STRENGTH_CHARM(StrengthCharm::new),
    SPEED_CHARM(SpeedCharm::new),
    ATKSPD_CHARM(AttackSpeedCharm::new),
    HEALTH_POTION(() -> new TypedItem(ItemDropSpec.single(ItemType.HEALTH_POTION))),
    SHIELD(() -> new TypedItem(ItemDropSpec.single(ItemType.SHIELD))),
    SPEED_POTION(() -> new TypedItem(ItemDropSpec.single(ItemType.SPEED_POTION))),
    STRENGTH_POTION(() -> new TypedItem(ItemDropSpec.single(ItemType.STRENGTH_POTION))),
    GOLD_COIN(() -> new TypedItem(new ItemDropSpec(ItemType.GOLD_COIN, 5)));

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
      return randomDrop(ThreadLocalRandom.current());
    }

    static DropTypes randomDrop(RandomGenerator random) {
      int idx = random.nextInt(VALUES.length);
      return VALUES[idx];
    }
  }

  public static Entity createRandomDrop(Vector2 position) {
    return createRandomDrop(position, ThreadLocalRandom.current());
  }

  /** Uses the shared random pool with injectable randomness for repeatable integration tests. */
  public static Entity createRandomDrop(Vector2 position, RandomGenerator random) {
    Objects.requireNonNull(position, NULL_POSITION_MESSAGE);
    Objects.requireNonNull(random, "random cannot be null");
    Entity item = createItem(DropTypes.randomDrop(random).getItemSupplier());
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
    return createDrop(ItemType.STRENGTH_CHARM, position);
  }

  /** Creates a single caller-selected item at a world position. */
  public static Entity createDrop(ItemType itemType, Vector2 position) {
    return createDrop(ItemDropSpec.single(itemType), position);
  }

  /** Creates a caller-selected item and quantity at a world position. */
  public static Entity createDrop(ItemDropSpec dropSpec, Vector2 position) {
    Objects.requireNonNull(dropSpec, "dropSpec cannot be null");
    Objects.requireNonNull(position, NULL_POSITION_MESSAGE);

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
    Objects.requireNonNull(position, NULL_POSITION_MESSAGE);
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
            .addComponent(new RotatingTextureRenderComponent(item.getTexture()))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item, quantity));

    // Only charms, consumables, and currency use the spinning visual.
    if (item instanceof StrengthCharm || item instanceof TypedItem) {
      itemEntity.addComponent(new ItemSpinComponent());
    }
    // Preserve the original item sizing based on the texture aspect ratio.
    var texture =
        com.csse3200.game.services.ServiceLocator.getResourceService()
            .getAsset(item.getTexture(), com.badlogic.gdx.graphics.Texture.class);
    itemEntity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
    return itemEntity;
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
