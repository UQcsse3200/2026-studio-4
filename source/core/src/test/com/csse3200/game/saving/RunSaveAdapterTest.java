package com.csse3200.game.saving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.RunTimer;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RunSaveAdapterTest {
  @BeforeEach
  void resetRunSaveState() {
    RunSaveState.reset();
  }

  @AfterEach
  void clearRunSaveState() {
    RunSaveState.reset();
  }

  @Test
  void captureIncludesRoomPositionAndRunTimerData() {
    Entity player = player();
    player.setPosition(12.5f, 8.25f);
    RunSaveState.setCurrentRoomId("dungeonOneMiddle");

    RunTimer timer = new RunTimer(mock(GameTime.class));
    timer.restoreRun(95f, Map.of("dungeonOne", 30f));
    timer.startDungeon("dungeonTwo");

    GameSaveData save = new RunSaveAdapter(player, timer).capture();

    assertEquals("dungeonOneMiddle", save.resumePosition.roomId);
    assertEquals(12.5f, save.resumePosition.x);
    assertEquals(8.25f, save.resumePosition.y);
    assertEquals(95f, save.playTimeSeconds);
    assertEquals(30f, save.dungeonTimesSeconds.get("dungeonOne"));
    assertEquals(0f, save.dungeonTimesSeconds.get("dungeonTwo"));
  }

  @Test
  void restorePlayerRestoresPlayerStatsAndRunTimerData() {
    Entity player = player();
    player.getComponent(InventoryComponent.class).setGold(1);
    player.getComponent(CombatStatsComponent.class).setHealth(10);

    GameSaveData save = new GameSaveData();
    save.playTimeSeconds = 80f;
    save.dungeonTimesSeconds.put("dungeonOne", 55f);
    save.playerData.gold = 84;
    save.playerData.health = 45;

    RunTimer timer = new RunTimer(mock(GameTime.class));
    new RunSaveAdapter(player, timer).restorePlayer(save);

    assertEquals(84, player.getComponent(InventoryComponent.class).getGold());
    assertEquals(45, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(80f, timer.getTotalTime());
    assertEquals(55f, timer.getDungeonTimes().get("dungeonOne"));
  }

  private static Entity player() {
    return new Entity()
        .addComponent(new InventoryComponent(0))
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new WeaponUpgradeComponent())
        .addComponent(new SwordWeaponComponent())
        .addComponent(new WeaponSelectionComponent());
  }
}
