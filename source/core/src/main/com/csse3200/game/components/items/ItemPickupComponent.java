package com.csse3200.game.components.items;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lets an entity (the player) pick up charms dropped in the game world.
 *
 * <p>Follows the same touch-detection shape as {@link
 * com.csse3200.game.components.TouchAttackComponent}: it listens for {@code collisionStart} /
 * {@code collisionEnd} on its own {@link HitboxComponent} to track which item entities (on the
 * {@link PhysicsLayer#ITEM} layer) are currently in range. Nothing happens on contact alone &mdash;
 * the charm is only picked up once an {@code "itemPickup"} event fires while at least one item is
 * in range. The dedicated event keeps item pickup independent from the Room feature's {@code
 * "interact"} event while allowing both actions to share the E key binding.
 *
 * <p>Item entities are expected to carry an {@link ItemComponent} (see {@link
 * com.csse3200.game.entities.factories.ItemFactory}), which is where this component reads the
 * {@link Charm} from.
 *
 * <p>Requires {@link HitboxComponent} and {@link InventoryComponent} on this entity.
 */
public class ItemPickupComponent extends Component {
  private HitboxComponent hitboxComponent;
  private final Set<Entity> nearbyItems = new LinkedHashSet<>();

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    entity.getEvents().addListener("itemPickup", this::onItemPickup);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      // Not triggered by our own hitbox, ignore.
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.ITEM, other.getFilterData().categoryBits)) {
      // Not an item, ignore.
      return;
    }

    Entity itemEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    if (itemEntity.getComponent(ItemComponent.class) != null) {
      nearbyItems.add(itemEntity);
    }
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }
    Entity itemEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    nearbyItems.remove(itemEntity);
  }

  /** Picks up the first item currently in range, if any. */
  private void onItemPickup() {
    if (nearbyItems.isEmpty()) {
      return;
    }

    Entity itemEntity = nearbyItems.iterator().next();
    ItemComponent itemComponent = itemEntity.getComponent(ItemComponent.class);
    if (itemComponent == null) {
      // Item entity was disposed of by something else between overlap and interact.
      nearbyItems.remove(itemEntity);
      return;
    }

    Item item = itemComponent.getItem();
    item.pickUp(entity);

    nearbyItems.remove(itemEntity);
    itemEntity.dispose();
  }
}
