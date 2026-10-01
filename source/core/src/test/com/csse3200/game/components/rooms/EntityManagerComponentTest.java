package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
public class EntityManagerComponentTest {

  @Spy EntityManagerComponent manager;
  @Mock TerrainComponent terrain;
  @Mock Entity room;
  @Mock EntityService entityService;

  private static final Vector2 BOUNDS = new Vector2(50, 50);

  @BeforeEach
  void setup() {
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(terrain.getMapBounds(anyInt())).thenReturn(new GridPoint2());
    when(terrain.tileToWorldPosition(any())).thenReturn(BOUNDS);

    manager.setEntity(room);

    ServiceLocator.registerEntityService(entityService);
  }

  private static Entity createEntityAtPos(float x, float y) {
    Entity entity = new Entity();
    entity.setPosition(x, y);
    return entity;
  }

  @Test
  void shouldRejectOutOfBoundsSpawns() {
    var oobEntitiesPos =
        new Vector2[] {
          new Vector2(-1, 0),
          new Vector2(0, -1),
          new Vector2(-1, -1),
          new Vector2(51, -1),
          new Vector2(1, 51),
          new Vector2(51, 1),
          new Vector2(51, 51),
          new Vector2(-1, 51),
          new Vector2(51, -1),
        };

    for (var pos : oobEntitiesPos) {
      var entity = new Entity();
      entity.setPosition(pos);
      manager.spawnEntity(entity);
    }

    verify(entityService, never()).register(any());
    assertEquals(0, manager.entities.size());
  }

  @Test
  void shouldSpawnValidEntities() {
    Entity entity = createEntityAtPos(10, 10);
    manager.spawnEntityAt(new Entity(), new GridPoint2(12, 12), true, true);
    manager.spawnEntity(entity);

    verify(entityService, times(2)).register(any());
    assertEquals(2, manager.entities.size());
  }
}
