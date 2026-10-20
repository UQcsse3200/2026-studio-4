package com.csse3200.game.rooms;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.files.AudioLevels;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Map;

/**
 * Owns the background music for the main game: loads the tracks, picks which one fits the current
 * room, keeps its volume in line with the settings, and releases everything on dispose.
 *
 * <p>Music provenance and the credit the boss theme's licence requires are recorded in {@code
 * assets/licenses/AUDIO-ATTRIBUTION.md}.
 */
public class MusicManager implements Disposable {
  /** Plays in the hub, and in any dungeon room once nothing is left alive in it. */
  static final String CALM_MUSIC = "sounds/lobby_music.mp3";

  /** Plays in a dungeon room that still has enemies in it. */
  static final String FIGHT_MUSIC = "sounds/fight_music.mp3";

  /** Plays while the final boss is still alive. */
  static final String BOSS_MUSIC = "sounds/boss_music.mp3";

  /**
   * Dungeons that have a theme of their own. Anything not listed here fights to {@link
   * #FIGHT_MUSIC}, so giving a dungeon its own track is one entry rather than a new branch.
   */
  private static final Map<String, String> DUNGEON_MUSIC = Map.of("finalDungeon", BOSS_MUSIC);

  private static final String[] MUSIC = {CALM_MUSIC, FIGHT_MUSIC, BOSS_MUSIC};

  /** Queues every track on the resource service and waits for them to finish loading. */
  public void load() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadMusic(MUSIC);
    resourceService.loadAll();
  }

  /**
   * Switches to the track the player's situation calls for: the calm hub track whenever there is
   * nothing left to fight, and otherwise whatever the dungeon fights to, which is the boss theme in
   * the final dungeon and the ordinary fight theme everywhere else.
   *
   * <p>Does nothing when the wanted track is already playing, so crossing rooms of one dungeon, or
   * re-entering a room already cleared, never restarts it from the top.
   *
   * @param dungeonId the dungeon the room belongs to, or null for the hub
   * @param cleared whether the room has no enemies left
   */
  public void play(String dungeonId, boolean cleared) {
    String wanted =
        dungeonId == null || cleared
            ? CALM_MUSIC
            : DUNGEON_MUSIC.getOrDefault(dungeonId, FIGHT_MUSIC);
    Music track = music(wanted);
    if (track == null || track.isPlaying()) {
      return;
    }
    stopAllBut(wanted);
    track.setLooping(true);
    track.setVolume(AudioLevels.music());
    track.play();
  }

  /** Keeps the tracks on the saved volume, including mute-when-unfocused. Call every frame. */
  public void applyVolume() {
    for (String name : MUSIC) {
      Music track = music(name);
      if (track != null) {
        track.setVolume(AudioLevels.music());
      }
    }
  }

  /** Silences every track except the one about to take over, which may be null to stop them all. */
  private void stopAllBut(String keep) {
    for (String name : MUSIC) {
      if (name.equals(keep)) {
        continue;
      }
      Music track = music(name);
      if (track != null) {
        track.stop();
      }
    }
  }

  /** Null rather than throwing, so a track that is not loaded cannot take the game down. */
  private Music music(String name) {
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService == null || !resourceService.containsAsset(name, Music.class)) {
      return null;
    }
    return resourceService.getAsset(name, Music.class);
  }

  /** Stops whatever is playing and unloads every track. */
  @Override
  public void dispose() {
    stopAllBut(null);
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService != null) {
      resourceService.unloadAssets(MUSIC);
    }
  }
}
