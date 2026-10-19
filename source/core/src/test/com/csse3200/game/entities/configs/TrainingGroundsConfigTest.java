package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.cutscene.DialogueScript;
import com.csse3200.game.components.rooms.configs.EntryPointConfig;
import com.csse3200.game.components.rooms.configs.NpcSpawnConfig;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.PropSpawnConfig;
import com.csse3200.game.components.rooms.configs.PropSpawnConfig.PropType;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.factories.TutorialPropFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the shipped training grounds: the room, its props, and the NPCs that link to it. */
@ExtendWith(GameExtension.class)
class TrainingGroundsConfigTest {
  private static final String ROOM_ID = "tutorialRoom";
  private static final String HUB_ID = "selection";
  private static final String FLOORS = "2345678";

  private WorldConfig world;
  private InteractableNpcConfigs npcs;
  private RoomConfig room;

  @BeforeEach
  void setUp() {
    world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    npcs = FileLoader.readClass(InteractableNpcConfigs.class, "configs/friendlyNpcs.json");
    room = world.getRoom(ROOM_ID);
  }

  private char tileAt(RoomConfig config, int x, int y) {
    return config.obstacles.spawns.get(y).charAt(x);
  }

  private boolean isFloor(RoomConfig config, int x, int y) {
    return FLOORS.indexOf(tileAt(config, x, y)) >= 0;
  }

  @Test
  void worldStillValidatesWithTheRoomAdded() {
    assertDoesNotThrow(world::validate);
    assertDoesNotThrow(npcs::validate);
  }

  @Test
  void roomIsAPracticeSpaceWithNoEnemiesOrExits() {
    assertNotNull(room);
    assertTrue(room.trainingGrounds);
    assertEquals(0, room.enemySpawns.length, "enemies would block clearing and drop loot");
    assertEquals(0, room.exits.length, "the Sage is the only way out");
    assertNull(room.dungeonId, "entering must not start a dungeon timer");
  }

  @Test
  void otherRoomsAreNotTrainingGrounds() {
    for (RoomConfig other : world.rooms) {
      if (!ROOM_ID.equals(other.id)) {
        assertFalse(other.trainingGrounds, other.id);
        assertEquals(0, other.propSpawns.length, other.id);
      }
    }
  }

  @Test
  void mapGridMatchesItsDeclaredSize() {
    assertEquals(room.mapHeight, room.obstacles.spawns.size());
    for (String row : room.obstacles.spawns) {
      assertEquals(room.mapWidth, row.length());
    }
  }

  @Test
  void bothSagesOfferToTeleportToEachOther() {
    InteractableNpcConfig hubSage = npcs.get("tutorialGuide");
    InteractableNpcConfig trainingSage = npcs.get("trainingGuide");

    assertEquals(ROOM_ID, hubSage.teleportOffer.roomId);
    assertEquals(HUB_ID, trainingSage.teleportOffer.roomId);
    for (InteractableNpcConfig sage : new InteractableNpcConfig[] {hubSage, trainingSage}) {
      RoomConfig destination = world.getRoom(sage.teleportOffer.roomId);
      assertNotNull(destination.getEntryPoint(sage.teleportOffer.entryPointId), sage.id);
    }
  }

  @Test
  void trainingSageStandsInTheRoomAndHubSageStaysInTheHub() {
    assertTrue(spawnsNpc(room, "trainingGuide"));
    assertTrue(spawnsNpc(world.getRoom(HUB_ID), "tutorialGuide"));
    assertFalse(spawnsNpc(world.getRoom(HUB_ID), "trainingGuide"));
  }

  private static boolean spawnsNpc(RoomConfig config, String npcId) {
    for (NpcSpawnConfig spawn : config.npcSpawns) {
      if (npcId.equals(spawn.npcId)) {
        return true;
      }
    }
    return false;
  }

  @Test
  void arrivalsLandOnFloorInBothDirections() {
    // RoomSelector moves a spawn by (+2, -2) to keep the player out of walls.
    for (String[] route : new String[][] {{ROOM_ID, "fromSage"}, {HUB_ID, "fromTutorial"}}) {
      RoomConfig destination = world.getRoom(route[0]);
      EntryPointConfig entry = destination.getEntryPoint(route[1]);
      assertNotNull(entry, route[1]);
      assertTrue(isFloor(destination, entry.x + 2, entry.y - 2), route[1] + " lands in a wall");
    }
  }

  @Test
  void everyPropAndNpcStandsOnOpenFloorInsideTheMap() {
    Set<String> taken = new HashSet<>();
    for (PositionConfig position : positionsInRoom()) {
      assertTrue(position.x >= 0 && position.x < room.mapWidth, "x " + position.x);
      assertTrue(position.y >= 0 && position.y < room.mapHeight, "y " + position.y);
      assertTrue(isFloor(room, position.x, position.y), "(" + position.x + "," + position.y + ")");
      assertTrue(taken.add(position.x + "," + position.y), "two things on one tile");
    }
  }

  private java.util.List<PositionConfig> positionsInRoom() {
    java.util.List<PositionConfig> all = new java.util.ArrayList<>();
    all.addAll(java.util.List.of(room.propSpawns));
    all.addAll(java.util.List.of(room.npcSpawns));
    return all;
  }

  @Test
  void roomTeachesEveryMechanic() {
    Map<PropType, Integer> counts = new EnumMap<>(PropType.class);
    for (PropSpawnConfig prop : room.propSpawns) {
      counts.merge(prop.type, 1, Integer::sum);
    }

    for (PropType type : PropType.values()) {
      assertTrue(counts.getOrDefault(type, 0) > 0, "no " + type + " in the room");
    }
  }

  @Test
  void everySignReadsARealDialogue() {
    for (PropSpawnConfig prop : room.propSpawns) {
      if (prop.type == PropType.SIGN) {
        DialogueScript script = DialogueScript.load(prop.dialogueId);
        assertNotNull(script, "missing dialogue " + prop.dialogueId);
        assertTrue(script.hasLines());
        for (DialogueScript.Line line : script.lines) {
          assertNotNull(script.findSpeaker(line.speaker), prop.dialogueId + ": " + line.speaker);
        }
        for (DialogueScript.Speaker speaker : script.speakers) {
          assertTrue(Gdx.files.internal(speaker.texture).exists(), speaker.texture);
        }
      }
    }
  }

  @Test
  void everySupplyStandLendsARealConsumableWithAnIcon() {
    Set<String> offered = new HashSet<>();
    for (PropSpawnConfig prop : room.propSpawns) {
      if (prop.type == PropType.SUPPLY_STAND) {
        assertTrue(ItemCatalog.contains(prop.itemId), prop.itemId);
        assertTrue(Gdx.files.internal(TutorialPropFactory.iconPath(prop.itemId)).exists());
        assertTrue(offered.add(prop.itemId), "duplicate stand for " + prop.itemId);
      }
    }
    assertTrue(offered.contains(ItemIds.FREEZE_BOMB));
    assertTrue(offered.contains(ItemIds.BURN_VIAL));
  }

  @Test
  void cannonsAreConfiguredToFireAtAReadableRate() {
    for (PropSpawnConfig prop : room.propSpawns) {
      if (prop.type == PropType.DODGE_CANNON) {
        assertTrue(prop.interval >= 1f, "orbs need room to be dodged");
        assertTrue(prop.range > 0f);
        assertTrue(prop.delay >= 0f);
      }
    }
  }

  @Test
  void trainingGuideDialogueLoads() {
    DialogueScript script = DialogueScript.load(npcs.get("trainingGuide").dialogueId);

    assertNotNull(script);
    assertTrue(script.hasLines());
  }

  @Test
  void teleportOffersNeedADestination() {
    InteractableNpcConfigs configs = new InteractableNpcConfigs();
    InteractableNpcConfig npc = new InteractableNpcConfig();
    npc.id = "guide";
    npc.name = "Guide";
    npc.dialogueId = "guide";
    npc.texture = "images/rock.png";
    configs.npcs = new InteractableNpcConfig[] {npc};
    assertDoesNotThrow(configs::validate, "no offer is fine");

    npc.teleportOffer = new InteractableNpcConfig.TeleportOffer();
    assertThrows(IllegalArgumentException.class, configs::validate);

    npc.teleportOffer.roomId = "somewhere";
    assertThrows(IllegalArgumentException.class, configs::validate, "entry point is needed too");

    npc.teleportOffer.entryPointId = "gate";
    assertDoesNotThrow(configs::validate);
  }
}
