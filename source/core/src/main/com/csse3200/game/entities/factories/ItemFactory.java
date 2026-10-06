package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemDropAnimationComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
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

  /** Creates unregistered enemy rewards around one common visual centre. */
  public List<Entity> createEnemyDrops(String enemyType, Vector2 position) {
    Objects.requireNonNull(position, "position cannot be null");
    List<Entity> drops = new ArrayList<>();
    for (ItemDropSpec spec : lootTable.forEnemy(enemyType).roll(random)) {
      for (Item item : ItemCatalog.createItems(spec.itemId(), spec.quantity())) {
        drops.add(createItem(item, position));
      }
    }
    float angle = ThreadLocalRandom.current().nextFloat() * 360f;
    for (int i = 0; i < drops.size(); i++) {
      Entity drop = drops.get(i);
      drop.setPosition(position.cpy().sub(drop.getScale().scl(0.5f)));
      float direction = angle + i * 360f / drops.size();
      float speed = (float) ThreadLocalRandom.current().nextDouble(4.5, 7.0);
      Vector2 velocity = new Vector2(speed, 0f).rotateDeg(direction);
      var body = drop.getComponent(PhysicsComponent.class).getBody();
      // Sensor-only items must stop short of terrain instead of flying through a wall.
      Vector2 size = drop.getScale();
      Vector2 centre = drop.getCenterPosition();
      float distance = speed / body.getLinearDamping();
      float allowedTravel = distance;
      RaycastHit hit = new RaycastHit();
      // Sweep the centre and all four sprite corners, including diagonal wall approaches.
      for (int sample = 0; sample < 5; sample++) {
        Vector2 from = centre.cpy();
        if (sample > 0) {
          from.add(
              (sample <= 2 ? -0.5f : 0.5f) * size.x, (sample % 2 == 0 ? -0.5f : 0.5f) * size.y);
        }
        Vector2 to = from.cpy().mulAdd(velocity, 1f / body.getLinearDamping());
        if (ServiceLocator.getPhysicsService()
            .getPhysics()
            .raycast(from, to, PhysicsLayer.OBSTACLE, hit)) {
          allowedTravel = Math.min(allowedTravel, Math.max(0f, from.dst(hit.point) - 0.05f));
        }
      }
      velocity.scl(allowedTravel / distance);
      body.setLinearVelocity(velocity);
      drop.getComponent(ItemDropAnimationComponent.class).launchFrom(position);
    }
    return drops;
  }

  /** Creates one world entity for an already constructed item. */
  public static Entity createItem(Item item, Vector2 position) {
    Objects.requireNonNull(item, "item cannot be null");
    Objects.requireNonNull(position, "position cannot be null");
    Entity itemEntity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item))
            .addComponent(new ItemDropAnimationComponent());
    // Preserve the original item sizing based on the texture aspect ratio.
    Texture texture =
        ServiceLocator.getResourceService().getAsset(item.getTexture(), Texture.class);
    itemEntity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
    itemEntity.setPosition(position);
    return itemEntity;
  }
}
