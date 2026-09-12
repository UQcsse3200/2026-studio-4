package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerFactoryTest {
  private GameTime time;

  @BeforeEach
  void beforeEach() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerInputService(new InputService());
    ResourceService resources = mock(ResourceService.class);
    ServiceLocator.registerResourceService(resources);
    TextureAtlas atlas = mock(TextureAtlas.class);
    AtlasRegion region = mock(AtlasRegion.class);
    when(region.getRegionWidth()).thenReturn(16);
    when(region.getRegionHeight()).thenReturn(32);
    when(atlas.findRegion("default")).thenReturn(region);
    when(atlas.findRegions(anyString())).thenReturn(new Array<>(new AtlasRegion[] {region}));
    when(resources.getAsset("images/idle_down.atlas", TextureAtlas.class)).thenReturn(atlas);
  }

  @Test
  void shouldAttachIndependentAbilitiesToEachPlayer() {
    Entity first = PlayerFactory.createPlayer();
    Entity second = PlayerFactory.createPlayer();
    PlayerAbilitiesComponent firstAbilities = first.getComponent(PlayerAbilitiesComponent.class);
    PlayerAbilitiesComponent secondAbilities = second.getComponent(PlayerAbilitiesComponent.class);
    assertNotNull(firstAbilities);
    assertNotNull(secondAbilities);
    assertNotSame(firstAbilities, secondAbilities);
    assertSame(first, firstAbilities.getEntity());
    assertSame(second, secondAbilities.getEntity());

    // Initialise only the integration under test, avoiding unrelated UI and input lifecycles.
    firstAbilities.create();
    secondAbilities.create();
    assertFalse(firstAbilities.isInvisible());
    assertFalse(firstAbilities.isLastStandActive());
    assertTrue(firstAbilities.tryInvisibility());
    assertTrue(firstAbilities.isInvisible());
    assertFalse(secondAbilities.isInvisible());
  }

  @Test
  void shouldWireFactoryAbilitiesToCombatStatsAndRegisteredClock() {
    Entity player = PlayerFactory.createPlayer();
    PlayerAbilitiesComponent abilities = player.getComponent(PlayerAbilitiesComponent.class);
    CombatStatsComponent combat = player.getComponent(CombatStatsComponent.class);
    assertNotNull(abilities);
    assertNotNull(combat);
    abilities.create();
    int rawAttack = combat.getBaseAttack();
    float rawAttackSpeed = combat.getAttackSpeed();
    float movementSpeed = combat.getMovementSpeed();
    abilities.enableLastStand();
    combat.takeDamage(
        combat.getHealth() - 1,
        new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    assertTrue(abilities.isLastStandActive());
    assertEquals(Math.round(rawAttack * 1.5f), combat.getEffectiveBaseAttack());
    assertEquals(rawAttackSpeed * 1.5f, combat.getEffectiveAttackSpeed());
    assertEquals(rawAttack, combat.getBaseAttack());
    assertEquals(rawAttackSpeed, combat.getAttackSpeed());
    assertEquals(movementSpeed, combat.getMovementSpeed());
    when(time.getTime()).thenReturn(PlayerAbilitiesComponent.LAST_STAND_DURATION_MS);
    assertEquals(rawAttack, combat.getEffectiveBaseAttack());
    assertEquals(rawAttackSpeed, combat.getEffectiveAttackSpeed());
  }
}
