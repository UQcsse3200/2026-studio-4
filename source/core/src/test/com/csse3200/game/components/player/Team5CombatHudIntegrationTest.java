package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Integration coverage for Team 5's pickup, inventory, effect, and combat HUD boundaries. */
@ExtendWith(GameExtension.class)
class Team5CombatHudIntegrationTest {
  private static final String ITEM_PICKUP_EVENT = "itemPickup";
  private Stage stage;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());

    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
  }

  @AfterEach
  void afterEach() {
    stage.dispose();
  }

  @Test
  void shouldShowPickedUpConsumableQuantityOnHud() {
    Entity player = createPlayerWithHud();
    Entity potion = createTypedItem(ItemType.HEALTH_POTION, 3);

    pickUp(player, potion);

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    assertEquals(3, inventory.getConsumableCount(ItemType.HEALTH_POTION));
    assertHudContains("[1] Health x3");
  }

  @Test
  void shouldShowPickedUpGoldOnNextHudUpdate() {
    Entity player = createPlayerWithHud();
    Entity gold = createTypedItem(ItemType.GOLD_COIN, 25);

    pickUp(player, gold);
    player.update();

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
    assertHudContains("Gold: 25");
  }

  @Test
  void shouldRemoveHudActorBeforeReplacementPlayerIsCreated() {
    Entity firstPlayer = createPlayerWithHud();
    assertEquals(1, stage.getActors().size);

    firstPlayer.dispose();
    assertEquals(0, stage.getActors().size);

    Entity replacementPlayer = createPlayerWithHud();
    assertEquals(1, stage.getActors().size);

    replacementPlayer.dispose();
    assertEquals(0, stage.getActors().size);
  }

  @Test
  void shouldApplyAndRestoreStrengthThroughSharedPickupFlow() {
    Entity player = createPlayerWithHud();
    StrengthCharm charm = new StrengthCharm();
    Entity charmEntity = createCharmItem(charm);

    pickUp(player, charmEntity);

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    assertTrue(inventory.hasCharm(charm));
    assertEquals(18, stats.getBaseAttack());

    charm.drop(player);

    assertEquals(0, inventory.getCharmCount());
    assertEquals(8, stats.getBaseAttack());
  }

  private Entity createPlayerWithHud() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 8))
            .addComponent(new InventoryComponent(0))
            .addComponent(new ItemPickupComponent())
            .addComponent(new Team5CombatHudDisplay());
    player.create();
    return player;
  }

  private Entity createTypedItem(ItemType itemType, int quantity) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(new ItemDropSpec(itemType, quantity)));
    item.create();
    return item;
  }

  private Entity createCharmItem(StrengthCharm charm) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(charm));
    item.create();
    return item;
  }

  private void pickUp(Entity player, Entity item) {
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = item.getComponent(HitboxComponent.class).getFixture();

    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    player.getEvents().trigger(ITEM_PICKUP_EVENT);
  }

  private void assertHudContains(String expectedText) {
    assertTrue(
        containsLabel(stage.getRoot(), expectedText),
        () -> String.format("Expected HUD label '%s'", expectedText));
  }

  private boolean containsLabel(Actor actor, String expectedText) {
    if (actor instanceof Label label && expectedText.contentEquals(label.getText())) {
      return true;
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        if (containsLabel(child, expectedText)) {
          return true;
        }
      }
    }
    return false;
  }
}
