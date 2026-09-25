package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.miniboss.dragon.ThunderOrbHitComponent;
import com.csse3200.game.components.miniboss.dragon.ThunderOrbMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ThunderOrbFactoryTest {
  private ResourceService resources;
  private EntityService entities;
  private final List<Entity> orbs = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerTimeSource(new GameTime());

    entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);

    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextureAtlases(new String[] {ThunderOrbFactory.ATLAS_PATH});
    resources.loadAll();
  }

  @AfterEach
  void tearDown() {
    for (Entity orb : orbs) {
      orb.getComponent(PhysicsComponent.class).dispose();
    }
    resources.unloadAssets(new String[] {ThunderOrbFactory.ATLAS_PATH});
  }

  private Entity createOrb(Vector2 center) {
    Entity target = new Entity();
    target.setPosition(10f, 0f);

    Entity orb = ThunderOrbFactory.createThunderOrb(center, target);
    orbs.add(orb);
    return orb;
  }

  @Test
  void shouldAttachRequiredComponents() {
    Entity orb = createOrb(new Vector2());

    assertNotNull(orb.getComponent(PhysicsComponent.class));
    assertNotNull(orb.getComponent(ThunderOrbMovementComponent.class));
    assertNotNull(orb.getComponent(ThunderOrbHitComponent.class));

    HitboxComponent hitbox = orb.getComponent(HitboxComponent.class);
    assertNotNull(hitbox);
    assertEquals(PhysicsLayer.NPC, hitbox.getLayer());
  }

  @Test
  void shouldUseRequestedCentreWithoutChangingInput() {
    Vector2 center = new Vector2(5f, 7f);
    Entity orb = createOrb(center);

    assertEquals(5f, orb.getCenterPosition().x, 0.001f);
    assertEquals(7f, orb.getCenterPosition().y, 0.001f);
    assertEquals(new Vector2(5f, 7f), center);
  }

  @Test
  void shouldLoadBothAnimationsAndStartFlying() {
    Entity orb = createOrb(new Vector2());
    AnimationRenderComponent animator = orb.getComponent(AnimationRenderComponent.class);
    TextureAtlas atlas = resources.getAsset(ThunderOrbFactory.ATLAS_PATH, TextureAtlas.class);

    assertEquals(7, atlas.findRegions("fly").size);
    assertEquals(6, atlas.findRegions("impact").size);
    assertTrue(animator.hasAnimation("fly"));
    assertTrue(animator.hasAnimation("impact"));
    assertEquals("fly", animator.getCurrentAnimation());
  }

  @Test
  void shouldConnectLifetimeExpiryToRemoval() {
    Entity orb = createOrb(new Vector2());
    ThunderOrbMovementComponent movement = orb.getComponent(ThunderOrbMovementComponent.class);

    movement.create();
    orb.getComponent(ThunderOrbHitComponent.class).create();

    movement.update(3f);

    assertTrue(movement.isStopped());
    verify(entities).scheduleDisposal(orb);
  }

  @Test
  void shouldRejectInvalidSpawnArguments() {
    Entity target = new Entity();
    Vector2 validCentre = new Vector2();
    Vector2 invalidCentre = new Vector2(Float.NaN, 0f);

    assertThrows(
        IllegalArgumentException.class, () -> ThunderOrbFactory.createThunderOrb(null, target));

    assertThrows(
        IllegalArgumentException.class,
        () -> ThunderOrbFactory.createThunderOrb(validCentre, null));

    assertThrows(
        IllegalArgumentException.class,
        () -> ThunderOrbFactory.createThunderOrb(invalidCentre, target));
  }
}
