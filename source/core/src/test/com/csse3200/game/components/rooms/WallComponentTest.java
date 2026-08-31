package com.csse3200.game.components.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WallComponentTest {
  @Test
  void shouldSpawnFourBoundaryWalls() {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    EntityService entityService = mock(EntityService.class);
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(terrain.getTileSize()).thenReturn(0.5f);
    when(terrain.getMapBounds(0)).thenReturn(new GridPoint2(50, 50));
    when(terrain.tileToWorldPosition(any(GridPoint2.class))).thenReturn(new Vector2());
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(entityService);

    WallComponent walls = new WallComponent();
    walls.setEntity(room);
    walls.create();

    verify(entityService, times(4)).register(any(Entity.class));
  }
}
