package com.csse3200.game.items.consumables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.statuseffects.MagnetEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.CurrencyItem;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemCategory;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MagnetPotionTest {
  /** 0.05s per frame at 6 units/s means items move 0.3 units each update. */
  private static final float STEP = MagnetPotion.PULL_SPEED * 0.05f;

  private final AtomicLong now = new AtomicLong();
  private GameTime time;
  private Entity player;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private StatusEffectsControllerComponent effects;
  private EntityService entityService;
  private Array<Entity> world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now.get());
    when(time.getDeltaTime()).thenReturn(0.05f);
    ServiceLocator.registerTimeSource(time);

    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    effects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables);
    effects.create();
    consumables.create();
    // Player sprite is 1x1, so its centre is (0.5, 0.5).
    player.setPosition(0f, 0f);

    world = new Array<>();
    world.add(player);
    entityService = mock(EntityService.class);
    when(entityService.getEntities()).thenReturn(world);
  }

  @Test
  void catalogCreatesMagnetPotionConsumable() {
    Item item = ItemCatalog.create(ItemIds.MAGNET_POTION, 2);
    MagnetPotion potion = assertInstanceOf(MagnetPotion.class, item);
    assertEquals(ItemCategory.CONSUMABLE, potion.getCategory());
    assertEquals(2, potion.getQuantity());
    assertEquals("images/magnet_potion_pixel.png", potion.getTexture());
  }

  @Test
  void rejectsNonPositiveDuration() {
    assertThrows(IllegalArgumentException.class, () -> new MagnetPotion(1, 0));
  }

  @Test
  void missingEntityServiceDoesNotConsumePotion() {
    inventory.addConsumable(ItemIds.MAGNET_POTION);
    assertFalse(consumables.tryUse(ItemIds.MAGNET_POTION));
    assertEquals(1, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
  }

  @Test
  void usingPotionConsumesOneAndStartsTenSecondGlowingMagnet() {
    ServiceLocator.registerEntityService(entityService);
    inventory.addConsumable(ItemIds.MAGNET_POTION, 2);

    assertTrue(consumables.tryUse(ItemIds.MAGNET_POTION));

    assertEquals(1, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    assertEquals(10000L, consumables.getRemainingMs(ItemIds.MAGNET_POTION));
    assertNotNull(effects.getGlow());
    now.set(10000L);
    assertEquals(0L, consumables.getRemainingMs(ItemIds.MAGNET_POTION));
  }

  @Test
  void pullsGoldInsideRadiusTowardsPlayer() {
    ServiceLocator.registerEntityService(entityService);
    Entity gold = itemAt(new CurrencyItem(5), 3f, 0f);
    MagnetEffect magnet = newMagnet();

    float before = distanceToPlayer(gold);
    magnet.update();

    assertEquals(before - STEP, distanceToPlayer(gold), 0.001f);
    assertEquals(0, inventory.getGold());
  }

  @Test
  void ignoresItemsOutsideRadiusAndCharms() {
    ServiceLocator.registerEntityService(entityService);
    Entity farGold = itemAt(new CurrencyItem(5), 10f, 0f);
    Entity charm = itemAt(new StrengthCharm(), 1f, 0f);
    Vector2 farBefore = farGold.getPosition().cpy();
    Vector2 charmBefore = charm.getPosition().cpy();

    newMagnet().update();

    assertEquals(farBefore, farGold.getPosition());
    assertEquals(charmBefore, charm.getPosition());
    verify(entityService, never()).scheduleDisposal(farGold);
    verify(entityService, never()).scheduleDisposal(charm);
  }

  @Test
  void collectsArrivingItemsExactlyOnce() {
    ServiceLocator.registerEntityService(entityService);
    Entity gold = itemAt(new CurrencyItem(5), 0.2f, 0f);
    Entity potion = itemAt(new SpeedPotion(1), 0f, 0.2f);
    MagnetEffect magnet = newMagnet();

    magnet.update();
    magnet.update();

    assertEquals(5, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.SPEED_POTION));
    verify(entityService, times(1)).scheduleDisposal(gold);
    verify(entityService, times(1)).scheduleDisposal(potion);
  }

  @Test
  void pulledItemIsEventuallyCollected() {
    ServiceLocator.registerEntityService(entityService);
    Entity gold = itemAt(new CurrencyItem(3), 3.5f, 0f);
    MagnetEffect magnet = newMagnet();

    for (int i = 0; i < 20; i++) {
      magnet.update();
    }

    assertEquals(3, inventory.getGold());
    verify(entityService, times(1)).scheduleDisposal(gold);
  }

  @Test
  void stopsPullingOnceExpired() {
    ServiceLocator.registerEntityService(entityService);
    Entity gold = itemAt(new CurrencyItem(5), 3f, 0f);
    Vector2 before = gold.getPosition().cpy();
    MagnetEffect magnet = newMagnet();

    now.set(MagnetPotion.DEFAULT_DURATION_MS);

    assertTrue(magnet.update());
    assertEquals(before, gold.getPosition());
  }

  private MagnetEffect newMagnet() {
    return new MagnetEffect(
        time,
        MagnetPotion.DEFAULT_DURATION_MS,
        player,
        MagnetPotion.RADIUS,
        MagnetPotion.PULL_SPEED,
        MagnetPotion.COLLECT_DISTANCE);
  }

  /** Places an item so its centre is at (x, y) relative to the player's centre. */
  private Entity itemAt(Item item, float x, float y) {
    Entity entity = new Entity().addComponent(new ItemComponent(item));
    Vector2 playerCentre = player.getCenterPosition();
    entity.setPosition(playerCentre.x + x - 0.5f, playerCentre.y + y - 0.5f);
    world.add(entity);
    return entity;
  }

  private float distanceToPlayer(Entity entity) {
    return entity.getCenterPosition().dst(player.getCenterPosition());
  }
}
