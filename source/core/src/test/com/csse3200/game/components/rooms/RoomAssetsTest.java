package com.csse3200.game.components.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Music;
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

  @Test
  void shouldNotStartMusicUntilARoomAsksForIt() {
    RoomAssets roomAssets = new RoomAssets();

    roomAssets.loadAll();

    // Which track plays depends on the room, so loading alone must not even reach for one.
    verify(resourceService, never()).getAsset(anyString(), any());
  }

  @Test
  void shouldStartTheTrackTheRoomAsksFor() {
    RoomAssets roomAssets = new RoomAssets();
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);

    roomAssets.playMusicFor(null, true);

    verify(music).setLooping(true);
    verify(music).play();
  }

  @Test
  void shouldFightOnlyWhileADungeonRoomStillHasEnemies() {
    RoomAssets roomAssets = new RoomAssets();
    Music calm = mock(Music.class);
    Music fight = mock(Music.class);
    when(resourceService.getAsset(eq("sounds/lobby_music.mp3"), any())).thenReturn(calm);
    when(resourceService.getAsset(eq("sounds/fight_music.mp3"), any())).thenReturn(fight);

    roomAssets.playMusicFor("dungeonOne", false);
    verify(fight).play();
    verify(calm, never()).play();

    // Clearing the room drops back to the calm track and stops the fight.
    roomAssets.playMusicFor("dungeonOne", true);
    verify(fight).stop();
    verify(calm).play();
  }

  @Test
  void shouldTreatTheHubAsCalmEvenIfSomethingClaimsItIsNotCleared() {
    RoomAssets roomAssets = new RoomAssets();
    Music calm = mock(Music.class);
    Music fight = mock(Music.class);
    when(resourceService.getAsset(eq("sounds/lobby_music.mp3"), any())).thenReturn(calm);

    roomAssets.playMusicFor(null, false);

    verify(calm).play();
    verify(fight, never()).play();
  }

  @Test
  void shouldNotRestartATrackThatIsAlreadyPlaying() {
    RoomAssets roomAssets = new RoomAssets();
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);
    when(music.isPlaying()).thenReturn(true);

    // Walking between rooms of one dungeon must not restart its music.
    roomAssets.playMusicFor("dungeonOne", false);
    roomAssets.playMusicFor("dungeonOne", false);

    verify(music, never()).play();
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
    verify(resourceService)
        .loadTextures(
            argThat(
                paths ->
                    Arrays.asList(paths).containsAll(Arrays.asList(SnakePoisonAssets.paths()))
                        && Arrays.asList(paths).contains(SnakeShieldComponent.SHIELD_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.GEM_TEXTURE)
                        && Arrays.asList(paths)
                            .contains(SnakeShieldPickupComponent.SPAWN_TEXTURE)));
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
    // Both tracks are stopped: the hub's and the dungeons'.
    verify(music, times(2)).stop();
    verify(resourceService, times(4)).unloadAssets(any());
    verify(resourceService)
        .unloadAssets(
            argThat(
                paths ->
                    Arrays.asList(paths).contains(SnakeShieldComponent.SHIELD_TEXTURE)
                        && Arrays.asList(paths).contains(SnakeShieldPickupComponent.GEM_TEXTURE)
                        && Arrays.asList(paths)
                            .contains(SnakeShieldPickupComponent.SPAWN_TEXTURE)));
  }
}
