package com.csse3200.game.components.items;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Temporary demo adapter that lets the player collect currency through {@code itemPickup}. */
public class CurrencyPickupComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(CurrencyPickupComponent.class);

  private final Set<Entity> nearbyCurrency = new LinkedHashSet<>();
  private HitboxComponent hitboxComponent;
  private InventoryComponent inventoryComponent;

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    inventoryComponent = entity.getComponent(InventoryComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    entity.getEvents().addListener("itemPickup", this::onItemPickup);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me
        || !PhysicsLayer.contains(PhysicsLayer.ITEM, other.getFilterData().categoryBits)) {
      return;
    }

    Entity itemEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    ItemComponent item = itemEntity.getComponent(ItemComponent.class);
    if (item != null && item.getItemType().isCurrency()) {
      nearbyCurrency.add(itemEntity);
    }
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    Entity itemEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    nearbyCurrency.remove(itemEntity);
  }

  private void onItemPickup() {
    if (nearbyCurrency.isEmpty()) {
      return;
    }

    Entity itemEntity = nearbyCurrency.iterator().next();
    ItemComponent item = itemEntity.getComponent(ItemComponent.class);
    if (item == null || !item.getItemType().isCurrency()) {
      nearbyCurrency.remove(itemEntity);
      return;
    }

    inventoryComponent.addGold(item.getQuantity());
    nearbyCurrency.remove(itemEntity);
    itemEntity.dispose();
    logger.info("Picked up {} Gold", item.getQuantity());
  }
}
