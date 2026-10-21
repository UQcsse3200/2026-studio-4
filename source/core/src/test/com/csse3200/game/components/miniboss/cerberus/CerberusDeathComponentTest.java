package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CerberusDeathComponentTest {
  @Test
  void shouldAnnounceDefeatOnceAfterAllHeadsDieInAnyOrder() {
    int[][] orders = {
      {0, 1, 2},
      {0, 2, 1},
      {1, 0, 2},
      {1, 2, 0},
      {2, 0, 1},
      {2, 1, 0}
    };

    for (int[] order : orders) {
      verifyDeathOrder(order);
    }
  }

  // creates a fresh setup for each death order and counts defeat events
  private void verifyDeathOrder(int[] order) {
    EntityService service = mock(EntityService.class);
    ServiceLocator.registerEntityService(service);

    Entity left = new Entity().addComponent(new CombatStatsComponent(125, 10));
    Entity middle = new Entity().addComponent(new CombatStatsComponent(250, 20));
    Entity right = new Entity().addComponent(new CombatStatsComponent(125, 10));

    CerberusDeathComponent death = new CerberusDeathComponent(left, right);
    middle.addComponent(death);

    int[] defeats = {0};
    middle.getEvents().addListener(CerberusDeathComponent.DEFEATED, () -> defeats[0]++);

    death.create();

    Entity[] heads = {left, middle, right};

    heads[order[0]].getComponent(CombatStatsComponent.class).setHealth(0);
    assertEquals(0, defeats[0]);
    verify(service, never()).scheduleDisposal(middle);

    heads[order[1]].getComponent(CombatStatsComponent.class).setHealth(0);
    assertEquals(0, defeats[0]);
    verify(service, never()).scheduleDisposal(middle);

    heads[order[2]].getComponent(CombatStatsComponent.class).setHealth(0);
    assertEquals(1, defeats[0]);
    verify(service, times(1)).scheduleDisposal(middle);

    // Repeated death notifications must not announce another victory.
    for (Entity head : heads) {
      head.getEvents().trigger("entityDied");
    }

    assertEquals(1, defeats[0]);
    verify(service, times(1)).scheduleDisposal(middle);
  }
}
