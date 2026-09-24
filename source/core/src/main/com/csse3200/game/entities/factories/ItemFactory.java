package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemSpinComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
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
  private final LootTable lootTable;
  private final RandomGenerator random;

  public ItemFactory() {
    this(LootTable.defaultTable(), RandomGenerator.getDefault());
  }

  public ItemFactory(LootTable lootTable, RandomGenerator random) {
    this.lootTable = Objects.requireNonNull(lootTable, "lootTable cannot be null");
    this.random = Objects.requireNonNull(random, "random cannot be null");
    this.lootTable.validate();
  }

  /** Creates the items selected by an enemy's loot rules without registering them. */
  public List<Entity> createEnemyDrops(String enemyType, Vector2 position) {
    Objects.requireNonNull(position, "position cannot be null");
    List<Entity> drops = new ArrayList<>();
    for (ItemDropSpec spec : lootTable.forEnemy(enemyType).roll(random)) {
      drops.add(createItem(spec.itemType().createItem(spec.quantity()), position));
    }
    return drops;
  }

  /** Creates one world entity for an already constructed item. */
  public static Entity createItem(Item item, Vector2 position) {
    Objects.requireNonNull(item, "item cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
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
}
