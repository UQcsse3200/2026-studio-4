package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class KnifeWeaponComponentTest {
  /** Wielder is 1x1, so half its extent is 0.5; the blade is 1.0 long, plus a 0.05 gap. */
  private static final float EXPECTED_REACH = 0.5f + 0.5f + 0.05f;

  private EntityService entityService;
  private GameTime gameTime;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);

    gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextures(new String[] {"images/weapons/knife.png"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  private Entity wielderWith(KnifeWeaponComponent knife) {
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(knife);
    wielder.create();
    return wielder;
  }

  private Entity attackAndCapture(Vector2 direction) {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    wielderWith(knife);
    assertTrue(knife.attack(new Vector2(0f, 0f), direction));

    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldSpawnFollowingHitboxWithoutASweep() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    assertNotNull(hitbox.getComponent(HitboxComponent.class));
    assertEquals(PhysicsLayer.WEAPON, hitbox.getComponent(HitboxComponent.class).getLayer());
    // A stab tracks the wielder, and unlike the sword it does not arc.
    assertNotNull(hitbox.getComponent(FollowComponent.class));
    assertNull(hitbox.getComponent(SweepComponent.class));
  }

  @Test
  void shouldSizeAndPlaceHitboxForAHorizontalAttack() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    assertEquals(new Vector2(1.0f, 0.5f), hitbox.getScale());
    assertEquals(
        new Vector2(EXPECTED_REACH, 0f),
        hitbox.getComponent(FollowComponent.class).getLocalOffset());
  }

  @Test
  void shouldTransposeHitboxForAVerticalAttack() {
    Entity hitbox = attackAndCapture(new Vector2(0f, 1f));

    // Blade length follows the attack axis, so the box is transposed when striking upward.
    assertEquals(new Vector2(0.5f, 1.0f), hitbox.getScale());
    assertEquals(
        new Vector2(0f, EXPECTED_REACH),
        hitbox.getComponent(FollowComponent.class).getLocalOffset());
  }

  @Test
  void shouldSnapOffsetToACardinalDirection() {
    // A mostly-rightward diagonal still stabs straight right rather than along the raw vector.
    Entity hitbox = attackAndCapture(new Vector2(1f, 0.4f));

    assertEquals(
        new Vector2(EXPECTED_REACH, 0f),
        hitbox.getComponent(FollowComponent.class).getLocalOffset());
  }

  @Test
  void shouldAttachSpriteFacingTheAttackDirection() {
    Entity hitbox = attackAndCapture(new Vector2(0f, -1f));
    RotatingTextureRenderComponent render =
        hitbox.getComponent(RotatingTextureRenderComponent.class);

    assertNotNull(render);
    // Striking downward faces 270 degrees; the offset corrects the sprite's own drawn angle.
    assertEquals(270f, render.getRotation());
    assertEquals(-135f, render.getRotationOffset());
    // Drawn square so the blade is not squashed into the oblong hitbox.
    assertEquals(new Vector2(0.8f, 0.8f), render.getVisualScale());
  }

  @Test
  void shouldScaleDamageByWielderBaseAttack() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    // round(baseAttack 10 * multiplier 0.8)
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  /**
   * Regression guard: a stab that outlives its own cooldown is still active when the next one
   * spawns, and an enemy entering the zone is hit by every live hitbox at once. The overlap grows
   * with attack-speed buffs, so the lifetime must stay under the resolved cooldown.
   */
  @Test
  void shouldExpireBeforeTheWielderCanAttackAgain() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = wielderWith(knife);
    float cooldown = wielder.getComponent(WeaponStatsComponent.class).resolveCooldown(1.0f);

    when(gameTime.getDeltaTime()).thenReturn(cooldown);
    assertTrue(knife.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    Entity hitbox = captor.getValue();

    // One cooldown's worth of time passes: the stab must already be gone.
    ServiceLocator.getEntityService().update();
    verify(entityService).unregister(hitbox);
  }
}
