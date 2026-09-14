package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SwordWeaponComponentTest {
  /** Wielder is 1x1, so half its extent is 0.5; the blade is 1.0 long, plus a 0.05 gap. */
  private static final float EXPECTED_REACH = 0.5f + 0.5f + 0.05f;

  private static final float ARC_DEGREES = 90f;
  private static final float LIFETIME = 0.25f;
  private static final float HEAVY_LIFETIME = 0.8f;
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

    return captureRegisteredHitbox();
  }

  private Entity captureRegisteredHitbox() {
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    return captor.getValue();
  }

  /** Wielder with base attack 10, sword multiplier 0.8 and a {@link WeaponUpgradeComponent}. */
  private Entity createUpgradableWielder(SwordWeaponComponent sword, boolean upgraded) {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(upgrades)
            .addComponent(sword);
    wielder.create();
    upgrades.setUpgraded(SwordWeaponComponent.class, upgraded);
    return wielder;
  }

  @Test
  void shouldRenderSpawnedSwordWithLivePlayerAbilitiesAndExpiry() {
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(gameTime);
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity player =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(sword);
    player.create();
    assertTrue(sword.attack(new Vector2(), new Vector2(1f, 0f)));
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    RotatingTextureRenderComponent render =
        captor.getValue().getComponent(RotatingTextureRenderComponent.class);
    assertNotNull(render);

    SpriteBatch batch = mock(SpriteBatch.class);
    Color original = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    Color color = new Color(original);
    when(batch.getColor()).thenReturn(color);
    doAnswer(
            invocation -> {
              color.set(
                  invocation.getArgument(0), invocation.getArgument(1),
                  invocation.getArgument(2), invocation.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    List<Color> drawn = new ArrayList<>();
    doAnswer(
            invocation -> {
              drawn.add(new Color(color));
              return null;
            })
        .when(batch)
        .draw(
            any(TextureRegion.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());

    render.render(batch);
    assertEquals(original, color);
    // Activate after spawning so a snapshot of the player's appearance cannot pass.
    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    render.render(batch);
    assertEquals(original, color);
    assertTrue(abilities.tryActivate(Invisibility.class));
    render.render(batch);
    assertEquals(original, color);
    // Render directly, without an entity update frame, at each effect's deadline.
    when(gameTime.getTime()).thenReturn(LastStand.DURATION_MS);
    render.render(batch);
    assertEquals(original, color);
    when(gameTime.getTime()).thenReturn(Invisibility.DURATION_MS);
    render.render(batch);
    assertEquals(original, color);
    assertEquals(
        List.of(
            original,
            new Color(0.8f, 0.6f * 0.35f, 0.4f * 0.35f, 0.5f),
            new Color(0.8f, 0.6f * 0.35f, 0.4f * 0.35f, 0.5f * 0.35f),
            new Color(0.8f, 0.6f, 0.4f, 0.5f * 0.35f),
            original),
        drawn);
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

  @Test
  void shouldKeepSprintOneLightAttackWhenNotUpgraded() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    createUpgradableWielder(sword, false);

    assertTrue(sword.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity hitbox = captureRegisteredHitbox();

    assertEquals(new Vector2(1.0f, 0.4f), hitbox.getScale());
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldNotHeavyAttackWhenNotUpgraded() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity wielder = createUpgradableWielder(sword, false);

    assertFalse(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    verify(entityService, never()).register(any(Entity.class));
    assertEquals(0f, wielder.getComponent(WeaponStatsComponent.class).getRemainingCooldown());
  }

  @Test
  void shouldBoostLightDamageButKeepItsSweepWhenUpgraded() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    createUpgradableWielder(sword, true);

    assertTrue(sword.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity hitbox = captureRegisteredHitbox();

    // Same blade and 90-degree arc as the unupgraded sword...
    assertEquals(new Vector2(1.0f, 0.4f), hitbox.getScale());
    assertEquals(
        -ARC_DEGREES / 2f,
        hitbox.getComponent(RotatingTextureRenderComponent.class).getRotation(),
        TOLERANCE);
    // ...but round(baseAttack 10 * multiplier 0.8 * light upgrade 1.2) = round(9.6)
    assertEquals(10, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldHeavyAttackWithTheSameBladeWhenUpgraded() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    createUpgradableWielder(sword, true);

    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity hitbox = captureRegisteredHitbox();

    assertEquals(new Vector2(1.0f, 0.4f), hitbox.getScale());
    assertNotNull(hitbox.getComponent(FollowComponent.class));
    assertNotNull(hitbox.getComponent(SweepComponent.class));
  }

  @Test
  void shouldSpinTheHeavyAttackAFullTurnAroundTheWielder() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    createUpgradableWielder(sword, true);

    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity hitbox = captureRegisteredHitbox();
    RotatingTextureRenderComponent render =
        hitbox.getComponent(RotatingTextureRenderComponent.class);

    // Aiming right, the spin opens directly behind the wielder at -180...
    assertEquals(-180f, render.getRotation(), TOLERANCE);
    Vector2 expected = new Vector2(EXPECTED_REACH, 0f).setAngleDeg(-180f);
    Vector2 actual = hitbox.getComponent(FollowComponent.class).getLocalOffset();
    assertEquals(expected.x, actual.x, TOLERANCE);
    assertEquals(expected.y, actual.y, TOLERANCE);

    // ...passes through the aim direction halfway through its slower spin...
    when(gameTime.getDeltaTime()).thenReturn(HEAVY_LIFETIME / 2f);
    hitbox.update();
    assertEquals(0f, render.getRotation(), TOLERANCE);

    // ...and ends a full 360 degrees later, back behind the wielder.
    when(gameTime.getDeltaTime()).thenReturn(HEAVY_LIFETIME);
    hitbox.update();
    assertEquals(180f, render.getRotation(), TOLERANCE);
  }

  @Test
  void shouldSpinTheHeavyAttackSlowerThanTheLightSweep() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity wielder = createUpgradableWielder(sword, true);

    assertTrue(sword.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity light = captureRegisteredHitbox();
    wielder.getComponent(WeaponStatsComponent.class).update(10f);
    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(2)).register(captor.capture());
    Entity heavy = captor.getAllValues().get(1);

    // After the light sweep's full lifetime it has finished its arc...
    when(gameTime.getDeltaTime()).thenReturn(LIFETIME);
    light.update();
    heavy.update();
    assertEquals(
        ARC_DEGREES / 2f,
        light.getComponent(RotatingTextureRenderComponent.class).getRotation(),
        TOLERANCE);
    // ...while the heavy spin is still well short of its end.
    float heavyProgress = LIFETIME / HEAVY_LIFETIME;
    assertEquals(
        -180f + 360f * heavyProgress,
        heavy.getComponent(RotatingTextureRenderComponent.class).getRotation(),
        TOLERANCE);
  }

  @Test
  void shouldKeepHeavyCooldownAtLeastAsLongAsTheSpinWithAttackSpeedBuffs() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10, 3f, 4f)) // attack speed 4x
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(upgrades)
            .addComponent(sword);
    wielder.create();
    upgrades.setUpgraded(SwordWeaponComponent.class, true);

    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    // 0.5s / 4 * 2 = 0.25s would let a second spin start mid-spin; clamp to the 0.8s spin.
    assertEquals(
        HEAVY_LIFETIME,
        wielder.getComponent(WeaponStatsComponent.class).getRemainingCooldown(),
        TOLERANCE);
  }

  @Test
  void shouldDealHeavyUpgradeDamage() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    createUpgradableWielder(sword, true);

    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity hitbox = captureRegisteredHitbox();

    // round(baseAttack 10 * multiplier 0.8 * heavy upgrade 1.35) = round(10.8)
    assertEquals(11, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldLengthenTheSharedCooldownAfterAHeavyAttack() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity wielder = createUpgradableWielder(sword, true);
    WeaponStatsComponent stats = wielder.getComponent(WeaponStatsComponent.class);

    assertTrue(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    // 0.5s cooldown doubled by the heavy attack.
    assertEquals(1.0f, stats.getRemainingCooldown(), TOLERANCE);
    // The cooldown is shared, so the light attack waits too.
    stats.update(0.5f);
    assertFalse(sword.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
  }

  @Test
  void shouldRevertToSprintOneSwordWhenUpgradeRemoved() {
    SwordWeaponComponent sword = new SwordWeaponComponent();
    Entity wielder = createUpgradableWielder(sword, true);
    wielder
        .getComponent(WeaponUpgradeComponent.class)
        .setUpgraded(SwordWeaponComponent.class, false);

    assertFalse(sword.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertTrue(sword.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    Entity hitbox = captureRegisteredHitbox();
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }
}
