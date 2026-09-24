package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemSpinComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Creates world item entities from explicit item IDs or enemy loot rules. */
public final class ItemFactory {
  /** Charms become separate entities; stackable items keep their requested quantity. */
  public static List<Entity> createDrops(ItemType itemId, int quantity, Vector2 position) {
    Objects.requireNonNull(itemId, "itemId cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    if (itemId.getCategory() != ItemCategory.CHARM) {
      return List.of(createEntity(itemId.createItem(quantity), position));
    }
    List<Entity> entities = new ArrayList<>(quantity);
    for (int i = 0; i < quantity; i++) {
      entities.add(createEntity(itemId.createItem(1), position));
    }
    return entities;
  }

  /** Creates the items selected by an enemy's loot rules without registering them. */
  public static List<Entity> createEnemyDrops(
      String enemyType, Vector2 position, LootTable lootTable, RandomGenerator random) {
    Objects.requireNonNull(lootTable, "lootTable cannot be null");
    Objects.requireNonNull(random, "random cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
    List<Entity> drops = new ArrayList<>();
    for (ItemDropSpec spec : lootTable.forEnemy(enemyType).roll(random)) {
      drops.addAll(createDrops(spec.itemType(), spec.quantity(), position));
    }
    return drops;
  }

  private static Entity createEntity(Item item, Vector2 position) {
    Entity itemEntity =
        new Entity()
            .addComponent(new RotatingTextureRenderComponent(item.getTexture()))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item))
            .addComponent(new ItemSpinComponent());
    // Preserve the original item sizing based on the texture aspect ratio.
    Texture texture =
        ServiceLocator.getResourceService().getAsset(item.getTexture(), Texture.class);
    itemEntity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
    itemEntity.setPosition(position);
    return itemEntity;
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
