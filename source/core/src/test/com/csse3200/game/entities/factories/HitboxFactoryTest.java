package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.components.weapons.FollowComponent;
import com.csse3200.game.components.weapons.LifetimeComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HitboxFactoryTest {
  @Test
  void shouldInheritLiveAppearanceWithoutFollowingOrRescalingDamage() {
    ResourceService resources = mock(ResourceService.class);
    ServiceLocator.registerResourceService(resources);
    Texture weaponTexture = mock(Texture.class);
    when(resources.getAsset("weapon.png", Texture.class)).thenReturn(weaponTexture);
    GameTime time = mock(GameTime.class);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    Entity source =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    source.create();
    Entity hitbox =
        HitboxFactory.createHitbox(meleeSpec().texture("weapon.png").visualSource(source));
    RotatingTextureRenderComponent render =
        hitbox.getComponent(RotatingTextureRenderComponent.class);
    assertNotNull(render);
    assertNull(hitbox.getComponent(FollowComponent.class));
    assertNull(hitbox.getComponent(PlayerAbilitiesComponent.class));
    SpriteBatch batch = mock(SpriteBatch.class);
    Color color = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    Color original = new Color(color);
    when(batch.getColor()).thenReturn(color);
    doAnswer(
            invocation -> {
              color.set(
                  invocation.getArgument(0),
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3));
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
    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertTrue(abilities.tryActivate(Invisibility.class));
    render.render(batch);
    assertEquals(
        List.of(original, new Color(0.8f, 0.6f * 0.35f, 0.4f * 0.35f, 0.5f * 0.35f)), drawn);
    assertEquals(original, color);
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getEffectiveBaseAttack());
    source.setPosition(20f, 30f);
    assertEquals(new Vector2(1f, 2f), hitbox.getPosition());
    when(time.getTime()).thenReturn(Invisibility.DURATION_MS);
    render.render(batch);
    assertEquals(original, drawn.get(2));
  }

  @Test
  void shouldAllowVisualSourceWithoutTextureAndWithoutFollowOwner() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec().visualSource(new Entity()));
    assertNull(hitbox.getComponent(RotatingTextureRenderComponent.class));
    assertNull(hitbox.getComponent(FollowComponent.class));
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldPreserveHostileHitboxAsDamageSourceAndRespectInvisibilityOnCollision() {
    GameTime time = mock(GameTime.class);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    CombatStatsComponent combat = new CombatStatsComponent(100, 0);
    Entity target =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    target.create();
    Entity owner = new Entity();
    Entity hitbox =
        HitboxFactory.createHitbox(
            meleeSpec()
                .targetLayer(PhysicsLayer.PLAYER)
                .owner(owner)
                .visualSource(owner)
                .knockback(0f));
    hitbox.create();
    List<Entity> sources = new ArrayList<>();
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) -> {
              sources.add(source);
              assertEquals(8, lost.intValue());
              assertEquals(92, remaining.intValue());
            });
    Fixture attackFixture = hitbox.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    assertTrue(CombatStatsComponent.isHostileAttacker(hitbox));
    assertTrue(abilities.tryActivate(Invisibility.class));
    hitbox.getEvents().trigger("collisionStart", attackFixture, targetFixture);
    assertEquals(100, combat.getHealth());
    assertTrue(sources.isEmpty());

    when(time.getTime()).thenReturn(Invisibility.DURATION_MS);
    hitbox.getEvents().trigger("collisionStart", attackFixture, targetFixture);
    assertEquals(92, combat.getHealth());
    assertEquals(List.of(hitbox), sources);
  }

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldSpawnSensorOnWeaponLayer() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    hitbox.create();

    HitboxComponent hitboxComponent = hitbox.getComponent(HitboxComponent.class);
    assertNotNull(hitboxComponent);
    assertTrue(hitboxComponent.getFixture().isSensor());
    assertEquals(PhysicsLayer.WEAPON, hitboxComponent.getLayer());
    assertEquals(
        BodyType.KinematicBody, hitbox.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(new Vector2(1f, 2f), hitbox.getPosition());
    assertEquals(new Vector2(0.4f, 0.8f), hitbox.getScale());
    assertNotNull(hitbox.getComponent(LifetimeComponent.class));
    assertNotNull(hitbox.getComponent(TouchAttackComponent.class));
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldFollowWhenOwnerProvided() {
    Entity owner = new Entity();
    HitboxSpec spec = meleeSpec().owner(owner).localOffset(new Vector2(0.5f, 0f));
    Entity hitbox = HitboxFactory.createHitbox(spec);
    assertNotNull(hitbox.getComponent(FollowComponent.class));
  }

  @Test
  void shouldNotFollowWhenOwnerOmitted() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    assertNull(hitbox.getComponent(FollowComponent.class));
  }

  @Test
  void shouldDamageTargetOnCollision() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    hitbox.create();

    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC));
    target.create();

    Fixture hitboxFixture = hitbox.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    hitbox.getEvents().trigger("collisionStart", hitboxFixture, targetFixture);

    assertEquals(2, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldRejectNullSpec() {
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(null));
  }

  @Test
  void shouldRejectMissingPosition() {
    HitboxSpec spec = new HitboxSpec().size(new Vector2(0.4f, 0.8f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectMissingSize() {
    HitboxSpec spec = new HitboxSpec().position(new Vector2(1f, 2f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNonPositiveSize() {
    HitboxSpec zeroWidth = meleeSpec().size(new Vector2(0f, 1f));
    HitboxSpec zeroHeight = meleeSpec().size(new Vector2(1f, 0f));
    HitboxSpec negativeSize = meleeSpec().size(new Vector2(-0.1f, 1f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(zeroWidth));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(zeroHeight));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(negativeSize));
  }

  @Test
  void shouldRejectNegativeLifetime() {
    HitboxSpec spec = meleeSpec().lifetime(-0.1f);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNegativeDamage() {
    HitboxSpec spec = meleeSpec().damage(-1);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNegativeKnockback() {
    HitboxSpec spec = meleeSpec().knockback(-0.5f);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void stubWeaponShouldSpawnHitboxThroughFactory() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.2f, 0.8f, 0f);
    FactoryWeapon weapon = new FactoryWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.setPosition(0f, 0f);
    wielder.create();

    assertTrue(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertNotNull(weapon.spawned);
    assertNotNull(weapon.spawned.getComponent(HitboxComponent.class));
    assertFalse(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
  }

  @Test
  void shouldPreventInstantiation() throws Exception {
    var constructor = HitboxFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    Exception thrown =
        assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
    assertTrue(thrown.getCause() instanceof IllegalStateException);
  }

  private static HitboxSpec meleeSpec() {
    return new HitboxSpec()
        .position(new Vector2(1f, 2f))
        .size(new Vector2(0.4f, 0.8f))
        .lifetime(0.15f)
        .layer(PhysicsLayer.WEAPON)
        .targetLayer(PhysicsLayer.NPC)
        .damage(8)
        .knockback(1.5f);
  }

  private static class FactoryWeapon extends WeaponComponent {
    Entity spawned;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
      HitboxSpec spec =
          new HitboxSpec()
              .position(origin)
              .size(new Vector2(0.4f, 0.8f))
              .lifetime(0.15f)
              .layer(PhysicsLayer.WEAPON)
              .targetLayer(PhysicsLayer.NPC)
              .damage(resolveHitboxDamage())
              .knockback(stats.getKnockback())
              .owner(entity)
              .localOffset(direction.cpy().nor().scl(0.5f));
      spawned = HitboxFactory.createHitbox(spec);
    }
  }
}
