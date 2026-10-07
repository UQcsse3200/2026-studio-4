package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.miniboss.cerberus.HomingProjectileMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CerberusProjectileFactoryTest {
  private ResourceService resources;
  private Entity player;
  private Entity projectile;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(mock(EntityService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextureAtlases(new String[] {CerberusProjectileFactory.ATLAS_PATH});
    resources.loadAll();

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(3f, 0f);
    player.create();

    projectile = CerberusProjectileFactory.createHomingProjectile(new Vector2(), player, 7);
    projectile.create();
  }

  @AfterEach
  void tearDown() {
    if (projectile != null) {
      projectile.dispose();
    }
    if (player != null) {
      player.dispose();
    }
    if (resources != null) {
      resources.unloadAssets(new String[] {CerberusProjectileFactory.ATLAS_PATH});
    }
  }

  @Test
  void shouldLoadRealFireballFramesAndStartFlightAnimation() {
    TextureAtlas atlas =
        resources.getAsset(CerberusProjectileFactory.ATLAS_PATH, TextureAtlas.class);

    assertEquals(7, atlas.findRegions("projectile").size);
    assertEquals(5, atlas.findRegions("projectileHit").size);
    assertEquals(
        "projectile",
        projectile.getComponent(AnimationRenderComponent.class).getCurrentAnimation());
    assertNotNull(projectile.getComponent(HomingProjectileMovementComponent.class));
  }

  @Test
  void shouldPreservePlayerContactDamage() {
    Fixture projectileFixture = projectile.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    projectile.getEvents().trigger("collisionStart", projectileFixture, playerFixture);

    assertEquals(13, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldPlayImpactAnimationWhenRangeIsReached() {
    projectile.getEvents().trigger("projectileRangeReached");

    assertEquals(
        "projectileHit",
        projectile.getComponent(AnimationRenderComponent.class).getCurrentAnimation());
  }
}
