package com.csse3200.game.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.Component;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.ui.UIComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EntityServiceFreezeTest {
  private static class Counter extends Component {
    int updates;

    @Override
    public void update() {
      updates++;
    }
  }

  private EntityService service;
  private Counter world;
  private Counter exempt;

  @BeforeEach
  void setUp() {
    service = new EntityService();
    world = new Counter();
    exempt = new Counter();
    service.register(new Entity().addComponent(world));
    Entity ui = new Entity().addComponent(exempt);
    ui.setUpdatesWhilePaused(true);
    service.register(ui);
  }

  @Test
  void everythingUpdatesWhenNothingIsFrozen() {
    service.update();
    assertEquals(1, world.updates);
    assertEquals(1, exempt.updates);
  }

  @Test
  void freezeStopsEveryoneExceptOptedInEntities() {
    service.setFrozen(this, true);
    assertTrue(service.isFrozen());

    service.update();

    assertEquals(0, world.updates);
    assertEquals(1, exempt.updates);
  }

  @Test
  void freezeIsReleasedOnlyWhenEveryOwnerHasReleased() {
    Object dialogue = new Object();
    Object cutscene = new Object();
    service.setFrozen(dialogue, true);
    service.setFrozen(cutscene, true);

    service.setFrozen(cutscene, false);
    assertTrue(service.isFrozen());
    service.setFrozen(cutscene, false);
    assertTrue(service.isFrozen());

    service.setFrozen(dialogue, false);
    assertFalse(service.isFrozen());
    service.update();
    assertEquals(1, world.updates);
  }

  @Test
  void freezeIsIndependentOfTheInventoryPause() {
    service.toggleUpdate(); // inventory opens: paused
    int before = world.updates;
    service.update();
    assertEquals(before, world.updates);

    service.setFrozen(this, true);
    service.setFrozen(this, false);
    service.update();
    assertEquals(before, world.updates, "still paused by the inventory, not released by freeze");
  }

  @Test
  void uiComponentsDoNotCountAsOptedIn() {
    Counter plainCounter = new Counter();
    Entity enemyWithHud =
        new Entity()
            .addComponent(plainCounter)
            .addComponent(
                new UIComponent() {
                  @Override
                  protected void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
                    // not needed: only the component's presence matters to this test
                  }
                });
    // Entities with a UIComponent keep updating under the inventory pause, but not under a freeze
    service.getEntities().add(enemyWithHud);
    service.setFrozen(this, true);

    service.update();

    assertEquals(0, plainCounter.updates);
  }
}
