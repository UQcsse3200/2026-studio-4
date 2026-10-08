package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.ChainRestrictionComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.HeadAttachmentComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusChainComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusChainRenderComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Action;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Part;
import com.csse3200.game.components.miniboss.cerberus.CerberusPhaseComponent;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.CerberusFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({GameExtension.class, MockitoExtension.class})
class CerberusTest {
  @Mock private EntityService entityService;
  @Mock private ResourceService resourceService;
  @Mock private com.csse3200.game.rendering.RenderService renderService;
  @Mock private com.csse3200.game.services.GameTime gameTime;
  @Mock private TextureAtlas textureAtlas;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerTimeSource(gameTime);

    ServiceLocator.registerPhysicsService(new PhysicsService());
    when(resourceService.getAsset(anyString(), eq(TextureAtlas.class))).thenReturn(textureAtlas);

    String[] parts = {"body", "left", "middle", "right"};
    String[] actions = {"idle", "move", "lunge", "cast"};

    for (String part : parts) {
      for (String action : actions) {
        for (int index = 0; index < 4; index++) {
          TextureAtlas.AtlasRegion region = mock(TextureAtlas.AtlasRegion.class);
          when(textureAtlas.findRegion(part + "_" + action, index)).thenReturn(region);
        }
      }
    }
  }

  private Entity createCerberusForAnimationTest() {
    return CerberusFactory.createCerberus(
        new Vector2(5f, 5f), entityService::register, "images/cerberus/cerberus-modular.atlas");
  }

  @Test
  void shouldPlayBodyAndMiddleLungeOnAttackStart() {
    Entity cerberus = createCerberusForAnimationTest();
    CerberusLayeredRenderComponent renderer =
        cerberus.getComponent(CerberusLayeredRenderComponent.class);

    cerberus.getEvents().trigger("attackStart");

    assertEquals(Action.LUNGE, renderer.getAction(Part.BODY));
    assertEquals(Action.LUNGE, renderer.getAction(Part.MIDDLE));
    assertEquals(Action.IDLE, renderer.getAction(Part.LEFT));
    assertEquals(Action.IDLE, renderer.getAction(Part.RIGHT));
  }

  @Test
  void shouldReturnBodyAndMiddleToIdleWithoutInterruptingSideHeads() {
    Entity cerberus = createCerberusForAnimationTest();
    CerberusLayeredRenderComponent renderer =
        cerberus.getComponent(CerberusLayeredRenderComponent.class);

    cerberus.getEvents().trigger("attackStart");
    renderer.play(Part.LEFT, Action.CAST);
    renderer.play(Part.RIGHT, Action.CAST);

    cerberus.getEvents().trigger("default");

    assertEquals(Action.IDLE, renderer.getAction(Part.BODY));
    assertEquals(Action.IDLE, renderer.getAction(Part.MIDDLE));
    assertEquals(Action.CAST, renderer.getAction(Part.LEFT));
    assertEquals(Action.CAST, renderer.getAction(Part.RIGHT));
  }

  @Test
  void shouldHideMiddleAndKeepBodyWhenMiddleHealthReachesZero() {
    Entity cerberus = createCerberusForAnimationTest();
    CerberusLayeredRenderComponent renderer =
        cerberus.getComponent(CerberusLayeredRenderComponent.class);

    cerberus.getEvents().trigger("attackStart");
    cerberus.getComponent(CombatStatsComponent.class).setHealth(0);

    // No renderer.update(): the Factory's death listener must react immediately.
    assertFalse(renderer.isPartVisible(Part.MIDDLE));
    assertTrue(renderer.isPartVisible(Part.BODY));
    assertTrue(renderer.isPartVisible(Part.LEFT));
    assertTrue(renderer.isPartVisible(Part.RIGHT));
    assertEquals(Action.IDLE, renderer.getAction(Part.BODY));
  }

  @Test
  void shouldCreateCerberusWithCorrectComponents() {
    Vector2 anchor = new Vector2(5f, 5f);
    String skin = "images/cerberus/cerberus-modular.atlas";

    Entity cerberus = CerberusFactory.createCerberus(anchor, entityService::register, skin);
    assertNotNull(cerberus);
    assertNotNull(cerberus.getComponent(ChainRestrictionComponent.class));
    assertNotNull(cerberus.getComponent(CombatStatsComponent.class));
    assertNotNull(cerberus.getComponent(CerberusPhaseComponent.class));
    assertNotNull(cerberus.getComponent(CerberusLayeredRenderComponent.class));
    assertNotNull(cerberus.getComponent(EnemyStatDisplay.class));

    verify(entityService, times(2)).register(any(Entity.class));
  }

  @Test
  void shouldAlignSideHeadsWithTheEnlargedSprite() {
    List<Entity> heads = new ArrayList<>();

    Entity body =
        CerberusFactory.createCerberus(
            new Vector2(5f, 5f), heads::add, "images/cerberus/cerberus-modular.atlas");

    assertEquals(2f, body.getScale().x, 0.001f);
    assertEquals(2f, body.getScale().y, 0.001f);
    assertEquals(2, heads.size());

    body.setPosition(5f, 7f);

    for (Entity head : heads) {
      head.getComponent(HeadAttachmentComponent.class).update();
      assertEquals(0.56f, head.getScale().x, 0.001f);
      assertEquals(0.70f, head.getScale().y, 0.001f);
    }

    assertEquals(5.5625f, heads.get(0).getCenterPosition().x, 0.001f);
    assertEquals(6.4375f, heads.get(1).getCenterPosition().x, 0.001f);
    assertEquals(8.30f, heads.get(0).getCenterPosition().y, 0.001f);
    assertEquals(8.30f, heads.get(1).getCenterPosition().y, 0.001f);
  }

  @Test
  void shouldAttachChainToBodyAndKeepWallAnchorFixed() {
    TextureAtlas.AtlasRegion chainRegion = mock(TextureAtlas.AtlasRegion.class);
    when(textureAtlas.findRegion("chain")).thenReturn(chainRegion);

    Vector2 wallAnchor = new Vector2(4f, 8f);

    Entity cerberus =
        CerberusFactory.createCerberus(
            null,
            new Vector2(5f, 5f),
            wallAnchor,
            entityService::register,
            "images/cerberus/cerberus-modular.atlas");

    CerberusChainComponent chain = cerberus.getComponent(CerberusChainComponent.class);
    CerberusChainRenderComponent renderer =
        cerberus.getComponent(CerberusChainRenderComponent.class);

    assertNotNull(chain);
    assertNotNull(renderer);

    renderer.create();
    try {
      cerberus.setPosition(6f, 5f);

      Vector2[] points = renderer.getChainPoints();

      assertEquals(new Vector2(4f, 8f), points[0]);
      assertEquals(chain.getBodyAttachment(), points[points.length - 1]);

      // Changing the caller's vector must not move the wall attachment.
      wallAnchor.setZero();
      assertEquals(new Vector2(4f, 8f), chain.getWallAnchor());
    } finally {
      renderer.dispose();
    }
  }
}
