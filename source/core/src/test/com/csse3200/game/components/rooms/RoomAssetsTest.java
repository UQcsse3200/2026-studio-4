package com.csse3200.game.components.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.components.miniboss.snake.SnakePoisonAssets;
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

  @Test
  void shouldStartMusicAfterLoad() {
    RoomAssets roomAssets = new RoomAssets();
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);

    roomAssets.loadAll();

    verify(music).play();
  }

  /**
   * asserts that RoomAssets.loadAll() calls load all on the resource service, but does not check
   * specific assets being loaded
   */
  @Test
  void shouldLoadAssets() {
    RoomAssets roomAssets = new RoomAssets();
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);

    roomAssets.loadAll();

    verify(resourceService).loadAll();
    verify(resourceService)
        .loadTextures(
            argThat(
                paths ->
                    Arrays.asList(paths).containsAll(Arrays.asList(SnakePoisonAssets.paths()))));
  }

  /**
   * Asserts 4 calls to {@link ResourceService#unloadAssets(String[])} (music, sounds, textures,
   * atlases)
   */
  @Test
  void shouldUnloadOnDispose() {
    RoomAssets roomAssets = new RoomAssets();
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);

    roomAssets.dispose();
    verify(music).stop();
    verify(resourceService, times(4)).unloadAssets(any());
  }
}
