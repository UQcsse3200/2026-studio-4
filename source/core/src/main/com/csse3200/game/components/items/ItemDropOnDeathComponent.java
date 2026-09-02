package com.csse3200.game.components.items;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creates and registers one item drop when this component's entity dies.
 *
 * <p>The component listens only to its owning entity's {@code entityDied} event. This avoids
 * treating unrelated entity deaths as item-drop requests and keeps item behaviour separate from
 * Enemy Team's death component.
 *
 * <p>Implementation developed with assistance from OpenAI Codex and reviewed by Yuezhou Wang.
 */
public class ItemDropOnDeathComponent extends Component {
  private static final String ENTITY_DIED_EVENT = "entityDied";

  private final Function<Vector2, Entity> dropFactory;
  private final Consumer<Entity> dropRegistrar;
  private boolean hasDropped;

  /**
   * Creates a component that drops the requested item type through {@link ItemFactory}.
   *
   * @param itemType item type created when the owning entity dies
   */
  public ItemDropOnDeathComponent(ItemType itemType) {
    this(
        position -> ItemFactory.createDrop(itemType, position),
        drop -> ServiceLocator.getEntityService().register(drop));
    Objects.requireNonNull(itemType, "itemType cannot be null");
  }

  ItemDropOnDeathComponent(Function<Vector2, Entity> dropFactory, Consumer<Entity> dropRegistrar) {
    this.dropFactory = Objects.requireNonNull(dropFactory, "dropFactory cannot be null");
    this.dropRegistrar = Objects.requireNonNull(dropRegistrar, "dropRegistrar cannot be null");
  }

  @Override
  public void create() {
    entity.getEvents().addListener(ENTITY_DIED_EVENT, this::onEntityDied);
  }

  private void onEntityDied() {
    if (hasDropped) {
      return;
    }

    hasDropped = true;
    Entity drop = dropFactory.apply(entity.getPosition());
    dropRegistrar.accept(drop);
  }
}
