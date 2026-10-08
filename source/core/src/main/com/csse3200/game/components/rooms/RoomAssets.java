package com.csse3200.game.components.rooms;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.components.boss.FinalBossStageThreeAssets;
import com.csse3200.game.components.boss.FinalBossStageTwoAssets;
import com.csse3200.game.components.boss.FinalBossVisualAssets;
import com.csse3200.game.components.miniboss.snake.SnakePlayerHitVisualComponent;
import com.csse3200.game.components.miniboss.snake.SnakePoisonAssets;
import com.csse3200.game.components.miniboss.snake.SnakeShieldComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldPickupComponent;
import com.csse3200.game.components.traps.FireTrapRenderComponent;
import com.csse3200.game.components.traps.IceTrapRenderComponent;
import com.csse3200.game.files.AudioLevels;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.minimap.Minimap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads the terrain, fixtures, audio, and enemy assets used by a room.
 *
 * <p>Music provenance and the credit the boss theme's licence requires are recorded in {@code
 * assets/licenses/AUDIO-ATTRIBUTION.md}.
 */
public class RoomAssets implements Disposable {
  /** Plays in the hub, and in any dungeon room once nothing is left alive in it. */
  private static final String CALM_MUSIC = "sounds/lobby_music.mp3";

  /** Plays in a dungeon room that still has enemies in it. */
  private static final String FIGHT_MUSIC = "sounds/fight_music.mp3";

  /** Plays while the final boss is still alive. */
  private static final String BOSS_MUSIC = "sounds/boss_music.mp3";

  /**
   * Dungeons that have a theme of their own. Anything not listed here fights to {@link
   * #FIGHT_MUSIC}, so giving a dungeon its own track is one entry rather than a new branch.
   */
  private static final Map<String, String> DUNGEON_MUSIC = Map.of("finalDungeon", BOSS_MUSIC);

  private static final String IMPACT_SOUND = "sounds/Impact4.ogg";
  private static final String[] MUSIC = {CALM_MUSIC, FIGHT_MUSIC, BOSS_MUSIC};
  private static final String[] SOUNDS = {IMPACT_SOUND};

  private static final String[] ENEMY_TEXTURES = {
    "images/hole.png",
    "images/dragon/smoke_sheet.png",
    SnakePlayerHitVisualComponent.HIT_SHEET,
    SnakeShieldComponent.SHIELD_TEXTURE,
    SnakeShieldPickupComponent.GEM_TEXTURE,
    SnakeShieldPickupComponent.SPAWN_TEXTURE
  };

  private static final String[] ENEMY_TEXTURE_ATLASES = {
    "images/bombEnemy.atlas",
    "images/beetle.atlas",
    "images/snake.atlas",
    "images/medusa.atlas",
    "images/mummy.atlas",
    "images/crab.atlas",
    "images/golem.atlas",
    "images/cyclops.atlas",
    "images/wasp.atlas",
    "images/floatingDemon.atlas",
    "images/harpy.atlas",
    "images/cerberus/cerberus-modular.atlas",
    "images/cerberus/cerberus-fireball.atlas",
    "images/cerberus/cerberus-chain.atlas",
    "images/dragon/dragon.atlas",
    "images/dragon/thunder-orb.atlas",
    "images/wolf.atlas",
    "images/horse.atlas",
    "images/crow.atlas",
    "images/minotaur.atlas",
    "images/sandeye.atlas",
    "images/dark_elves.atlas",
    "images/jingwei.atlas",
    "images/zombie.atlas",
    "images/wukong.atlas",
    "images/bug.atlas",
    "images/longwei.atlas",
    "images/knight.atlas",
    "images/jotunn.atlas",
  };

  private static final String[] PLAYER_ATLASES = {"images/idle_down.atlas"};
  private static final String[] PLAYER_TEXTURES = {Minimap.PLAYER_HEAD_PATH};

  private static final String[] DUNGEON_TEXTURES = {
    "images/dungeons/tileSet0.png",
    "images/dungeons/tileSet1.png",
    "images/dungeons/tileSet2.png",
    "images/dungeons/tileSet3.png",
    "images/dungeons/tileSet4.png",
    "images/dungeons/tileSet5.png",
    "images/dungeons/fantasy_dreamland_door.png"
  };

  private static final String[] OBSTACLE_TEXTURES = {
    "images/hole.png", "images/rock.png",
  };

  private static final String[] TRAP_TEXTURES = {
    FireTrapRenderComponent.START_TEXTURE,
    FireTrapRenderComponent.LOOP_TEXTURE,
    FireTrapRenderComponent.END_TEXTURE,
    IceTrapRenderComponent.TEXTURE
  };

  private static final String[] ITEM_TEXTURES = {
    "images/heart.png",
    "images/strength_charm_pixel.png",
    "images/attack_speed_charm.png",
    "images/speed_charm.png",
    "images/health_potion_pixel.png",
    "images/health_potion_small_pixel.png",
    "images/health_potion_medium_pixel.png",
    "images/health_potion_large_pixel.png",
    "images/shield_consumable_pixel.png",
    "images/speed_potion_pixel.png",
    "images/strength_potion_pixel.png",
    "images/freeze_bomb_pixel.png",
    "images/burn_vial_pixel.png",
    "images/magnet_potion_pixel.png",
    "images/consumable-slot-idle.png",
    "images/consumable-slot-selected.png",
    "images/gold_coin_pixel.png"
  };

  private static final String[] ALL_TEXTURES =
      Stream.of(
              DUNGEON_TEXTURES,
              OBSTACLE_TEXTURES,
              TRAP_TEXTURES,
              ITEM_TEXTURES,
              ENEMY_TEXTURES,
              PLAYER_TEXTURES,
              SnakePoisonAssets.paths(),
              FinalBossVisualAssets.paths(),
              FinalBossStageTwoAssets.paths(),
              FinalBossStageThreeAssets.paths())
          .flatMap(Arrays::stream)
          .toArray(String[]::new);

  private static final String[] ALL_ATLASES =
      Stream.of(ENEMY_TEXTURE_ATLASES, PLAYER_ATLASES)
          .flatMap(Arrays::stream)
          .toArray(String[]::new);

  public void loadAll() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(ALL_TEXTURES);
    resourceService.loadTextureAtlases(ALL_ATLASES);
    resourceService.loadMusic(MUSIC);
    resourceService.loadSounds(SOUNDS);
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
  public void playMusicFor(String dungeonId, boolean cleared) {
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

  /** Keeps the playing track on the saved volume, including mute-when-unfocused. */
  public void applyMusicVolume() {
    for (String name : MUSIC) {
      Music track = music(name);
      if (track != null) {
        track.setVolume(AudioLevels.music());
      }
    }
  }

  private void stopMusic() {
    stopAllBut(null);
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

  @Override
  public void dispose() {
    ResourceService resourceService = ServiceLocator.getResourceService();

    stopMusic();
    resourceService.unloadAssets(MUSIC);
    resourceService.unloadAssets(SOUNDS);

    resourceService.unloadAssets(ALL_TEXTURES);
    resourceService.unloadAssets(ALL_ATLASES);
  }
}
