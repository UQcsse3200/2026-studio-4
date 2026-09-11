package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.BossPhaseComponent;
import com.csse3200.game.components.ChainRestrictionComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CerberusTest {
  @Mock private EntityService entityService;
  @Mock private ResourceService resourceService;
  @Mock private com.csse3200.game.rendering.RenderService renderService;
  @Mock private com.csse3200.game.services.GameTime gameTime;
  @Mock private com.csse3200.game.physics.PhysicsService physicsService;
  @Mock private com.csse3200.game.physics.PhysicsEngine physicsEngine;
  @Mock private TextureAtlas textureAtlas;
  @Mock private Entity target;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerTimeSource(gameTime);

    when(physicsService.getPhysics()).thenReturn(physicsEngine);
    ServiceLocator.registerPhysicsService(physicsService);
    when(resourceService.getAsset(anyString(), eq(TextureAtlas.class))).thenReturn(textureAtlas);
  }

  @Test
  void shouldCreateCerberusWithCorrectComponents() {
    Vector2 anchor = new Vector2(5f, 5f);
    String skin = "images/chaseEnemy.atlas";

    Entity cerberus = NPCFactory.createCerberus(target, anchor, skin);
    assertNotNull(cerberus);
    assertNotNull(cerberus.getComponent(ChainRestrictionComponent.class));
    assertNotNull(cerberus.getComponent(CombatStatsComponent.class));
    assertNotNull(cerberus.getComponent(BossPhaseComponent.class));
    assertNotNull(cerberus.getComponent(AnimationRenderComponent.class));

    verify(entityService, times(2)).register(any(Entity.class));
  }
}
