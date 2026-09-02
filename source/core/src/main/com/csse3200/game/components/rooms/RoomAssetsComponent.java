package com.csse3200.game.components.rooms;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Loads the terrain, fixtures, audio, and enemy assets used by a room. */
public class RoomAssetsComponent extends Component {
  private static final String BACKGROUND_MUSIC = "sounds/BGM_03_mp3.mp3";
  private static final String IMPACT_SOUND = "sounds/Impact4.ogg";
  private static final String ROCK_TEXTURE = "images/rock.png";
  private static final String FANTASY_DUNGEON_TILESET = "images/dungeons/fantasy_dreamland_16.png";
  private static final String FANTASY_DUNGEON_DOOR = "images/dungeons/fantasy_dreamland_door.png";
  private static final String SWORD_TEXTURE = "images/weapons/sword.png";
  private static final String KNIFE_TEXTURE = "images/weapons/knife.png";
  private static final String THROWING_KNIFE_TEXTURE = "images/weapons/throwing_knife.png";

  private static final String[] TEXTURES = {
    ROCK_TEXTURE,
    FANTASY_DUNGEON_TILESET,
    FANTASY_DUNGEON_DOOR,
    SWORD_TEXTURE,
    KNIFE_TEXTURE,
    THROWING_KNIFE_TEXTURE
  };
  private static final String[] MUSIC = {BACKGROUND_MUSIC};
  private static final String[] SOUNDS = {IMPACT_SOUND};
  private static final String[] TEXTURE_ATLASES = {
    "images/bombEnemy.atlas", "images/chaseEnemy.atlas", "images/floatingDemon.atlas"
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
}
