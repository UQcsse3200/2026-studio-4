package com.csse3200.game.components.rooms;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.areas.terrain.TerrainConfig;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Loads the terrain, audio, and player-action assets used by a basic room. */
public class RoomAssetsComponent extends Component {
  private static final String GROUND_TEXTURE = "images/grass_1.png";
  private static final String ALTERNATE_TEXTURE_ONE = "images/grass_2.png";
  private static final String ALTERNATE_TEXTURE_TWO = "images/grass_3.png";
  private static final String BACKGROUND_MUSIC = "sounds/BGM_03_mp3.mp3";
  private static final String IMPACT_SOUND = "sounds/Impact4.ogg";
  private static final String ROCK_TEXTURE = "images/rock.png";
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";

  private static final String[] TEXTURES = {
    GROUND_TEXTURE,
    ALTERNATE_TEXTURE_ONE,
    ALTERNATE_TEXTURE_TWO,
    ROCK_TEXTURE,
    FANTASY_DUNGEON_TILESET
  };
  private static final String[] MUSIC = {BACKGROUND_MUSIC};
  private static final String[] SOUNDS = {IMPACT_SOUND};
  private static final String[] TEXTURE_ATLASES = {
    "images/bombEnemy.atlas", "images/chaseEnemy.atlas", "images/idle_down.atlas"
  };

  public RoomAssetsComponent() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(TEXTURES);
    resourceService.loadTextureAtlases(TEXTURE_ATLASES);
    resourceService.loadMusic(MUSIC);
    resourceService.loadSounds(SOUNDS);
    resourceService.loadAll();
  }

  @Override
  public void create() {
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    music.setLooping(true);
    music.setVolume(0.3f);
    music.play();
  }

  @Override
  public void dispose() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.getAsset(BACKGROUND_MUSIC, Music.class).stop();
    resourceService.unloadAssets(TEXTURES);
    resourceService.unloadAssets(TEXTURE_ATLASES);
    resourceService.unloadAssets(MUSIC);
    resourceService.unloadAssets(SOUNDS);
  }

  /** Supplies this room's terrain configuration with its loaded floor texture paths. */
  public void setTerrainConfig(TerrainConfig terrainConfig) {
    terrainConfig.setTextures(GROUND_TEXTURE, ALTERNATE_TEXTURE_ONE, ALTERNATE_TEXTURE_TWO);
  }
}
