package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.ProjectileComponent;
import com.csse3200.game.components.weapons.SweepComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Verifies shop fulfillment reaches the real combat hitboxes, rather than only an upgrade flag. */
@ExtendWith(GameExtension.class)
class ShopWeaponCombatIntegrationTest {
  private EntityService entities;
  private PhysicsService physics;
  private ResourceService resources;
  private GameTime time;
  private final List<Entity> players = new ArrayList<>();

  @BeforeEach
  void setUpCombat() {
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    RenderService render = new RenderService();
    render.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(render);
    resources = new ResourceService();
    resources.loadTextures(
        new String[] {
          SwordWeaponComponent.TEXTURE, SwordWeaponComponent.UPGRADED_TEXTURE,
          KnifeWeaponComponent.TEXTURE, KnifeWeaponComponent.UPGRADED_TEXTURE,
          BowWeaponComponent.TEXTURE, BowWeaponComponent.UPGRADED_TEXTURE
        });
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
  }

  @AfterEach
  void disposeCombat() {
    for (Entity hitbox : entities.getEntities().toArray(Entity.class)) hitbox.dispose();
    for (Entity player : players) player.dispose();
    resources.dispose();
    physics.getPhysics().dispose();
  }

  @Test
  void swordPurchaseRaisesSpawnedLightHitAndEnemyHealthLoss() {
    assertLightDamageIncreases("sword", WeaponType.SWORD);
  }

  @Test
  void knifePurchaseRaisesSpawnedLightHitAndEnemyHealthLoss() {
    assertLightDamageIncreases("knife", WeaponType.DAGGER);
  }

  @Test
  void bowPurchaseRaisesSpawnedLightHitAndEnemyHealthLoss() {
    assertLightDamageIncreases("bow", WeaponType.BOW);
  }

  @Test
  void swordPurchaseUnlocksHeavySweepWithRealDamageAndCooldown() {
    PlayerCombat combat = createPlayer(WeaponType.SWORD);
    assertHeavyLocked(combat);
    assertEquals(ShopPurchaseResult.SUCCESS, combat.shop().purchase("sword"));
    combat.player().getEvents().trigger("weaponHeavyAttack", new Vector2(1f, 0f));
    assertEquals(1, entities.getEntities().size);
    assertNotNull(lastHitbox().getComponent(SweepComponent.class));
    assertHitDeals(lastHitbox(), 14);
    assertEquals(1f, combat.stats().getRemainingCooldown(), 0.0001f);
    combat.player().getEvents().trigger("weaponHeavyAttack", new Vector2(1f, 0f));
    assertEquals(1, entities.getEntities().size);
  }

  @Test
  void knifePurchaseUnlocksTwoSlashesAndFinisherWithRealDamageAndCooldown() {
    PlayerCombat combat = createPlayer(WeaponType.DAGGER);
    assertHeavyLocked(combat);
    assertEquals(ShopPurchaseResult.SUCCESS, combat.shop().purchase("knife"));
    combat.player().getEvents().trigger("weaponHeavyAttack", new Vector2(1f, 0f));
    assertEquals(1, entities.getEntities().size);
    assertNotNull(lastHitbox().getComponent(SweepComponent.class));
    assertHitDeals(lastHitbox(), 6);
    assertEquals(1.5f, combat.stats().getRemainingCooldown(), 0.0001f);
    KnifeWeaponComponent knife = combat.player().getComponent(KnifeWeaponComponent.class);
    when(time.getDeltaTime()).thenReturn(0.15f);
    knife.update();
    assertEquals(2, entities.getEntities().size);
    assertNotNull(lastHitbox().getComponent(SweepComponent.class));
    assertHitDeals(lastHitbox(), 6);
    knife.update();
    assertEquals(3, entities.getEntities().size);
    assertNull(lastHitbox().getComponent(SweepComponent.class));
    assertHitDeals(lastHitbox(), 12);
    knife.update();
    assertEquals(3, entities.getEntities().size);
  }

  @Test
  void bowPurchaseUnlocksThreeRealHeavyProjectilesWithSharedCooldown() {
    PlayerCombat combat = createPlayer(WeaponType.BOW);
    assertHeavyLocked(combat);
    assertEquals(ShopPurchaseResult.SUCCESS, combat.shop().purchase("bow"));
    combat.player().getEvents().trigger("weaponHeavyAttack", new Vector2(1f, 0f));
    assertEquals(3, entities.getEntities().size);
    for (Entity arrow : entities.getEntities()) {
      assertNotNull(arrow.getComponent(ProjectileComponent.class));
      assertHitDeals(arrow, 10);
    }
    assertEquals(1f, combat.stats().getRemainingCooldown(), 0.0001f);
    combat.player().getEvents().trigger("weaponAttack", new Vector2(1f, 0f));
    assertEquals(3, entities.getEntities().size);
  }

  private void assertLightDamageIncreases(String offerId, WeaponType type) {
    PlayerCombat combat = createPlayer(type);
    combat.player().getEvents().trigger("weaponAttack", new Vector2(1f, 0f));
    assertEquals(1, entities.getEntities().size);
    assertHitDeals(lastHitbox(), 10);
    combat.stats().update(0.5f);
    assertEquals(ShopPurchaseResult.SUCCESS, combat.shop().purchase(offerId));
    assertEquals(40, combat.inventory().getGold());
    combat.player().getEvents().trigger("weaponAttack", new Vector2(1f, 0f));
    assertEquals(2, entities.getEntities().size);
    assertHitDeals(lastHitbox(), 12);
    assertEquals(0.5f, combat.stats().getRemainingCooldown(), 0.0001f);
  }

  private void assertHeavyLocked(PlayerCombat combat) {
    combat.player().getEvents().trigger("weaponHeavyAttack", new Vector2(1f, 0f));
    assertEquals(0, entities.getEntities().size);
    assertEquals(0f, combat.stats().getRemainingCooldown());
  }

  private PlayerCombat createPlayer(WeaponType type) {
    InventoryComponent inventory = new InventoryComponent(100);
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 2f);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    WeaponSelectionComponent selection = new WeaponSelectionComponent();
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(120, 10, 3f, 1f))
            .addComponent(inventory)
            .addComponent(stats)
            .addComponent(upgrades)
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(selection);
    players.add(player);
    player.create();
    assertTrue(selection.equip(type));
    ShopService shop =
        new ShopService(
            inventory,
            WeaponUpgradeCatalog.load("configs/shops/merchant-upgrades.json").catalog(),
            Map.of(ShopProductKind.WEAPON_UPGRADE, new WeaponUpgradePurchaseEffect(upgrades)));
    return new PlayerCombat(player, inventory, stats, shop);
  }

  private Entity lastHitbox() {
    return entities.getEntities().peek();
  }

  private void assertHitDeals(Entity hitbox, int expectedDamage) {
    assertEquals(expectedDamage, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(100, 0));
    enemy.create();
    try {
      hitbox
          .getEvents()
          .trigger(
              "collisionStart",
              hitbox.getComponent(HitboxComponent.class).getFixture(),
              enemy.getComponent(HitboxComponent.class).getFixture());
      assertEquals(
          100 - expectedDamage, enemy.getComponent(CombatStatsComponent.class).getHealth());
    } finally {
      enemy.dispose();
    }
  }

  private record PlayerCombat(
      Entity player, InventoryComponent inventory, WeaponStatsComponent stats, ShopService shop) {}
}
