package com.csse3200.game.items;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.maingame.ConsumableHotbarDisplay;
import com.csse3200.game.components.rooms.RoomAssets;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Keeps Team 5's item and hotbar assets in the shared game resource lifecycle. */
@ExtendWith(GameExtension.class)
class ConsumableRoomAssetsIntegrationTest {
  private AssetManager assetManager;
  private RoomAssets roomAssets;

  @BeforeEach
  void setUp() {
    assetManager = mock(AssetManager.class);
    Music music = mock(Music.class);
    when(assetManager.get("sounds/BGM_03_mp3.mp3", Music.class)).thenReturn(music);
    ServiceLocator.registerResourceService(new ResourceService(assetManager));
    roomAssets = new RoomAssets();
  }

  @Test
  void shouldPreloadTexturesNeededByItemPickupsAndConsumableHotbar() {
    roomAssets.loadAll();

    for (String texture : requiredTextures()) {
      verify(assetManager).load(texture, Texture.class);
    }
  }

  @Test
  void shouldReleaseItemAndHotbarTexturesWhenLeavingGame() {
    roomAssets.loadAll();
    roomAssets.dispose();

    for (String texture : requiredTextures()) {
      verify(assetManager).unload(texture);
    }
  }

  private static Set<String> requiredTextures() {
    Set<String> textures = new HashSet<>();
    for (String itemId : ItemCatalog.ids()) {
      textures.add(ItemCatalog.create(itemId, 1).getTexture());
    }
    textures.add(ConsumableHotbarDisplay.IDLE_FRAME_TEXTURE);
    textures.add(ConsumableHotbarDisplay.SELECTED_FRAME_TEXTURE);
    return textures;
  }
}
