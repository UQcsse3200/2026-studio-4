package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.spells.FreezeSpellComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.spells.SpellAoeVisualComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** The player is actually given the spells, and each player gets its own. */
@ExtendWith(GameExtension.class)
class PlayerFactorySpellsTest {

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    EntityService entities = mock(EntityService.class);
    when(entities.getEntities()).thenReturn(new Array<>());
    ServiceLocator.registerEntityService(entities);

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
  void carriesBothSpellsAndTheDiscTheyShareToShowTheirArea() {
    Entity player = PlayerFactory.createPlayer();

    assertNotNull(player.getComponent(LightningSpellComponent.class));
    assertNotNull(player.getComponent(FreezeSpellComponent.class));
    assertNotNull(player.getComponent(SpellAoeVisualComponent.class), "one disc serves both");
  }

  @Test
  void bothSpellsStartReadyToCast() {
    Entity player = PlayerFactory.createPlayer();

    assertTrue(player.getComponent(LightningSpellComponent.class).canCast());
    assertTrue(player.getComponent(FreezeSpellComponent.class).canCast());
  }

  @Test
  void givesEachPlayerItsOwnSpellsRatherThanSharingOneCooldown() {
    Entity first = PlayerFactory.createPlayer();
    Entity second = PlayerFactory.createPlayer();

    assertNotSame(
        first.getComponent(LightningSpellComponent.class),
        second.getComponent(LightningSpellComponent.class));
    assertNotSame(
        first.getComponent(FreezeSpellComponent.class),
        second.getComponent(FreezeSpellComponent.class));
  }

  @Test
  void castingOneSpellDoesNotPutTheOtherOnCooldown() {
    // Only the spells are brought to life here, not the player's UI and input lifecycles.
    Entity player = PlayerFactory.createPlayer();

    assertTrue(player.getComponent(FreezeSpellComponent.class).cast());

    assertTrue(player.getComponent(LightningSpellComponent.class).canCast());
  }

  @Test
  void bothSpellsCoverTheSameAreaSoThePlayerLearnsOneRange() {
    Entity player = PlayerFactory.createPlayer();

    float lightningWidth = discWidthAfterCasting(player, true);
    float freezeWidth = discWidthAfterCasting(player, false);

    assertTrue(lightningWidth > 0f, "a cast should show the area it covered");
    assertEquals(lightningWidth, freezeWidth);
  }

  /** Casts one spell on a fresh player and returns how wide a disc it drew. */
  private static float discWidthAfterCasting(Entity player, boolean lightning) {
    SpellAoeVisualComponent visual = player.getComponent(SpellAoeVisualComponent.class);
    if (lightning) {
      player.getComponent(LightningSpellComponent.class).cast();
    } else {
      player.getComponent(FreezeSpellComponent.class).cast();
    }

    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
    visual.render(batch);

    ArgumentCaptor<Float> width = ArgumentCaptor.forClass(Float.class);
    verify(batch).draw(any(Texture.class), anyFloat(), anyFloat(), width.capture(), anyFloat());
    return width.getValue();
  }
}
