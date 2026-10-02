package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.entities.factories.FriendlyNpcFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class FriendlyNpcManagerComponentTest {
  private ResourceService resources;
  private EntityService entityService;
  private Entity room;
  private Entity player;
  private InteractableNpcConfigs definitions;

  @BeforeEach
  void setUp() {
    resources = mock(ResourceService.class);
    entityService = mock(EntityService.class);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerEntityService(entityService);

    room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    when(room.getEvents()).thenReturn(new EventHandler());
    when(terrain.getTileSize()).thenReturn(1f);
    // EntityManagerComponent.spawnEntity rejects entities outside the map bounds, so the mocked
    // terrain needs a map large enough to hold every NPC these tests spawn.
    when(terrain.getMapBounds(0)).thenReturn(new GridPoint2(20, 20));
    when(terrain.tileToWorldPosition(any(GridPoint2.class)))
        .thenAnswer(
            inv -> {
              GridPoint2 tile = inv.getArgument(0);
              return new Vector2(tile.x, tile.y);
            });
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);

    player = new Entity().addComponent(new NpcInteractorComponent());
    player.create();

    definitions = new InteractableNpcConfigs();
    definitions.npcs = new InteractableNpcConfig[] {definition("near"), definition("far")};
  }

  @Test
  void roomWithoutNpcsLoadsNothing() {
    FriendlyNpcManagerComponent manager = new FriendlyNpcManagerComponent();
    verify(resources, never()).loadAll();
    assertTrue(manager.getNpcs().isEmpty());
  }

  @Test
  void rejectsUnknownNpcIds() {
    NpcSpawnConfig[] spawns = {spawn("missing", 0)};
    assertThrows(
        IllegalArgumentException.class, () -> new FriendlyNpcManagerComponent(spawns, definitions));
  }

  @Test
  void loadsEachAtlasOnce() {
    new FriendlyNpcManagerComponent(
        new NpcSpawnConfig[] {spawn("near", 0), spawn("far", 5)}, definitions);
    verify(resources).loadTextureAtlases(new String[] {"images/ghost.atlas"});
    verify(resources).loadAll();
  }

  @Test
  void spawnsOnRoomCreatedAndFindsNearestInRange() {
    FriendlyNpcManagerComponent manager = spawnedManager();

    assertEquals(2, manager.getNpcs().size());
    verify(entityService, org.mockito.Mockito.times(2)).register(any(Entity.class));

    player.setPosition(1f, 0f);
    NpcInteractableComponent nearest = manager.findNearestInRange(player);
    assertSame(manager.getNpcs().get(0), nearest);
    assertEquals("Press E — Talk to near", manager.getPrompt(player));

    player.setPosition(20f, 0f);
    assertNull(manager.findNearestInRange(player));
    assertNull(manager.getPrompt(player));
  }

  @Test
  void showsIndicatorOnlyOnNearestAvailableNpc() {
    FriendlyNpcManagerComponent manager = spawnedManager();
    NpcInteractableComponent near = manager.getNpcs().get(0);
    NpcInteractableComponent far = manager.getNpcs().get(1);

    player.setPosition(1f, 0f);
    manager.update();
    assertTrue(near.isIndicatorVisible());
    assertFalse(far.isIndicatorVisible());

    player.setPosition(20f, 0f);
    manager.update();
    assertFalse(near.isIndicatorVisible());
  }

  @Test
  void roomClearedConditionReadsTheEnemyManager() {
    definitions.npcs[0].requiresRoomCleared = true;
    EnemyManagerComponent enemies = new EnemyManagerComponent();
    enemies.setEntity(room);
    when(room.getComponent(EnemyManagerComponent.class)).thenReturn(enemies);
    Entity enemy = mock(Entity.class);
    when(enemy.getEvents()).thenReturn(new EventHandler());
    when(enemy.getPosition()).thenReturn(new Vector2());
    enemies.track(enemy);
    FriendlyNpcManagerComponent manager = spawnedManager();
    NpcInteractableComponent near = manager.getNpcs().get(0);

    assertEquals(
        NpcInteractableComponent.Availability.CONDITIONS_UNMET, near.getAvailability(player));

    enemy.getEvents().trigger("entityDied");
    assertEquals(NpcInteractableComponent.Availability.AVAILABLE, near.getAvailability(player));
  }

  @Test
  void disposeUnloadsAssets() {
    FriendlyNpcManagerComponent manager = spawnedManager();
    manager.dispose();
    verify(resources).unloadAssets(new String[] {"images/ghost.atlas"});
    assertTrue(manager.getNpcs().isEmpty());
  }

  private FriendlyNpcManagerComponent spawnedManager() {
    FriendlyNpcManagerComponent manager =
        new FriendlyNpcManagerComponent(
            new NpcSpawnConfig[] {spawn("near", 0), spawn("far", 10)}, definitions);
    manager.setEntity(room);
    manager.create();
    try (MockedStatic<FriendlyNpcFactory> factory = mockStatic(FriendlyNpcFactory.class)) {
      factory
          .when(() -> FriendlyNpcFactory.createFriendlyNpc(any(InteractableNpcConfig.class)))
          .thenAnswer(
              inv ->
                  new Entity()
                      .addComponent(
                          new NpcInteractableComponent(
                              (InteractableNpcConfig) inv.getArgument(0))));
      room.getEvents().trigger("RoomCreated", player);
    }
    return manager;
  }

  private static InteractableNpcConfig definition(String id) {
    InteractableNpcConfig config = new InteractableNpcConfig();
    config.id = id;
    config.name = id;
    config.dialogueId = id + "_dialogue";
    config.atlas = "images/ghost.atlas";
    config.animation = "float";
    config.interactionRange = 1.5f;
    return config;
  }

  private static NpcSpawnConfig spawn(String npcId, int x) {
    NpcSpawnConfig spawn = new NpcSpawnConfig();
    spawn.npcId = npcId;
    spawn.x = x;
    spawn.y = 0;
    return spawn;
  }
}
