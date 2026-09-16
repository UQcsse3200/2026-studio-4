package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.player.*;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.EnemyDropPolicy;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;
import java.util.random.RandomGenerator;
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
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    for (ItemType itemType : ItemType.values()) {
      when(resourceService.getAsset(itemType.getTexturePath(), Texture.class)).thenReturn(texture);
    }
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);

    room = createMockRoom();
    enemyManager =
        new EnemyManagerComponent(
            new EnemySpawnConfig[0], new EnemyDropPolicy(5, 0, new Random(1)));
    enemyManager.setEntity(room);
    enemyManager.create();
  }

  /** Registers n enemies with the manager and returns them. */
  private Entity[] trackEnemies(int n) {
    Entity[] enemies = new Entity[n];
    for (int i = 0; i < n; i++) {
      Entity enemy = mock(Entity.class);
      when(enemy.getEvents()).thenReturn(new EventHandler());
      when(enemy.getPosition()).thenReturn(new Vector2());
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
    assertFalse(enemyManager.isCleared());
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
    assertTrue(enemyManager.isCleared());
  }

  @Test
  void shouldRegisterGoldAtCapturedDefeatedEnemyPosition() {
    Vector2 deathPosition = new Vector2(4f, 6f);
    Entity enemy = new Entity();
    enemy.setPosition(deathPosition);
    enemyManager.track(enemy);

    enemy.getEvents().trigger("entityDied");
    enemy.setPosition(20, 20);
    verify(entityService, never()).register(Mockito.any(Entity.class));

    entityService.update();

    ArgumentCaptor<Entity> dropCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(1)).register(dropCaptor.capture());
    Entity drop = dropCaptor.getValue();
    ItemComponent item = drop.getComponent(ItemComponent.class);

    assertEquals(deathPosition, drop.getPosition());
    assertNotNull(item);
    assertEquals(ItemType.GOLD_COIN, item.getItemType());
    assertEquals(5, item.getQuantity());
    assertEquals(PhysicsLayer.ITEM, drop.getComponent(HitboxComponent.class).getLayer());

    entityService.update();
    verify(entityService, times(1)).register(Mockito.any(Entity.class));
  }

  @Test
  void shouldRegisterGoldAndConsumableOnceDespiteRepeatedDeathAndTracking() {
    enemyManager =
        new EnemyManagerComponent(
            new EnemySpawnConfig[0], new EnemyDropPolicy(7, 1, new Random(2)));
    enemyManager.setEntity(room);
    enemyManager.create();
    Entity enemy = new Entity();
    enemyManager.track(enemy);
    enemyManager.track(enemy);
    enemy.getEvents().trigger("entityDied");
    enemy.getEvents().trigger("entityDied");
    entityService.update();
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(2)).register(captor.capture());
    assertEquals(
        ItemType.GOLD_COIN,
        captor.getAllValues().get(0).getComponent(ItemComponent.class).getItemType());
    assertEquals(7, captor.getAllValues().get(0).getComponent(ItemComponent.class).getQuantity());
    assertTrue(
        captor
            .getAllValues()
            .get(1)
            .getComponent(ItemComponent.class)
            .getItemType()
            .isConsumable());
    enemyManager.dispose();
    for (Entity drop : captor.getAllValues()) {
      verify(entityService).unregister(drop);
    }
  }

  @Test
  void shouldNotSpawnQueuedRewardsAfterOwningRoomIsDisposed() {
    Entity enemy = new Entity();
    enemyManager.track(enemy);
    enemy.getEvents().trigger("entityDied");
    enemyManager.dispose();
    entityService.update();
    enemy.getEvents().trigger("entityDied");
    entityService.update();
    verify(entityService, never()).register(Mockito.any(Entity.class));
  }

  @Test
  void shouldReplaceSplitParentWithTrackedChildren() {
    Entity parent = enemyMock();
    Entity firstChild = enemyMock();
    Entity secondChild = enemyMock();
    int[] cleared = {0};
    room.getEvents().addListener("roomCleared", () -> cleared[0]++);
    enemyManager.track(parent);

    parent.getEvents().trigger("spawnChildren", firstChild);
    parent.getEvents().trigger("spawnChildren", secondChild);
    firstChild.getEvents().trigger("entityDied");

    assertFalse(enemyManager.isCleared());
    assertEquals(0, cleared[0]);
    secondChild.getEvents().trigger("entityDied");
    assertTrue(enemyManager.isCleared());
    assertEquals(1, cleared[0]);
    verify(entityService).register(firstChild);
    verify(entityService).register(secondChild);
  }

  @Test
  void shouldClearEveryLivingEnemy() {
    Entity first = combatEnemy();
    Entity second = combatEnemy();
    enemyManager.track(first);
    enemyManager.track(second);

    enemyManager.clear();

    assertTrue(first.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(second.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(enemyManager.isCleared());
  }

  @Test
  void enemyRewardFlowsThroughPickupHudKeyboardAndEffect() {
    Stage stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    when(ServiceLocator.getRenderService().getStage()).thenReturn(stage);
    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(4)).thenReturn(0);
    enemyManager =
        new EnemyManagerComponent(new EnemySpawnConfig[0], new EnemyDropPolicy(5, 1, random));
    enemyManager.setEntity(room);
    enemyManager.create();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new InventoryComponent(0))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new ConsumableEffectComponent())
            .addComponent(new ConsumableLoadoutComponent())
            .addComponent(new KeyboardPlayerInputComponent())
            .addComponent(new ItemPickupComponent())
            .addComponent(new Team5CombatHudDisplay());
    player.create();
    player.getComponent(CombatStatsComponent.class).setHealth(50);
    Entity enemy = new Entity();
    enemyManager.track(enemy);
    enemy.getEvents().trigger("entityDied");
    entityService.update();
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(2)).register(captor.capture());
    for (Entity drop : captor.getAllValues()) {
      player
          .getEvents()
          .trigger(
              "collisionStart",
              player.getComponent(HitboxComponent.class).getFixture(),
              drop.getComponent(HitboxComponent.class).getFixture());
      player.getComponent(KeyboardPlayerInputComponent.class).keyDown(Keys.E);
    }
    player.update();
    assertEquals(5, player.getComponent(InventoryComponent.class).getGold());
    assertTrue(hasLabel(stage.getRoot(), "Gold: 5"));
    assertTrue(hasLabel(stage.getRoot(), "[8] Health x1"));
    player.getComponent(KeyboardPlayerInputComponent.class).keyDown(Keys.NUM_8);
    assertEquals(75, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(hasLabel(stage.getRoot(), "[8] Health x0"));
    assertTrue(hasLabel(stage.getRoot(), "Last used: Health"));
    enemyManager.dispose();
    player.dispose();
    assertEquals(0, stage.getActors().size);
    stage.dispose();
  }

  private static boolean hasLabel(Actor actor, String text) {
    if (actor instanceof Label label && text.contentEquals(label.getText())) {
      return true;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        if (hasLabel(child, text)) {
          return true;
        }
      }
    }
    return false;
  }

  private Entity combatEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 1));
    entityService.register(enemy);
    return enemy;
  }

  private static Entity enemyMock() {
    Entity enemy = mock(Entity.class);
    when(enemy.getEvents()).thenReturn(new EventHandler());
    when(enemy.getPosition()).thenReturn(new Vector2());
    when(enemy.getCenterPosition()).thenReturn(new Vector2());
    return enemy;
  }
}
