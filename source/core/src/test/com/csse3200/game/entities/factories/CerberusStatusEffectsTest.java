package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression: Cerberus once built its parts from a private copy of the shared NPC base that had
 * drifted, leaving out the status effects controller. It still counted as an enemy, so spells
 * picked it out and lightning hurt it, but every effect put on it was silently dropped: no freeze,
 * no tint, nothing. A player saw spells work on some enemies and do nothing at all to Cerberus.
 */
@ExtendWith(GameExtension.class)
class CerberusStatusEffectsTest {
  private final List<Entity> sideHeads = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(mock(EntityService.class));
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    // A real physics service, so Box2D's natives are loaded and colliders can actually be built.
    ServiceLocator.registerPhysicsService(new PhysicsService());

    TextureAtlas atlas = mock(TextureAtlas.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(TextureAtlas.class))).thenReturn(atlas);
    ServiceLocator.registerResourceService(resources);
  }

  private Entity createCerberus() {
    sideHeads.clear();
    return CerberusFactory.createCerberus(
        new Vector2(5f, 5f), sideHeads::add, "images/cerberus.atlas");
  }

  @Test
  void everyCerberusPartCanCarryAStatusEffect() {
    Entity mainHead = createCerberus();

    assertNotNull(
        mainHead.getComponent(StatusEffectsControllerComponent.class),
        "the body cannot be frozen, burned or tinted without one");
    assertEquals(2, sideHeads.size());
    for (Entity sideHead : sideHeads) {
      assertNotNull(sideHead.getComponent(StatusEffectsControllerComponent.class));
    }
  }

  @Test
  void everyCerberusPartIsOnTheLayerSpellsTargetAndCanBeSteered() {
    // These are what made the missing controller so easy to miss: Cerberus was picked as a target
    // and could be moved, so only the effects quietly did nothing.
    Entity mainHead = createCerberus();

    for (Entity part : allParts(mainHead)) {
      HitboxComponent hitbox = part.getComponent(HitboxComponent.class);
      assertNotNull(hitbox);
      assertTrue(PhysicsLayer.contains(hitbox.getLayer(), PhysicsLayer.NPC));
      assertNotNull(
          part.getComponent(com.csse3200.game.physics.components.PhysicsMovementComponent.class),
          "freezing works by stopping the shared movement component");
    }
  }

  @Test
  void aCerberusPartIsBuiltTheSameWayEveryOtherEnemyIs() {
    // Pinning this to the shared base rather than to a list of components, so a component added
    // for all enemies later cannot go missing on Cerberus again.
    Entity mainHead = createCerberus();
    Entity baseNpc = NPCFactory.createBaseNPC();

    for (Entity part : allParts(mainHead)) {
      for (Class<? extends com.csse3200.game.components.Component> type :
          List.of(
              StatusEffectsControllerComponent.class,
              HitboxComponent.class,
              com.csse3200.game.physics.components.PhysicsComponent.class,
              com.csse3200.game.physics.components.PhysicsMovementComponent.class,
              com.csse3200.game.physics.components.ColliderComponent.class)) {
        assertNotNull(baseNpc.getComponent(type), "the shared base should still provide " + type);
        assertNotNull(part.getComponent(type), type + " missing from a Cerberus part");
      }
    }
  }

  private List<Entity> allParts(Entity mainHead) {
    List<Entity> parts = new ArrayList<>(sideHeads);
    parts.add(mainHead);
    return parts;
  }
}
