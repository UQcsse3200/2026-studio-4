package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FreezeBombUseTest {
  private final AtomicLong now = new AtomicLong();
  private Entity player;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now.get());
    when(time.getDeltaTime()).thenReturn(0.05f);
    ServiceLocator.registerTimeSource(time);
    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables);
    effects.create();
    consumables.create();
  }

  @Test
  void selectedBombFreezesOnlyVisibleEnemiesAndUsesOneItem() {
    Camera camera = new OrthographicCamera();
    camera.position.set(0f, 0f, 0f);
    camera.viewportWidth = 20f;
    camera.viewportHeight = 10f;
    ServiceLocator.registerWorldCamera(camera);

    Entity visible = enemyAt(2f, 1f);
    Entity edge = enemyAt(10f, 5f);
    Entity offscreen = enemyAt(11f, 0f);
    EntityService service = mock(EntityService.class);
    Array<Entity> world = new Array<>();
    world.addAll(player, visible, edge, offscreen);
    when(service.getEntities()).thenReturn(world);
    ServiceLocator.registerEntityService(service);
    RenderService renderService = new RenderService();
    ServiceLocator.registerRenderService(renderService);

    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    player.addComponent(selection).addComponent(input);
    selection.create();
    inventory.addConsumable(ItemType.FREEZE_BOMB, 2);
    for (int i = 0; i < 4; i++) {
      input.keyDown(Keys.TAB);
    }
    assertEquals(ItemType.FREEZE_BOMB, selection.getSelectedType());
    input.keyDown(Keys.Q);

    assertEquals(1, inventory.getConsumableCount(ItemType.FREEZE_BOMB));
    assertTrue(renderService.getWhiteFlashAlpha() > 0f);
    assertFrozenForThreeSeconds(visible);
    assertFrozenForThreeSeconds(edge);
    verify(offscreen.getComponent(StatusEffectsControllerComponent.class), never())
        .addStatusEffect(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void missingCameraDoesNotConsumeBomb() {
    inventory.addConsumable(ItemType.FREEZE_BOMB);

    assertFalse(consumables.tryUse(ItemType.FREEZE_BOMB));
    assertEquals(1, inventory.getConsumableCount(ItemType.FREEZE_BOMB));
  }

  private void assertFrozenForThreeSeconds(Entity enemy) {
    StatusEffectsControllerComponent effects =
        enemy.getComponent(StatusEffectsControllerComponent.class);
    ArgumentCaptor<StatusEffect> applied = ArgumentCaptor.forClass(StatusEffect.class);
    verify(effects).addStatusEffect(applied.capture());
    FrozenEffect frozen = assertInstanceOf(FrozenEffect.class, applied.getValue());
    assertTrue(frozen.immobilisesOwner());
    assertEquals(3000L, frozen.getRemainingDuration());
    now.set(3000L);
    assertTrue(frozen.isExpired());
    now.set(0L);
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
    when(enemy.getComponent(StatusEffectsControllerComponent.class))
        .thenReturn(mock(StatusEffectsControllerComponent.class));
    return enemy;
  }
}
