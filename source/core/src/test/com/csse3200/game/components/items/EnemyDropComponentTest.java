package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
@SuppressWarnings("unchecked")
class EnemyDropComponentTest {
  @Test
  void shouldDropFactoryItemAtEnemyPositionOnDeath() {
    Vector2 enemyPosition = new Vector2(4.5f, 7.25f);
    Entity droppedItem = new Entity();
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    when(itemFactory.get()).thenReturn(droppedItem);

    EnemyDropComponent dropComponent = new EnemyDropComponent(itemFactory, itemRegistrar);
    Entity enemy = new Entity().addComponent(dropComponent);
    enemy.create();
    enemy.setPosition(enemyPosition);

    assertFalse(dropComponent.hasDroppedItem());
    enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT);

    verify(itemFactory).get();
    verify(itemRegistrar).accept(droppedItem);
    assertEquals(enemyPosition, droppedItem.getPosition());
    assertTrue(dropComponent.hasDroppedItem());
  }

  @Test
  void shouldNotDropBeforeDeathEvent() {
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    Entity enemy = new Entity().addComponent(new EnemyDropComponent(itemFactory, itemRegistrar));
    enemy.create();

    enemy.getEvents().trigger("unrelatedEvent");

    verify(itemFactory, never()).get();
    verify(itemRegistrar, never()).accept(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldDropOnlyOnceForDuplicateDeathEvents() {
    Entity droppedItem = new Entity();
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    when(itemFactory.get()).thenReturn(droppedItem);
    Entity enemy = new Entity().addComponent(new EnemyDropComponent(itemFactory, itemRegistrar));
    enemy.create();

    enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT);
    enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT);

    verify(itemFactory, times(1)).get();
    verify(itemRegistrar, times(1)).accept(droppedItem);
  }

  @Test
  void shouldSupportConfiguredDeathEventName() {
    Entity droppedItem = new Entity();
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    when(itemFactory.get()).thenReturn(droppedItem);
    Entity enemy =
        new Entity()
            .addComponent(new EnemyDropComponent("enemyDefeated", itemFactory, itemRegistrar));
    enemy.create();

    enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT);
    verify(itemRegistrar, never()).accept(droppedItem);

    enemy.getEvents().trigger("enemyDefeated");
    verify(itemRegistrar).accept(droppedItem);
  }

  @Test
  void shouldRejectInvalidDependencies() {
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);

    assertThrows(
        IllegalArgumentException.class,
        () -> new EnemyDropComponent(" ", itemFactory, itemRegistrar));
    assertThrows(
        NullPointerException.class, () -> new EnemyDropComponent("death", null, itemRegistrar));
    assertThrows(
        NullPointerException.class, () -> new EnemyDropComponent("death", itemFactory, null));
  }

  @Test
  void shouldRejectNullItemFromFactory() {
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    EnemyDropComponent dropComponent = new EnemyDropComponent(itemFactory, itemRegistrar);
    Entity enemy = new Entity().addComponent(dropComponent);
    enemy.create();

    assertThrows(
        NullPointerException.class,
        () -> enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT));
    verify(itemRegistrar, never()).accept(org.mockito.ArgumentMatchers.any());
    assertFalse(dropComponent.hasDroppedItem());
  }

  @Test
  void shouldNotMarkDropCompleteWhenRegistrationFails() {
    Entity droppedItem = new Entity();
    Supplier<Entity> itemFactory = mock(Supplier.class);
    Consumer<Entity> itemRegistrar = mock(Consumer.class);
    when(itemFactory.get()).thenReturn(droppedItem);
    org.mockito.Mockito.doThrow(new IllegalStateException("Room is unavailable"))
        .when(itemRegistrar)
        .accept(droppedItem);
    EnemyDropComponent dropComponent = new EnemyDropComponent(itemFactory, itemRegistrar);
    Entity enemy = new Entity().addComponent(dropComponent);
    enemy.create();

    assertThrows(
        IllegalStateException.class,
        () -> enemy.getEvents().trigger(EnemyDropComponent.DEFAULT_DEATH_EVENT));
    assertFalse(dropComponent.hasDroppedItem());
  }
}
