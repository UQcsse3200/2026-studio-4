package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RoomCommandTest {
  private RoomManager roomManager;
  private EntityService entities;
  private WorldConfig world;
  private RoomConfig bossRoom;
  private RoomConfig ordinaryRoom;
  private RoomCommand command;

  @BeforeEach
  void setUp() {
    roomManager = mock(RoomManager.class);
    entities = spy(new EntityService());
    ServiceLocator.registerEntityService(entities);

    bossRoom = new RoomConfig();
    bossRoom.id = "finalBossRoom";
    EnemySpawnConfig bossSpawn = new EnemySpawnConfig();
    bossSpawn.type = EnemyType.FINAL_BOSS;
    bossRoom.enemySpawns = new EnemySpawnConfig[] {bossSpawn};
    ordinaryRoom = new RoomConfig();
    ordinaryRoom.id = "dungeonOneEntrance";
    world = new WorldConfig();
    world.rooms = new RoomConfig[] {bossRoom, ordinaryRoom};
    when(roomManager.getWorld()).thenReturn(world);
    command = new RoomCommand(roomManager);
  }

  @Test
  void finalBossStageOneSchedulesTheFirstStageAfterEntityUpdate() {
    assertSchedulesBossStage("finalBossStage1", FinalBossPhase.STAGE_ONE);
  }

  @Test
  void finalBossStageTwoSchedulesTheSecondStageAfterEntityUpdate() {
    assertSchedulesBossStage("finalBossStage2", FinalBossPhase.STAGE_TWO);
  }

  @Test
  void finalBossStageThreeSchedulesTheThirdStageAfterEntityUpdate() {
    assertSchedulesBossStage("finalBossStage3", FinalBossPhase.STAGE_THREE);
  }

  @Test
  void ordinaryGotoStillSchedulesTheRequestedRoom() {
    assertSchedulesOrdinaryRoom(ordinaryRoom);
  }

  @Test
  void originalFinalBossRoomCommandStillUsesTheNormalRoomTransition() {
    assertSchedulesOrdinaryRoom(bossRoom);
  }

  @Test
  void clearStillClearsImmediatelyWithoutSchedulingATransition() {
    assertTrue(command.action(args("clear")));

    verify(roomManager).clearCurrentRoom();
    verify(entities, never()).schedule(any(Runnable.class));
    verify(roomManager, never()).debugSwitchRoom(any(RoomConfig.class));
    verify(roomManager, never())
        .debugSwitchFinalBossRoom(any(RoomConfig.class), any(FinalBossPhase.class));
  }

  @Test
  void clearDoesNotRequireAWorldOrEntityService() {
    when(roomManager.getWorld()).thenReturn(null);
    ServiceLocator.registerEntityService(null);

    assertTrue(command.action(args("clear")));

    verify(roomManager).clearCurrentRoom();
    verify(entities, never()).schedule(any(Runnable.class));
  }

  @Test
  void malformedArgumentsAreRejectedWithoutSchedulingOrChangingRooms() {
    List<ArrayList<String>> invalid =
        Arrays.asList(
            null,
            args(),
            args((String) null),
            args(""),
            args("unknown"),
            args("goto"),
            args("goto", null),
            args(null, "finalBossStage1"),
            args("goto", ""),
            args("goto", " "),
            args("goto", "finalBossStage2", "extra"),
            args("goto", "finalBossRoom", "extra"),
            args("goto", "dungeonOneEntrance", "extra"),
            args("goto", "finalBossStage2", null),
            args("clear", "extra"),
            args("clear", null),
            args("Goto", "finalBossStage1"),
            args("CLEAR"));

    for (ArrayList<String> input : invalid) {
      assertFalse(assertDoesNotThrow(() -> command.action(input)), "Arguments: " + input);
    }
    assertNoScheduledRoomChange();
  }

  @Test
  void unknownRoomsAndInvalidStageAliasesAreRejected() {
    String[] invalidTargets = {
      "missingRoom",
      "finalBossStage0",
      "finalBossStage4",
      "finalBossStage-1",
      "finalBossStagex",
      "finalBossStage",
      "finalBossStage01",
      "finalBossStage1.0",
      "finalBossStageOne",
      "FinalBossStage1",
      "finalbossstage2",
      "FINALBOSSSTAGE3",
      "finalBossstage1",
      "1",
      "2",
      "3"
    };
    for (String target : invalidTargets) {
      assertFalse(assertDoesNotThrow(() -> command.action(args("goto", target))), target);
    }
    assertNoScheduledRoomChange();
  }

  @Test
  void missingRoomManagerRejectsGotoAndClearGracefully() {
    RoomCommand missingManager = new RoomCommand(null);

    assertFalse(assertDoesNotThrow(() -> missingManager.action(args("goto", "finalBossStage2"))));
    assertFalse(assertDoesNotThrow(() -> missingManager.action(args("goto", "finalBossRoom"))));
    assertFalse(assertDoesNotThrow(() -> missingManager.action(args("clear"))));
    assertNoScheduledRoomChange();
  }

  @Test
  void missingWorldRejectsStageAndOrdinaryGotoGracefully() {
    when(roomManager.getWorld()).thenReturn(null);

    assertFalse(assertDoesNotThrow(() -> command.action(args("goto", "finalBossStage2"))));
    assertFalse(assertDoesNotThrow(() -> command.action(args("goto", "finalBossRoom"))));
    assertNoScheduledRoomChange();
  }

  @Test
  void missingEntityServiceRejectsStageAndOrdinaryGotoGracefully() {
    ServiceLocator.registerEntityService(null);

    assertFalse(assertDoesNotThrow(() -> command.action(args("goto", "finalBossStage2"))));
    assertFalse(assertDoesNotThrow(() -> command.action(args("goto", "finalBossRoom"))));
    assertNoScheduledRoomChange();
  }

  @Test
  void missingFinalBossRoomRejectsAllStageAliases() {
    world.rooms = new RoomConfig[] {ordinaryRoom};

    assertAllStageAliasesRejected();
  }

  @Test
  void stageAliasesRequireAFinalBossSpawnInTheirDestination() {
    EnemySpawnConfig otherEnemy = new EnemySpawnConfig();
    otherEnemy.type = EnemyType.MINOTAUR;
    bossRoom.enemySpawns = new EnemySpawnConfig[] {otherEnemy};

    assertAllStageAliasesRejected();
  }

  @Test
  void emptyOrMissingEnemySpawnsRejectStageAliasesGracefully() {
    bossRoom.enemySpawns = new EnemySpawnConfig[0];
    assertAllStageAliasesRejected();
    bossRoom.enemySpawns = null;
    assertAllStageAliasesRejected();
  }

  @Test
  void terminalRejectsInvalidCommandsWithoutChangingRooms() {
    Terminal terminal = new Terminal();
    terminal.addCommand("room", command);
    for (String message :
        List.of(
            "room",
            "room goto",
            "room goto finalBossStage4",
            "room goto finalBossStage2 extra",
            "room clear extra")) {
      terminal.setEnteredMessage(message);
      assertFalse(assertDoesNotThrow(terminal::processMessage), message);
    }
    assertNoScheduledRoomChange();
  }

  @Test
  void terminalRoutesAValidStageAliasThroughTheDeferredTransition() {
    Terminal terminal = new Terminal();
    terminal.addCommand("room", command);
    terminal.setEnteredMessage("room goto finalBossStage2");

    assertTrue(terminal.processMessage());
    assertNoRoomMutation();
    entities.update();

    verify(roomManager).debugSwitchFinalBossRoom(bossRoom, FinalBossPhase.STAGE_TWO);
    verify(roomManager, never()).debugSwitchRoom(any(RoomConfig.class));
  }

  private void assertSchedulesBossStage(String alias, FinalBossPhase phase) {
    assertTrue(command.action(args("goto", alias)));
    verify(entities).schedule(any(Runnable.class));
    assertNoRoomMutation();

    entities.update();

    verify(roomManager).debugSwitchFinalBossRoom(bossRoom, phase);
    verify(roomManager, never()).debugSwitchRoom(any(RoomConfig.class));
    verify(roomManager, never()).clearCurrentRoom();
    entities.update();
    verify(roomManager, times(1)).debugSwitchFinalBossRoom(bossRoom, phase);
  }

  private void assertSchedulesOrdinaryRoom(RoomConfig destination) {
    assertTrue(command.action(args("goto", destination.id)));
    verify(entities).schedule(any(Runnable.class));
    assertNoRoomMutation();

    entities.update();

    verify(roomManager).debugSwitchRoom(destination);
    verify(roomManager, never())
        .debugSwitchFinalBossRoom(any(RoomConfig.class), any(FinalBossPhase.class));
    verify(roomManager, never()).clearCurrentRoom();
  }

  private void assertAllStageAliasesRejected() {
    for (String target : List.of("finalBossStage1", "finalBossStage2", "finalBossStage3")) {
      assertFalse(assertDoesNotThrow(() -> command.action(args("goto", target))), target);
    }
    assertNoScheduledRoomChange();
  }

  private void assertNoScheduledRoomChange() {
    verify(entities, never()).schedule(any(Runnable.class));
    assertNoRoomMutation();
    entities.update();
    assertNoRoomMutation();
  }

  private void assertNoRoomMutation() {
    verify(roomManager, never()).clearCurrentRoom();
    verify(roomManager, never()).debugSwitchRoom(any(RoomConfig.class));
    verify(roomManager, never())
        .debugSwitchFinalBossRoom(any(RoomConfig.class), any(FinalBossPhase.class));
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(Arrays.asList(values));
  }
}
