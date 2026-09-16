package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import java.util.Objects;
import java.util.Random;

/** Factory for creating item entities. */
public final class ItemFactory {

  /**
   * Creates the requested item at a world position.
   *
   * @param position world position assigned to the item entity
   * @return a non-null, positioned, unregistered item entity for the room to spawn
   */
  public static Entity createDrop(Vector2 position) {
    Objects.requireNonNull(position, "position cannot be null");
    Random random = new Random();
    //    Random for now, in future will be determined by entity type.
    int choice = random.nextInt(3); // Gives 0, 1, or 2
    final Entity item =
        switch (choice) {
          case 0 -> createStrengthCharm();
          case 1 -> createAttackSpeedCharm();
          default -> createMovementSpeedCharm();
        };
    item.setPosition(position);
    return item;
  }

  /** Creates an item entity to be spawned into the game. */
  public static Entity createItem(Item item) {
    Entity itemEntity =
        new Entity()
            .addComponent(new TextureRenderComponent(item.getTexture()))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(item));

    itemEntity.getComponent(TextureRenderComponent.class).scaleEntity();
    return itemEntity;
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createStrengthCharm() {
    return createItem(new StrengthCharm());
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createMovementSpeedCharm() {
    return createItem(new SpeedCharm());
  }

  /**
   * Creates the Strength Charm used for Sprint 1 item drops.
   *
   * <p>The returned entity is not positioned or registered. The room that requests the item owns
   * those responsibilities.
   *
   * @return an unregistered Strength Charm entity
   */
  public static Entity createAttackSpeedCharm() {
    return createItem(new AttackSpeedCharm());
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
