package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class SplitComponentTest {

  private MockedStatic<NPCFactory> npcFactoryMock;
  private EntityService entityService;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
    npcFactoryMock = mockStatic(NPCFactory.class);
  }

  @AfterEach
  void tearDown() {
    npcFactoryMock.close();
  }

  @Test
  void shouldSplitIntoTwoOnFirstHit() {
    Entity target = new Entity();

    Entity original = new Entity();
    original.addComponent(new CombatStatsComponent(40, 10));
    original.addComponent(new SplitComponent(target));
    original.create();

    Entity childStub = new Entity();
    childStub.addComponent(new CombatStatsComponent(1, 1));

    npcFactoryMock.when(() -> NPCFactory.createChaseEnemy(target)).thenReturn(childStub);

    int[] spawnedChildren = {0};
    original.getEvents().addListener("spawnChildren", (Entity child) -> spawnedChildren[0]++);

    original.getEvents().trigger("hitReaction", target);

    assertEquals(2, spawnedChildren[0]);
    npcFactoryMock.verify(() -> NPCFactory.createChaseEnemy(target), times(2));
  }

  @Test
  void shouldOnlySplitOnce() {
    Entity target = new Entity();

    Entity original = new Entity();
    original.addComponent(new CombatStatsComponent(40, 10));
    original.addComponent(new SplitComponent(target));
    original.create();

    Entity childStub = new Entity();
    childStub.addComponent(new CombatStatsComponent(1, 1));

    npcFactoryMock.when(() -> NPCFactory.createChaseEnemy(target)).thenReturn(childStub);

    int[] spawnedChildren = {0};
    original.getEvents().addListener("spawnChildren", (Entity child) -> spawnedChildren[0]++);

    original.getEvents().trigger("hitReaction", target);
    original.getEvents().trigger("hitReaction", target);

    assertEquals(2, spawnedChildren[0]);
    npcFactoryMock.verify(() -> NPCFactory.createChaseEnemy(target), times(2));
  }

  @Test
  void shouldDoNothingIfNoCombatStatsComponent() {
    Entity target = new Entity();

    Entity original = new Entity();
    original.addComponent(new SplitComponent(target));
    original.create();

    int[] spawnedChildren = {0};
    original.getEvents().addListener("spawnChildren", (Entity child) -> spawnedChildren[0]++);

    original.getEvents().trigger("hitReaction", target);

    assertEquals(0, spawnedChildren[0]);
    npcFactoryMock.verifyNoInteractions();
  }
}
