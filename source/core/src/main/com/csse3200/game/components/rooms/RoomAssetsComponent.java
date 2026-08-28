package com.csse3200.game.components.rooms;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.areas.terrain.TerrainConfig;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the provided textures into the resourceService. Any textures used within the room should
 * exist here. Different rooms may use different textures.
 *
 * <p>The assets are loaded to the resource service on constructor call.
 */
public class RoomAssetsComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(RoomAssetsComponent.class);
  private final String tileTexture1 = "images/grass_1.png";
  private final String tileTexture2 = "images/grass_2.png";
  private final String tileTexture3 = "images/grass_3.png";

  private final String backgroundMusic = "sounds/BGM_03_mp3.mp3";

  private final String[] textures = {tileTexture1, tileTexture2, tileTexture3};
  private final String[] music = {backgroundMusic};
  private final String[] sounds = {"sounds/Impact4.ogg"};
  private final String[] enemyTextures = {"images/ghost_1.png", "images/ghost_king.png"};
  private final String[] obstacleTextures = {
    "images/tree.png", "images/rock.png", "images/hole.png"
  };
  private final String[] enemyTextureAtlases = {
    "images/terrain_iso_grass.atlas", "images/ghost.atlas", "images/ghostKing.atlas"
  };

  @Override
  public void create() {
    playMusic();
  }

  @Override
  public void dispose() {
    ServiceLocator.getResourceService().getAsset(backgroundMusic, Music.class).stop();
    unloadAssets();
  }

  public RoomAssetsComponent() {
    loadAssets();
  }

  /**
   * Sets the tiletextures paths into a TerrainConfig.
   *
   * @param terrainConfig
   */
  public void setTerrainConfig(TerrainConfig terrainConfig) {
    terrainConfig.setTextures(tileTexture1, tileTexture2, tileTexture3);
  }

  private void loadAssets() {
    logger.debug("loading assets");
    final ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(textures);
    resourceService.loadTextures(enemyTextures);
    resourceService.loadTextures(obstacleTextures);
    resourceService.loadTextureAtlases(enemyTextureAtlases);

    resourceService.loadMusic(music);
    resourceService.loadSounds(sounds);

    while (!resourceService.loadForMillis(10)) {
      // This could be upgraded to a loading screen
      logger.info("Loading... {}%", resourceService.getProgress());
    }
  }

  /**
   * Starts playing the background music.
   *
   * @requires The music to be loaded into the resource service.
   */
  private void playMusic() {
    final Music music = ServiceLocator.getResourceService().getAsset(backgroundMusic, Music.class);
    music.setLooping(true);
    music.setVolume(0.3f);
    music.play();
  }

  /** Unloads the assets used in the room */
  private void unloadAssets() {
    logger.debug("unloading assets");
    final ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.unloadAssets(textures);
    resourceService.unloadAssets(enemyTextures);
    resourceService.unloadAssets(enemyTextureAtlases);
    resourceService.unloadAssets(music);
    resourceService.unloadAssets(sounds);
  }
}
