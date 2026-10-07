package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * The condition the Magnet Potion puts on the player. While it runs, every Gold coin and consumable
 * lying within {@code radius} of the player slides towards them, and is collected through the
 * item's normal {@link Item#pickUp(Entity)} rule once it arrives.
 *
 * <p>Charms are deliberately left alone: picking a charm is a build choice the player should make
 * themselves with the pickup key. While active, the player is drawn with a soft golden glow.
 */
public class MagnetEffect extends TimedStatusEffect {
  /** Golden glow added on top of the player's sprite while the magnet is active. */
  private static final Color GLOW = new Color(1f, 0.82f, 0.2f, 0.35f);

  private final GameTime time;
  private final Entity owner;
  private final float radius;
  private final float pullSpeed;
  private final float collectDistance;
  private final Set<Entity> collected = Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * @param time game clock used for the countdown and per-frame movement
   * @param durationMs how long the magnet lasts, in milliseconds
   * @param owner the entity that items are pulled towards (the player)
   * @param radius world-unit distance within which items are pulled
   * @param pullSpeed world units per second that items move towards the owner
   * @param collectDistance distance at which a pulled item is collected
   */
  public MagnetEffect(
      GameTime time,
      long durationMs,
      Entity owner,
      float radius,
      float pullSpeed,
      float collectDistance) {
    super(time, durationMs);
    if (radius <= 0 || pullSpeed <= 0 || collectDistance <= 0) {
      throw new IllegalArgumentException("Magnet radius, speed and collect distance must be > 0");
    }
    this.time = time;
    this.owner = Objects.requireNonNull(owner, "owner cannot be null");
    this.radius = radius;
    this.pullSpeed = pullSpeed;
    this.collectDistance = collectDistance;
  }

  @Override
  public boolean update() {
    if (!isExpired()) {
      pullNearbyItems();
    }
    return super.update();
  }

  @Override
  public Color getGlow() {
    return GLOW;
  }

  /** Returns whether the magnet would pull this item (Gold and consumables only). */
  public static boolean isMagnetic(Item item) {
    return item != null
        && (item.getCategory() == ItemCategory.CURRENCY
            || item.getCategory() == ItemCategory.CONSUMABLE);
  }

  private void pullNearbyItems() {
    EntityService entityService = ServiceLocator.getEntityService();
    if (entityService == null) {
      return;
    }
    Vector2 target = owner.getCenterPosition();
    float step = pullSpeed * time.getDeltaTime();
    Array<Entity> entities = entityService.getEntities();
    // Index loop: this runs inside EntityService's own iteration over the same array.
    for (int i = 0; i < entities.size; i++) {
      Entity entity = entities.get(i);
      if (entity == null || entity == owner || collected.contains(entity)) {
        continue;
      }
      ItemComponent itemComponent = entity.getComponent(ItemComponent.class);
      if (itemComponent == null || !isMagnetic(itemComponent.getItem())) {
        continue;
      }
      Vector2 offset = target.cpy().sub(entity.getCenterPosition());
      float distance = offset.len();
      if (distance > radius) {
        continue;
      }
      if (distance <= collectDistance || distance <= step) {
        collect(entity, itemComponent.getItem(), entityService);
      } else {
        entity.setPosition(entity.getPosition().cpy().add(offset.scl(step / distance)));
      }
    }
  }

  private void collect(Entity itemEntity, Item item, EntityService entityService) {
    collected.add(itemEntity);
    item.pickUp(owner);
    // Deferred: we are inside the entity update loop, so the item is disposed once it finishes.
    entityService.scheduleDisposal(itemEntity);
  }
}
