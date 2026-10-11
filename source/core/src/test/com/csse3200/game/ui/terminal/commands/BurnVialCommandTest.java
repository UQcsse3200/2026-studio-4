package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BurnVialCommandTest {
  private Entity player;
  private InventoryComponent inventory;
  private BurnVialCommand command;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    inventory = new InventoryComponent(0);
    ConsumableEffectComponent consumables = new ConsumableEffectComponent();
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables);
    effects.create();
    consumables.create();
    command = new BurnVialCommand(player);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  @Test
  void grantsAndImmediatelyBurnsOnlyVisibleEnemies() {
    Camera camera = new OrthographicCamera();
    camera.position.set(0f, 0f, 0f);
    camera.viewportWidth = 20f;
    camera.viewportHeight = 10f;
    ServiceLocator.registerWorldCamera(camera);

    Entity visible = enemyAt(2f, 1f);
    Entity offscreen = enemyAt(11f, 0f);
    EntityService service = mock(EntityService.class);
    Array<Entity> world = new Array<>();
    world.addAll(player, visible, offscreen);
    when(service.getEntities()).thenReturn(world);
    ServiceLocator.registerEntityService(service);
    ServiceLocator.registerRenderService(new RenderService());

    assertTrue(command.action(args()));

    // Consumed immediately, so the player ends up holding none.
    assertEquals(0, inventory.getConsumableCount(ItemIds.BURN_VIAL));

    StatusEffectsControllerComponent visibleEffects =
        visible.getComponent(StatusEffectsControllerComponent.class);
    ArgumentCaptor<StatusEffect> applied = ArgumentCaptor.forClass(StatusEffect.class);
    verify(visibleEffects).addStatusEffect(applied.capture());
    assertInstanceOf(Burning.class, applied.getValue());

    verify(offscreen.getComponent(StatusEffectsControllerComponent.class), never())
        .addStatusEffect(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void giveLeavesOneVialInAnExistingSlotWithoutUsingIt() {
    assertTrue(command.action(args("give")));
    assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    assertEquals(ItemIds.BURN_VIAL, inventory.getConsumableSlot(0));
  }

  @Test
  void rejectsArguments() {
    assertFalse(command.action(args("extra")));
    assertEquals(0, inventory.getConsumableCount(ItemIds.BURN_VIAL));
  }

  @Test
  void failsWhenPlayerHasNoInventoryComponent() {
    Entity bareEntity = new Entity();
    bareEntity.create();

    assertFalse(new BurnVialCommand(bareEntity).action(args()));
  }

  private static Entity enemyAt(float x, float y) {
    Entity enemy = mock(Entity.class);
    when(enemy.getCenterPosition()).thenAnswer(invocation -> new Vector2(x, y));
    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.NPC;
    Fixture fixture = mock(Fixture.class);
    when(fixture.getFilterData()).thenReturn(filter);
    HitboxComponent hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(fixture);
    when(enemy.getComponent(HitboxComponent.class)).thenReturn(hitbox);
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    when(enemy.getComponent(CombatStatsComponent.class)).thenReturn(stats);
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    when(enemy.getComponent(StatusEffectsControllerComponent.class)).thenReturn(effects);
    return enemy;
  }
}
