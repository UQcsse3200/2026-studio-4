package com.csse3200.game.components.items;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Charm;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets an entity (the player) pick up charms dropped in the game world.
 *
 * <p>Follows the same touch-detection shape as {@link
 * com.csse3200.game.components.TouchAttackComponent}: it listens for {@code collisionStart} /
 * {@code collisionEnd} on its own {@link HitboxComponent} to track which item entities (on the
 * {@link PhysicsLayer#ITEM} layer) are currently in range. Nothing happens on contact alone &mdash;
 * the charm is only picked up once an {@code "interact"} event fires while at least one item is in
 * range (see the Core Player Actions feature, which owns the interact key binding).
 *
 * <p>Item entities are expected to carry an {@link ItemComponent} (see {@link
 * com.csse3200.game.entities.factories.ItemFactory}), which is where this component reads the
 * {@link Charm} from.
 *
 * <p>Requires {@link HitboxComponent} and {@link InventoryComponent} on this entity.
 */
public class CharmPickupComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(CharmPickupComponent.class);

  private HitboxComponent hitboxComponent;
  private InventoryComponent inventoryComponent;
  private final Set<Entity> nearbyItems = new LinkedHashSet<>();

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    inventoryComponent = entity.getComponent(InventoryComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    entity.getEvents().addListener("interact", this::onInteract);
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
  private void onInteract() {
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

    Charm charm = itemComponent.getCharm();
    inventoryComponent.addCharm(charm);
    nearbyItems.remove(itemEntity);
    itemEntity.dispose();

    logger.info("Picked up charm: {}", charm.getName());
  }
}
