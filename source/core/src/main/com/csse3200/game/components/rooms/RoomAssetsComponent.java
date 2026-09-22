package com.csse3200.game.components.rooms;

import java.util.Arrays;
import java.util.stream.Stream;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.components.boss.FinalBossStageThreeAssets;
import com.csse3200.game.components.boss.FinalBossVisualAssets;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Loads the terrain, fixtures, audio, and enemy assets used by a room. */
public class RoomAssetsComponent implements Disposable {
  private static final String BACKGROUND_MUSIC = "sounds/BGM_03_mp3.mp3";
  private static final String IMPACT_SOUND = "sounds/Impact4.ogg";
  private static final String[] MUSIC = {BACKGROUND_MUSIC};
  private static final String[] SOUNDS = {IMPACT_SOUND};

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
    "images/cerberus.atlas",
  };

  private static final String[] PLAYER_ATLASES = { "images/idle_down.atlas" };

  private static final String[] DUNGEON_TEXTURES = {
    "images/dungeons/fantasy_dreamland_16.png", // tile set texture
    "images/dungeons/fantasy_dreamland_door.png", // door texture
  };

  private static final String[] OBSTACLE_TEXTURES = {
    "images/hole.png",
    "images/rock.png",
  };

  private static final String[] ITEM_TEXTURES = {
    "images/heart.png",
    "images/strength_charm_pixel.png",
    "images/attack_speed_charm.png",
    "images/speed_charm.png",
    "images/health_potion_pixel.png",
    "images/shield_consumable_pixel.png",
    "images/speed_potion_pixel.png",
    "images/strength_potion_pixel.png",
    "images/gold_coin_pixel.png"
  };

  private static final String[] ALL_TEXTURES = Stream.of(
    DUNGEON_TEXTURES, OBSTACLE_TEXTURES, ITEM_TEXTURES, FinalBossVisualAssets.paths(), FinalBossStageThreeAssets.paths()
  ).flatMap(Arrays::stream).toArray(String[]::new);

  private static final String[] ALL_ATLASES = Stream.of(
    ENEMY_TEXTURE_ATLASES, PLAYER_ATLASES
  ).flatMap(Arrays::stream).toArray(String[]::new);

  public void loadAll() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(ALL_TEXTURES);
    resourceService.loadTextureAtlases(ALL_ATLASES);
    resourceService.loadMusic(MUSIC);
    resourceService.loadSounds(SOUNDS);
    resourceService.loadAll();

    startMusic();
  }

  private void startMusic() {
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    music.setLooping(true);
    music.setVolume(0.3f);
    music.play();
  }

  private void stopMusic() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.getAsset(BACKGROUND_MUSIC, Music.class).stop();
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
