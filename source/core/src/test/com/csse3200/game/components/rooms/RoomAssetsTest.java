package com.csse3200.game.components.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.miniboss.snake.SnakePoisonAssets;
import com.csse3200.game.components.miniboss.snake.SnakeShieldComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldPickupComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** RoomAssetsTest */
@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
public class RoomAssetsTest {

  @Mock ResourceService resourceService;

  @BeforeEach
  void setup() {
    ServiceLocator.registerResourceService(resourceService);
  }

  /**
   * asserts that RoomAssets.loadAll() calls load all on the resource service, but does not check
   * specific assets being loaded
   */
  @Test
  void shouldLoadAssets() {
    RoomAssets roomAssets = new RoomAssets();

    roomAssets.loadAll();

    verify(resourceService).loadAll();
    // Music belongs to MusicManager now.
    verify(resourceService, never()).loadMusic(any());
    verify(resourceService)
        .loadTextures(
            argThat(
                paths ->
                    Arrays.asList(paths).containsAll(Arrays.asList(SnakePoisonAssets.paths()))
                        && Arrays.asList(paths).contains(SnakeShieldComponent.SHIELD_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.GEM_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.SPAWN_TEXTURE)
                        && Arrays.asList(paths)
                            .containsAll(
                                Arrays.asList(
                                    "images/items/consumables/health_potion_small_pixel.png",
                                    "images/items/consumables/health_potion_medium_pixel.png",
                                    "images/items/consumables/health_potion_large_pixel.png"))));
  }

  /**
   * Asserts 3 calls to {@link ResourceService#unloadAssets(String[])} (sounds, textures, atlases).
   * Music is unloaded by MusicManager.
   */
  @Test
  void shouldUnloadOnDispose() {
    RoomAssets roomAssets = new RoomAssets();

    roomAssets.dispose();
    verify(resourceService, times(3)).unloadAssets(any());
    verify(resourceService)
        .unloadAssets(
            argThat(
                paths ->
                    Arrays.asList(paths).contains(SnakeShieldComponent.SHIELD_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.GEM_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.SPAWN_TEXTURE)
                        && Arrays.asList(paths)
                            .containsAll(
                                Arrays.asList(
                                    "images/items/consumables/health_potion_small_pixel.png",
                                    "images/items/consumables/health_potion_medium_pixel.png",
                                    "images/items/consumables/health_potion_large_pixel.png"))));
  }
}
