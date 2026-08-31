package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class EnemyManagerComponentTest {
  private Entity room;
  private EnemyManagerComponent enemyManager;
  private EntityService entityService;
  private List<Vector2> dropPositions;
  private List<Entity> drops;

  private static Entity createMockRoom() {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);

    // A real EventHandler, so triggers actually reach listeners.
    when(room.getEvents()).thenReturn(new EventHandler());
    when(terrain.getMapBounds(0)).thenAnswer(inv -> new GridPoint2(20, 20));
    when(terrain.getTileSize()).thenReturn(1f);
    when(terrain.tileToWorldPosition(Mockito.any())).thenReturn(new Vector2());
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);

    return room;
  }

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
    room = createMockRoom();
    dropPositions = new ArrayList<>();
    drops = new ArrayList<>();
    enemyManager =
        new EnemyManagerComponent(
            position -> {
              dropPositions.add(position.cpy());
              Entity drop = mock(Entity.class);
              drops.add(drop);
              return drop;
            });
    enemyManager.setEntity(room);
    enemyManager.create();
  }

  /** Registers n enemies with the manager and returns them. */
  private Entity[] trackEnemies(int n) {
    Entity[] enemies = new Entity[n];
    for (int i = 0; i < n; i++) {
      Entity enemy = mock(Entity.class);
      when(enemy.getEvents()).thenReturn(new EventHandler());
      when(enemy.getPosition()).thenReturn(new Vector2(i + 1f, i + 2f));
      enemyManager.track(enemy);
      enemies[i] = enemy;
    }
    return enemies;
  }

  @Test
  void shouldNotClearRoomWhileEnemiesRemain() {
    Entity[] enemies = trackEnemies(3);
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);

    enemies[0].getEvents().trigger("entityDied");
    enemies[1].getEvents().trigger("entityDied");

    assertEquals(0, cleared[0], "roomCleared fired before the last enemy died");
  }

  @Test
  void shouldClearRoomWhenLastEnemyDies() {
    Entity[] enemies = trackEnemies(3);
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);

    for (Entity enemy : enemies) {
      enemy.getEvents().trigger("entityDied");
    }

    assertEquals(1, cleared[0], "roomCleared should fire once all enemies are dead");
  }

  @Test
  void shouldSpawnAndRegisterDropAtEnemyDeathPosition() {
    Entity enemy = trackEnemies(1)[0];

    enemy.getEvents().trigger("entityDied");

    assertEquals(List.of(), dropPositions);
    verify(entityService, never()).register(Mockito.any());

    enemyManager.update();

    assertEquals(List.of(new Vector2(1f, 2f)), dropPositions);
    assertEquals(1, drops.size());
    verify(entityService).register(drops.get(0));
  }

  @Test
  void shouldDropAndClearRoomOnlyOnceForRepeatedDeathEvents() {
    Entity enemy = trackEnemies(1)[0];
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);

    enemy.getEvents().trigger("entityDied");
    enemy.getEvents().trigger("entityDied");
    enemyManager.update();

    assertEquals(1, drops.size());
    assertEquals(1, cleared[0]);
    verify(entityService, times(1)).register(drops.get(0));
  }

  @Test
  void shouldDisposeSpawnedDropWithRoomContent() {
    Entity enemy = trackEnemies(1)[0];
    enemy.getEvents().trigger("entityDied");
    enemyManager.update();

    enemyManager.dispose();

    verify(drops.get(0)).dispose();
  }

  @Test
  void productionFactoryShouldCreateStrengthCharmAtEnemyDeathPosition() {
    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    EnemyManagerComponent productionEnemyManager = new EnemyManagerComponent();
    productionEnemyManager.setEntity(room);
    productionEnemyManager.create();
    Entity enemy = mock(Entity.class);
    when(enemy.getEvents()).thenReturn(new EventHandler());
    when(enemy.getPosition()).thenReturn(new Vector2(4f, 6f));
    productionEnemyManager.track(enemy);

    enemy.getEvents().trigger("entityDied");
    productionEnemyManager.update();

    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(entityCaptor.capture());
    Entity drop = entityCaptor.getValue();
    assertEquals(new Vector2(4f, 6f), drop.getPosition());
    assertNotNull(drop.getComponent(ItemComponent.class));
    assertEquals("Strength Charm", drop.getComponent(ItemComponent.class).getCharm().getName());
  }
}
