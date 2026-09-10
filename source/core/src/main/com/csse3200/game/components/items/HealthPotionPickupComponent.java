package com.csse3200.game.components.items;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Temporary demo adapter that stores Health Potions when the player picks them up. */
public class HealthPotionPickupComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(HealthPotionPickupComponent.class);

  private final Set<Entity> nearbyPotions = new LinkedHashSet<>();
  private HitboxComponent hitboxComponent;
  private InventoryComponent inventory;

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    inventory = entity.getComponent(InventoryComponent.class);
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
    if (item != null && item.getItemType() == ItemType.HEALTH_POTION) {
      nearbyPotions.add(itemEntity);
    }
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    Entity itemEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    nearbyPotions.remove(itemEntity);
  }

  private void onItemPickup() {
    if (nearbyPotions.isEmpty()) {
      return;
    }

    Entity itemEntity = nearbyPotions.iterator().next();
    ItemComponent item = itemEntity.getComponent(ItemComponent.class);
    if (item == null || item.getItemType() != ItemType.HEALTH_POTION) {
      nearbyPotions.remove(itemEntity);
      return;
    }

    for (int i = 0; i < item.getQuantity(); i++) {
      inventory.addConsumable(ItemType.HEALTH_POTION);
    }
    nearbyPotions.remove(itemEntity);
    itemEntity.dispose();
    logger.info("Stored {} Health Potion(s)", item.getQuantity());
  }
}
