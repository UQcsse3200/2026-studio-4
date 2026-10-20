package com.csse3200.game.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class MusicManagerTest {

  @Mock ResourceService resourceService;

  private MusicManager musicManager;

  @BeforeEach
  void setup() {
    ServiceLocator.registerResourceService(resourceService);
    musicManager = new MusicManager();
  }

  @Test
  void shouldLoadEveryTrackWithoutStartingOne() {
    musicManager.load();

    verify(resourceService)
        .loadMusic(
            argThat(
                paths ->
                    Arrays.asList(paths)
                        .containsAll(
                            Arrays.asList(
                                MusicManager.CALM_MUSIC,
                                MusicManager.FIGHT_MUSIC,
                                MusicManager.BOSS_MUSIC))));
    verify(resourceService).loadAll();
    // Which track plays depends on the room, so loading alone must not even reach for one.
    verify(resourceService, never()).getAsset(anyString(), any());
  }

  @Test
  void shouldStartTheTrackTheRoomAsksFor() {
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.play(null, true);

    verify(music).setLooping(true);
    verify(music).play();
  }

  @Test
  void shouldFightOnlyWhileADungeonRoomStillHasEnemies() {
    Music calm = mock(Music.class);
    Music fight = mock(Music.class);
    when(resourceService.getAsset(eq(MusicManager.CALM_MUSIC), any())).thenReturn(calm);
    when(resourceService.getAsset(eq(MusicManager.FIGHT_MUSIC), any())).thenReturn(fight);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.play("dungeonOne", false);
    verify(fight).play();
    verify(calm, never()).play();

    // Clearing the room drops back to the calm track and stops the fight.
    musicManager.play("dungeonOne", true);
    verify(fight).stop();
    verify(calm).play();
  }

  @Test
  void shouldTreatTheHubAsCalmEvenIfSomethingClaimsItIsNotCleared() {
    Music calm = mock(Music.class);
    Music fight = mock(Music.class);
    when(resourceService.getAsset(eq(MusicManager.CALM_MUSIC), any())).thenReturn(calm);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.play(null, false);

    verify(calm).play();
    verify(fight, never()).play();
  }

  @Test
  void shouldPlayTheBossThemeInTheFinalDungeon() {
    Music boss = mock(Music.class);
    Music calm = mock(Music.class);
    when(resourceService.getAsset(eq(MusicManager.BOSS_MUSIC), any())).thenReturn(boss);
    when(resourceService.getAsset(eq(MusicManager.CALM_MUSIC), any())).thenReturn(calm);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.play("finalDungeon", false);
    verify(boss).play();

    // Beating the boss drops back to calm like any other cleared room.
    musicManager.play("finalDungeon", true);
    verify(boss).stop();
    verify(calm).play();
  }

  @Test
  void shouldNotRestartATrackThatIsAlreadyPlaying() {
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);
    when(music.isPlaying()).thenReturn(true);

    // Walking between rooms of one dungeon must not restart its music.
    musicManager.play("dungeonOne", false);
    musicManager.play("dungeonOne", false);

    verify(music, never()).play();
  }

  @Test
  void shouldDoNothingWhenTheTrackIsNotLoaded() {
    when(resourceService.containsAsset(anyString(), any())).thenReturn(false);

    musicManager.play("dungeonOne", false);

    verify(resourceService, never()).getAsset(anyString(), any());
  }

  @Test
  void shouldApplyVolumeToEveryLoadedTrack() {
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.applyVolume();

    verify(music, atLeastOnce()).setVolume(any(Float.class));
  }

  @Test
  void shouldStopAndUnloadEveryTrackOnDispose() {
    Music music = mock(Music.class);
    when(resourceService.getAsset(anyString(), any())).thenReturn(music);
    when(resourceService.containsAsset(anyString(), any())).thenReturn(true);

    musicManager.dispose();

    verify(music, atLeastOnce()).stop();
    verify(resourceService)
        .unloadAssets(
            argThat(
                paths ->
                    Arrays.asList(paths)
                        .containsAll(
                            Arrays.asList(
                                MusicManager.CALM_MUSIC,
                                MusicManager.FIGHT_MUSIC,
                                MusicManager.BOSS_MUSIC))));
  }
}
