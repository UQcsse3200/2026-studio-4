package com.csse3200.game.components.tutorial;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.rooms.configs.PropSpawnConfig;
import com.csse3200.game.components.rooms.configs.PropSpawnConfig.PropType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TutorialPropManagerComponentTest {
  private static PropSpawnConfig spawn(PropType type) {
    PropSpawnConfig spawn = new PropSpawnConfig();
    spawn.type = type;
    return spawn;
  }

  private static TutorialPropManagerComponent managerFor(PropSpawnConfig... spawns) {
    TutorialPropManagerComponent manager = new TutorialPropManagerComponent(spawns);
    new Entity().addComponent(manager).create();
    return manager;
  }

  @Test
  void shouldSkipAPropWithNoTypeInsteadOfFailingTheRoom() {
    TutorialPropManagerComponent manager = managerFor(spawn(null));

    assertDoesNotThrow(() -> manager.spawnProps(new Entity()));
  }

  @Test
  void shouldSkipASignWithNoDialogue() {
    TutorialPropManagerComponent manager = managerFor(spawn(PropType.SIGN));

    assertDoesNotThrow(() -> manager.spawnProps(new Entity()));
  }

  @Test
  void shouldSkipASupplyStandForAnUnknownItem() {
    PropSpawnConfig stand = spawn(PropType.SUPPLY_STAND);
    stand.itemId = "NOT_AN_ITEM";
    TutorialPropManagerComponent manager = managerFor(stand, spawn(PropType.SUPPLY_STAND));

    assertDoesNotThrow(() -> manager.spawnProps(new Entity()));
  }

  @Test
  void shouldFindNothingToUseInARoomWithNoProps() {
    TutorialPropManagerComponent manager = managerFor();
    manager.spawnProps(new Entity());

    assertNull(manager.findNearestInRange(new Entity()));
  }
}
