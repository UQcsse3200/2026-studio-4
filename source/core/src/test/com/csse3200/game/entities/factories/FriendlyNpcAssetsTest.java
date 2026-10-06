package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the art every friendly NPC asks for actually exists and says what the config claims.
 *
 * <p>{@link InteractableNpcConfigs#validate()} only checks that an atlas is paired with an
 * animation name; nothing proves the file is there or that the region is spelled the same way. A
 * mismatch is otherwise a crash on walking into the room that spawns the NPC.
 */
@ExtendWith(GameExtension.class)
class FriendlyNpcAssetsTest {
  @Test
  void everyFriendlyNpcAtlasExistsAndDeclaresItsAnimation() {
    for (InteractableNpcConfig npc : load().npcs) {
      if (npc.atlas == null) {
        continue;
      }
      FileHandle file = Gdx.files.internal(npc.atlas);
      assertTrue(file.exists(), npc.id + " is missing its atlas " + npc.atlas);

      TextureAtlasData data = new TextureAtlasData(file, file.parent(), false);
      assertTrue(
          hasRegion(data, npc.animation),
          npc.id + " atlas " + npc.atlas + " has no region named " + npc.animation);

      for (Region region : data.getRegions()) {
        assertTrue(region.width > 0 && region.height > 0, npc.id + " region has no size");
      }
    }
  }

  @Test
  void everyFriendlyNpcTextureExists() {
    for (InteractableNpcConfig npc : load().npcs) {
      if (npc.atlas == null && npc.texture != null) {
        assertTrue(
            Gdx.files.internal(npc.texture).exists(),
            npc.id + " is missing its texture " + npc.texture);
      }
    }
  }

  @Test
  void atlasPagesPointAtImagesThatExist() {
    for (InteractableNpcConfig npc : load().npcs) {
      if (npc.atlas == null) {
        continue;
      }
      FileHandle file = Gdx.files.internal(npc.atlas);
      TextureAtlasData data = new TextureAtlasData(file, file.parent(), false);
      assertFalse(data.getPages().isEmpty(), npc.id + " atlas declares no page");
      data.getPages()
          .forEach(
              page ->
                  assertTrue(
                      page.textureFile.exists(),
                      npc.id + " atlas points at a missing image " + page.textureFile.path()));
    }
  }

  @Test
  void hecateAtlasCarvesHerOwnFigureOutOfTheSharedRoguesSheet() {
    FileHandle file = Gdx.files.internal("images/hecate.atlas");
    assertTrue(file.exists(), "images/hecate.atlas is missing");

    TextureAtlasData data = new TextureAtlasData(file, file.parent(), false);
    Region region = null;
    for (Region candidate : data.getRegions()) {
      if ("default".equals(candidate.name)) {
        region = candidate;
      }
    }
    assertNotNull(region, "hecate.atlas must declare a 'default' region");

    // rogues.png is shared with the Travelling Merchant, who takes 64,192. Pinning the coordinates
    // means repacking the sheet fails here rather than silently giving Hecate someone else's body.
    assertEquals(0, region.left);
    assertEquals(128, region.top);
    assertEquals(32, region.width);
    assertEquals(32, region.height);
  }

  private static InteractableNpcConfigs load() {
    InteractableNpcConfigs configs =
        FileLoader.readClass(InteractableNpcConfigs.class, InteractableNpcConfigs.CONFIG_PATH);
    assertNotNull(configs, "friendlyNpcs.json failed to parse");
    return configs;
  }

  private static boolean hasRegion(TextureAtlasData data, String name) {
    for (Region region : data.getRegions()) {
      if (region.name.equals(name)) {
        return true;
      }
    }
    return false;
  }
}
