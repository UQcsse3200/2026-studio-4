package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class SwordWeaponComponentTest {
  /** Wielder is 1x1, so half its extent is 0.5; the blade is 1.0 long, plus a 0.05 gap. */
  private static final float EXPECTED_REACH = 0.5f + 0.5f + 0.05f;

  private static final float ARC_DEGREES = 90f;
  private static final float LIFETIME = 0.5f;
  private static final float TOLERANCE = 1e-4f;

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
    resourceService.loadTextures(new String[] {"images/weapons/sword.png"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  private Entity attackAndCapture(Vector2 direction) {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(sword);
    wielder.create();
    assertTrue(sword.attack(new Vector2(0f, 0f), direction));

    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldSpawnASweepingHitboxThatFollowsTheWielder() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    assertNotNull(hitbox.getComponent(HitboxComponent.class));
    assertEquals(PhysicsLayer.WEAPON, hitbox.getComponent(HitboxComponent.class).getLayer());
    assertNotNull(hitbox.getComponent(FollowComponent.class));
    // Unlike the knife, the sword arcs around the wielder.
    assertNotNull(hitbox.getComponent(SweepComponent.class));
  }

  @Test
  void shouldSizeAndTransposeHitboxWithTheAttackAxis() {
    assertEquals(new Vector2(1.0f, 0.4f), attackAndCapture(new Vector2(1f, 0f)).getScale());
  }

  @Test
  void shouldStartTheArcHalfATurnBeforeTheAimDirection() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    // Aiming right, the 90-degree arc opens at -45 and closes at +45.
    Vector2 expected = new Vector2(EXPECTED_REACH, 0f).setAngleDeg(-ARC_DEGREES / 2f);
    Vector2 actual = hitbox.getComponent(FollowComponent.class).getLocalOffset();
    assertEquals(expected.x, actual.x, TOLERANCE);
    assertEquals(expected.y, actual.y, TOLERANCE);
  }

  @Test
  void shouldAimTheSpriteAlongTheStartOfTheArc() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));
    RotatingTextureRenderComponent render =
        hitbox.getComponent(RotatingTextureRenderComponent.class);

    assertNotNull(render);
    assertEquals(-ARC_DEGREES / 2f, render.getRotation(), TOLERANCE);
    assertEquals(-135f, render.getRotationOffset());
    // Drawn square so the blade keeps its shape rather than squashing into the 1.0 x 0.4 hitbox,
    // and pulled back along the swing so the handle sits at the wielder instead of mid-arc.
    assertEquals(new Vector2(1.0f, 1.0f), render.getVisualScale());
    assertEquals(new Vector2(-0.45f, 0f), render.getVisualOffset());
  }

  @Test
  void shouldTurnTheBladeAsTheSweepProgresses() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));
    RotatingTextureRenderComponent render =
        hitbox.getComponent(RotatingTextureRenderComponent.class);

    // Halfway through the swing the blade points at the aim direction, not the arc start.
    when(gameTime.getDeltaTime()).thenReturn(LIFETIME / 2f);
    hitbox.update();

    assertEquals(0f, render.getRotation(), TOLERANCE);

    // And it finishes at the far edge of the arc rather than overshooting it.
    when(gameTime.getDeltaTime()).thenReturn(LIFETIME);
    hitbox.update();

    assertEquals(ARC_DEGREES / 2f, render.getRotation(), TOLERANCE);
  }

  @Test
  void shouldScaleDamageByWielderBaseAttack() {
    Entity hitbox = attackAndCapture(new Vector2(1f, 0f));

    // round(baseAttack 10 * multiplier 0.8)
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }
}
