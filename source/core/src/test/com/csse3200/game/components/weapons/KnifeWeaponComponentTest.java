package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    resourceService.loadTextures(
        new String[] {KnifeWeaponComponent.TEXTURE, KnifeWeaponComponent.UPGRADED_TEXTURE});
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
  void shouldRenderSpawnedKnifeWithLivePlayerAbilitiesAndExpiry() {
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(gameTime);
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity player =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 2f))
            .addComponent(knife);
    player.create();
    assertTrue(knife.attack(new Vector2(), new Vector2(1f, 0f)));
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

  // Heavy flurry. Wielder: base attack 10, knife multiplier 1, 0.5s cooldown, knockback 2.
  private static final float SLASH_LIFETIME = 0.12f;
  private static final float STRIKE_INTERVAL = 0.15f;
  private static final float HALF_SLASH_ARC = 35f;
  private static final float TOLERANCE = 1e-4f;

  private Entity upgradableWielder(
      KnifeWeaponComponent knife, boolean upgraded, float attackSpeed) {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10, 3f, attackSpeed))
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(upgrades)
            .addComponent(knife);
    wielder.create();
    upgrades.setUpgraded(KnifeWeaponComponent.class, upgraded);
    return wielder;
  }

  private List<Entity> registeredHitboxes(int expected) {
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(expected)).register(captor.capture());
    return captor.getAllValues();
  }

  /** Advance the flurry's queue by one strike interval. */
  private void tickStrikeInterval(KnifeWeaponComponent knife) {
    when(gameTime.getDeltaTime()).thenReturn(STRIKE_INTERVAL);
    knife.update();
  }

  private static int damageOf(Entity hitbox) {
    return hitbox.getComponent(CombatStatsComponent.class).getBaseAttack();
  }

  private static float knockbackOf(Entity hitbox) {
    return hitbox.getComponent(TouchAttackComponent.class).getKnockbackForce();
  }

  private static float rotationOf(Entity hitbox) {
    return hitbox.getComponent(RotatingTextureRenderComponent.class).getRotation();
  }

  @Test
  void shouldNotFlurryWhenNotUpgraded() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = upgradableWielder(knife, false, 1f);

    assertFalse(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    tickStrikeInterval(knife);
    tickStrikeInterval(knife);

    verify(entityService, never()).register(any(Entity.class));
    assertEquals(0f, wielder.getComponent(WeaponStatsComponent.class).getRemainingCooldown());
  }

  @Test
  void shouldBoostTheLightStabWhenUpgraded() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, true, 1f);

    assertTrue(knife.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity stab = registeredHitboxes(1).get(0);

    assertEquals(new Vector2(1.0f, 0.5f), stab.getScale());
    assertNull(stab.getComponent(SweepComponent.class));
    assertEquals(12, damageOf(stab)); // round(10 * 1 * 1.2)
    assertEquals(2f, knockbackOf(stab));
  }

  @Test
  void shouldOpenTheFlurryWithAKnockbackFreeSlashRightToLeft() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, true, 1f);

    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    Entity slash = registeredHitboxes(1).get(0);

    assertNotNull(slash.getComponent(SweepComponent.class));
    assertEquals(new Vector2(1.0f, 0.5f), slash.getScale());
    assertEquals(6, damageOf(slash)); // round(10 * 1 * 0.6)
    assertEquals(0f, knockbackOf(slash));

    // Aiming right, it swings from +35 degrees down to -35 degrees.
    assertEquals(HALF_SLASH_ARC, rotationOf(slash), TOLERANCE);
    when(gameTime.getDeltaTime()).thenReturn(SLASH_LIFETIME);
    slash.update();
    assertEquals(-HALF_SLASH_ARC, rotationOf(slash), TOLERANCE);
  }

  @Test
  void shouldFollowWithAReverseSlashThenAFinishingStab() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, true, 1f);
    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    tickStrikeInterval(knife);
    Entity secondSlash = registeredHitboxes(2).get(1);
    assertNotNull(secondSlash.getComponent(SweepComponent.class));
    assertEquals(6, damageOf(secondSlash));
    assertEquals(0f, knockbackOf(secondSlash));
    // Swings back the other way: -35 up to +35.
    assertEquals(-HALF_SLASH_ARC, rotationOf(secondSlash), TOLERANCE);
    when(gameTime.getDeltaTime()).thenReturn(SLASH_LIFETIME);
    secondSlash.update();
    assertEquals(HALF_SLASH_ARC, rotationOf(secondSlash), TOLERANCE);

    tickStrikeInterval(knife);
    Entity finisher = registeredHitboxes(3).get(2);
    assertNull(finisher.getComponent(SweepComponent.class));
    assertEquals(0f, rotationOf(finisher), TOLERANCE); // straight out along the aim
    assertEquals(12, damageOf(finisher)); // twice a slash
    assertEquals(3f, knockbackOf(finisher)); // 1.5x the weapon knockback

    // The combo is over: nothing else spawns.
    tickStrikeInterval(knife);
    tickStrikeInterval(knife);
    registeredHitboxes(3);
  }

  @Test
  void shouldSpawnQueuedStrikesInFrontOfTheWielderNotOnThem() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = upgradableWielder(knife, true, 1f);
    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    tickStrikeInterval(knife);
    tickStrikeInterval(knife);
    List<Entity> strikes = registeredHitboxes(3);

    // Queued strikes are rendered before their FollowComponent first updates, so they must already
    // sit on their offset: the second slash opens at -35 degrees, the finisher straight ahead.
    Vector2 slashCentre =
        wielder.getCenterPosition().add(new Vector2(EXPECTED_REACH, 0f).setAngleDeg(-35f));
    assertEquals(slashCentre.x, strikes.get(1).getCenterPosition().x, TOLERANCE);
    assertEquals(slashCentre.y, strikes.get(1).getCenterPosition().y, TOLERANCE);
    Vector2 finisherCentre = wielder.getCenterPosition().add(EXPECTED_REACH, 0f);
    assertEquals(finisherCentre.x, strikes.get(2).getCenterPosition().x, TOLERANCE);
    assertEquals(finisherCentre.y, strikes.get(2).getCenterPosition().y, TOLERANCE);
  }

  @Test
  void shouldKeepEveryStrikeAimedWhereTheFlurryStarted() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, true, 1f);

    // Aiming up.
    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(0f, 1f)));
    tickStrikeInterval(knife);
    tickStrikeInterval(knife);
    List<Entity> strikes = registeredHitboxes(3);

    assertEquals(90f + HALF_SLASH_ARC, rotationOf(strikes.get(0)), TOLERANCE);
    assertEquals(90f - HALF_SLASH_ARC, rotationOf(strikes.get(1)), TOLERANCE);
    assertEquals(90f, rotationOf(strikes.get(2)), TOLERANCE);
    // Vertical blades are transposed like the stab.
    assertEquals(new Vector2(0.5f, 1.0f), strikes.get(2).getScale());
  }

  @Test
  void shouldCancelTheRestOfTheFlurryWhenTheKnifeIsUnequipped() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, true, 1f);
    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    // e.g. the L key switches away and back mid-combo
    knife.setEnabled(false);
    knife.setEnabled(true);
    tickStrikeInterval(knife);
    tickStrikeInterval(knife);

    registeredHitboxes(1);
  }

  @Test
  void shouldTripleTheCooldownAfterAFlurry() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = upgradableWielder(knife, true, 1f);

    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    assertEquals(
        1.5f, wielder.getComponent(WeaponStatsComponent.class).getRemainingCooldown(), TOLERANCE);
  }

  @Test
  void shouldNeverCoolDownBeforeTheFlurryEndsWithAttackSpeedBuffs() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = upgradableWielder(knife, true, 4f); // 0.5s / 4 * 3 = 0.375s

    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    // Clamped to the 0.45s combo: two 0.15s intervals plus the 0.15s finisher.
    assertEquals(
        0.45f, wielder.getComponent(WeaponStatsComponent.class).getRemainingCooldown(), TOLERANCE);
  }

  @Test
  void shouldUseTheBaseSpriteWhenNotUpgraded() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    upgradableWielder(knife, false, 1f);

    assertEquals(KnifeWeaponComponent.TEXTURE, knife.resolveTexture());
  }

  @Test
  void shouldUseTheUpgradedSpriteForEveryStrikeOnceUpgraded() {
    KnifeWeaponComponent knife = new KnifeWeaponComponent();
    Entity wielder = upgradableWielder(knife, true, 1f);

    assertEquals(KnifeWeaponComponent.UPGRADED_TEXTURE, knife.resolveTexture());
    // Both the stab and the flurry still spawn a textured blade with the upgraded sprite loaded.
    assertTrue(knife.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertNotNull(registeredHitboxes(1).get(0).getComponent(RotatingTextureRenderComponent.class));
    wielder.getComponent(WeaponStatsComponent.class).update(10f);
    assertTrue(knife.heavyAttack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    // Reverting the upgrade brings the base sprite back.
    wielder
        .getComponent(WeaponUpgradeComponent.class)
        .setUpgraded(KnifeWeaponComponent.class, false);
    assertEquals(KnifeWeaponComponent.TEXTURE, knife.resolveTexture());
  }
}
