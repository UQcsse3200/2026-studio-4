package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and registers one item when the owning enemy emits its death event.
 *
 * <p>Item creation and room registration are supplied by callers so this component does not depend
 * on a particular item factory or game-area implementation. For Sprint 1, the item factory should
 * create the Team 5 Strength Charm.
 */
public class EnemyDropComponent extends Component {
  public static final String DEFAULT_DEATH_EVENT = "death";

  private static final Logger logger = LoggerFactory.getLogger(EnemyDropComponent.class);

  private final String deathEventName;
  private final Supplier<Entity> itemFactory;
  private final Consumer<Entity> itemRegistrar;
  private boolean itemDropped;

  /**
   * Creates an enemy drop component using the default {@value #DEFAULT_DEATH_EVENT} event.
   *
   * @param itemFactory creates the item entity to drop
   * @param itemRegistrar registers the dropped item with the active room or game area
   */
  public EnemyDropComponent(Supplier<Entity> itemFactory, Consumer<Entity> itemRegistrar) {
    this(DEFAULT_DEATH_EVENT, itemFactory, itemRegistrar);
  }

  /**
   * Creates an enemy drop component using a configurable death event name.
   *
   * @param deathEventName zero-argument event emitted when the owning enemy dies
   * @param itemFactory creates the item entity to drop
   * @param itemRegistrar registers the dropped item with the active room or game area
   */
  public EnemyDropComponent(
      String deathEventName, Supplier<Entity> itemFactory, Consumer<Entity> itemRegistrar) {
    if (deathEventName == null || deathEventName.isBlank()) {
      throw new IllegalArgumentException("Death event name must not be blank");
    }
    this.deathEventName = deathEventName;
    this.itemFactory = Objects.requireNonNull(itemFactory, "Item factory must not be null");
    this.itemRegistrar = Objects.requireNonNull(itemRegistrar, "Item registrar must not be null");
  }

  @Override
  public void create() {
    entity.getEvents().addListener(deathEventName, this::dropItem);
  }

  /**
   * Returns whether this enemy has already produced its item drop.
   *
   * @return true after the item has been successfully registered
   */
  public boolean hasDroppedItem() {
    return itemDropped;
  }

  private void dropItem() {
    if (itemDropped) {
      logger.debug("Ignoring duplicate {} event for {}", deathEventName, entity);
      return;
    }

    Entity item = Objects.requireNonNull(itemFactory.get(), "Item factory returned null");
    item.setPosition(entity.getPosition());
    itemDropped = true;

    try {
      itemRegistrar.accept(item);
    } catch (RuntimeException | Error exception) {
      itemDropped = false;
      throw exception;
    }

    logger.debug("Dropped {} at {} for {}", item, item.getPosition(), entity);
  }
}
