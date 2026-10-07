package com.csse3200.game.items.consumables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Covers {@link BurnVial}, which reuses the existing {@link Burning} status effect rather than a
 * new damage-over-time system, following the same on-screen-targeting pattern as {@link
 * FreezeBomb}.
 */
@ExtendWith(GameExtension.class)
class BurnVialTest {
  private Entity caster;
  private CombatStatsComponent casterStats;

  @BeforeEach
  void setUp() {
    casterStats = mock(CombatStatsComponent.class);
    caster = mock(Entity.class);
    when(casterStats.getEntity()).thenReturn(caster);
  }

  @Test
  void isRegisteredUnderItsOwnStableId() {
    BurnVial vial = (BurnVial) ItemCatalog.create(ItemIds.BURN_VIAL, 2);
    assertEquals(ItemIds.BURN_VIAL, vial.getId());
    assertEquals(2, vial.getQuantity());
  }

  @Test
  void cannotBeUsedWithoutAWorldCameraOrEntityService() {
    BurnVial vial = new BurnVial(1);
    assertFalse(
        vial.canUse(
            casterStats, mock(StatusEffectsControllerComponent.class), mock(GameTime.class)));
  }

  @Test
  void burnsOnlyVisibleEnemiesAndLeavesPlayerEffectsAlone() {
    Camera camera = new OrthographicCamera();
    camera.position.set(0f, 0f, 0f);
    camera.viewportWidth = 20f;
    camera.viewportHeight = 10f;
    ServiceLocator.registerWorldCamera(camera);

    EntityService service = mock(EntityService.class);
    Entity visible = enemyAt(2f, 1f);
    Entity offscreen = enemyAt(11f, 0f);
    Array<Entity> world = new Array<>();
    world.addAll(caster, visible, offscreen);
    when(service.getEntities()).thenReturn(world);
    ServiceLocator.registerEntityService(service);
    RenderService renderService = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderService);

    BurnVial vial = new BurnVial(1, 6000);
    assertTrue(
        vial.canUse(
            casterStats, mock(StatusEffectsControllerComponent.class), mock(GameTime.class)));
    assertNull(vial.use(casterStats, mock(GameTime.class)));

    StatusEffectsControllerComponent visibleEffects =
        visible.getComponent(StatusEffectsControllerComponent.class);
    ArgumentCaptor<StatusEffect> applied = ArgumentCaptor.forClass(StatusEffect.class);
    verify(visibleEffects).addStatusEffect(applied.capture());
    Burning burn = assertInstanceOf(Burning.class, applied.getValue());
    assertTrue(
        Math.abs(6000L - burn.getRemainingDuration()) < 200L,
        "expected remaining duration close to 6000ms, was " + burn.getRemainingDuration());

    verify(offscreen.getComponent(StatusEffectsControllerComponent.class), never())
        .addStatusEffect(org.mockito.ArgumentMatchers.any());
    verify(renderService).startWhiteFlash();
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
