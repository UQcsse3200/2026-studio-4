package com.csse3200.game.components.items;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.components.rooms.FriendlyNpcManagerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lets an entity (the player) pick up items dropped in the game world.
 *
 * <p>Follows the same touch-detection shape as {@link
 * com.csse3200.game.components.TouchAttackComponent}: it listens for {@code collisionStart} /
 * {@code collisionEnd} on its own {@link HitboxComponent} to track which item entities (on the
 * {@link PhysicsLayer#ITEM} layer) are currently in range. Nothing happens on contact alone &mdash;
 * an item is only picked up once an {@code "itemPickup"} event fires while at least one item is in
 * range. The dedicated event keeps item pickup independent from the Room feature's {@code
 * "interact"} event while allowing both actions to share the E key binding.
 *
 * <p>Item entities are expected to carry an {@link ItemComponent} (see {@link
 * com.csse3200.game.entities.factories.ItemFactory}), which is where this component reads the
 * shared {@link com.csse3200.game.items.Item} from.
 *
 * <p>Requires {@link HitboxComponent} and {@link
 * com.csse3200.game.components.player.InventoryComponent} on this entity.
 */
public class ItemPickupComponent extends Component {
  private HitboxComponent hitboxComponent;
  private final Set<Entity> nearbyItems = new LinkedHashSet<>();
  private boolean showingPickupPrompt;

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    entity.getEvents().addListener("itemPickup", this::onItemPickup);
  }

  /**
   * Shows "Press E — Pick up …" while a drop is in range, and hides that sentence after the player
   * walks away or picks it up. An NPC sentence takes priority, and any other prompt is left as it
   * is.
   */
  @Override
  public void update() {
    refreshPickupPrompt(entity.getComponent(InteractionPromptDisplay.class));
  }

  /**
   * Puts the current pickup sentence on the HUD.
   *
   * @param display the player's prompt, or null when this entity has none
   */
  void refreshPickupPrompt(InteractionPromptDisplay display) {
    if (display == null) {
      return;
    }
    // The room is registered before the player in the first room, so this update runs after the
    // NPC prompt. Yielding here keeps "Press E — Talk to …" from being replaced by a drop.
    if (npcPromptInRange() != null) {
      showingPickupPrompt = false;
      return;
    }
    String prompt = getPickupPrompt();
    if (prompt != null) {
      display.setPrompt(prompt);
      showingPickupPrompt = true;
      return;
    }
    if (showingPickupPrompt) {
      display.clearPrompt();
      showingPickupPrompt = false;
    }
  }

  /**
   * The NPC sentence that should stay on the HUD instead of a pickup sentence.
   *
   * @return that sentence, or null when no NPC is in range
   */
  private String npcPromptInRange() {
    EntityService entities = ServiceLocator.getEntityService();
    if (entities == null) {
      return null;
    }
    for (Entity other : entities.getEntities()) {
      FriendlyNpcManagerComponent npcs = other.getComponent(FriendlyNpcManagerComponent.class);
      if (npcs == null) {
        continue;
      }
      String prompt = npcs.getPrompt(entity);
      if (prompt != null) {
        return prompt;
      }
    }
    return null;
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
    ItemComponent itemComponent = itemEntity.getComponent(ItemComponent.class);
    if (itemComponent != null && !itemComponent.isCollected()) {
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
    removeUnavailableItems();
    if (nearbyItems.isEmpty()) {
      return;
    }

    Entity itemEntity = nearbyItems.iterator().next();
    ItemComponent itemComponent = itemEntity.getComponent(ItemComponent.class);
    nearbyItems.remove(itemEntity);
    if (itemComponent.tryPickUp(entity)) {
      ServiceLocator.getEntityService().scheduleDisposal(itemEntity);
    }
  }

  /**
   * HUD copy while an item is in pickup range.
   *
   * @return prompt text, or null if nothing is nearby
   */
  public String getPickupPrompt() {
    removeUnavailableItems();
    if (nearbyItems.isEmpty()) {
      return null;
    }
    Entity itemEntity = nearbyItems.iterator().next();
    ItemComponent itemComponent = itemEntity.getComponent(ItemComponent.class);
    return InteractionPrompt.forItem(itemComponent.getItem().getName());
  }

  private void removeUnavailableItems() {
    nearbyItems.removeIf(
        itemEntity -> {
          ItemComponent itemComponent = itemEntity.getComponent(ItemComponent.class);
          return itemComponent == null || itemComponent.isCollected();
        });
  }
}
