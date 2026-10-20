package com.csse3200.game.components.rooms;

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
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.entities.factories.TutorialPropFactory;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.minimap.Minimap;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Loads the terrain, fixtures, sound effects, and enemy assets used by a room.
 *
 * <p>Background music is owned by {@link com.csse3200.game.rooms.MusicManager}.
 */
public class RoomAssets implements Disposable {
  private static final String ACHIEVEMENT_SOUND = "sounds/achievementSound.mp3";

  private static final String IMPACT_SOUND = "sounds/Impact4.ogg";
  private static final String[] SOUNDS = {IMPACT_SOUND, ACHIEVEMENT_SOUND};

  private static final String[] ENEMY_TEXTURES = {
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
    "images/minotaur-combat/attacks.atlas",
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
    RoomFactory.DEFAULT_TILESET,
    RoomFactory.DUNGEON1_TILESET,
    RoomFactory.DUNGEON2_TILESET,
    RoomFactory.DUNGEON3_TILESET,
    RoomFactory.DUNGEON4_TILESET,
    RoomFactory.DUNGEON5_TILESET,
    "images/dungeons/fantasy_dreamland_door.png"
  };

  private static final String[] OBSTACLE_TEXTURES = {"images/rock.png"};

  private static final String[] TRAP_TEXTURES = {
    FireTrapRenderComponent.START_TEXTURE,
    FireTrapRenderComponent.LOOP_TEXTURE,
    FireTrapRenderComponent.END_TEXTURE,
    IceTrapRenderComponent.TEXTURE
  };

  private static final String[] ITEM_TEXTURES = {
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
              TutorialPropFactory.textures(),
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
    resourceService.loadSounds(SOUNDS);
    resourceService.loadAll();
  }

  @Override
  public void dispose() {
    ResourceService resourceService = ServiceLocator.getResourceService();

    resourceService.unloadAssets(SOUNDS);

    resourceService.unloadAssets(ALL_TEXTURES);
    resourceService.unloadAssets(ALL_ATLASES);
  }
}
