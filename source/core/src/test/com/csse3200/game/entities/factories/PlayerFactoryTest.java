package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
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
    assertFalse(firstAbilities.isActive(Invisibility.class));
    assertFalse(firstAbilities.isActive(LastStand.class));

    // A factory-built player has earned nothing yet, so every ability is locked.
    AbilityAttunementComponent firstAttunement =
        first.getComponent(AbilityAttunementComponent.class);
    AbilityAttunementComponent secondAttunement =
        second.getComponent(AbilityAttunementComponent.class);
    assertNotNull(firstAttunement);
    assertNotNull(secondAttunement);
    assertNotSame(firstAttunement, secondAttunement);
    firstAttunement.create();
    secondAttunement.create();
    assertNull(firstAttunement.getAttuned());
    assertFalse(firstAbilities.isUnlocked(Invisibility.class));
    assertFalse(firstAbilities.tryActivate(Invisibility.class));

    assertTrue(firstAttunement.attune(Invisibility.class));
    assertTrue(firstAbilities.tryActivate(Invisibility.class));
    assertTrue(firstAbilities.isActive(Invisibility.class));
    // Attuning one player leaves the other untouched.
    assertFalse(secondAbilities.isActive(Invisibility.class));
    assertFalse(secondAbilities.isUnlocked(Invisibility.class));
  }

  @Test
  void shouldTakeBackTheAttunedAbilityWhenThePlayerDies() {
    Entity player = PlayerFactory.createPlayer();
    PlayerAbilitiesComponent abilities = player.getComponent(PlayerAbilitiesComponent.class);
    AbilityAttunementComponent attunement = player.getComponent(AbilityAttunementComponent.class);
    abilities.create();
    attunement.create();
    assertTrue(attunement.attune(Invisibility.class));

    // PlayerAbilitiesComponent relocks to each ability's default on death, which would hand back
    // an ability that starts unlocked. Attunement has to survive that.
    player.getEvents().trigger("entityDied");
    attunement.update();

    assertNull(attunement.getAttuned());
    assertFalse(abilities.isUnlocked(Invisibility.class));
    assertFalse(abilities.isUnlocked(LastStand.class));
  }
}
