package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.rooms.RoomAcheivements;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AchievementReportersTest {
  private AchievementService service;
  private RunTimer timer;

  @BeforeEach
  void setUp() {
    service = mock(AchievementService.class);
    timer = mock(RunTimer.class);
    ServiceLocator.registerAchievementService(service);
    ServiceLocator.registerRunTimer(timer);
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  private AchievementContext capturedContext() {
    ArgumentCaptor<AchievementContext> captor = ArgumentCaptor.forClass(AchievementContext.class);
    verify(service).update(captor.capture());
    return captor.getValue();
  }

  // ---- AchievementProgressComponent ----

  private Entity player() {
    Entity player = new Entity().addComponent(new AchievementProgressComponent());
    player.create();
    return player;
  }

  @Test
  void damageTakenReportsPlayerDamaged() {
    player().getEvents().trigger("damageTaken", (Entity) null, 5, 95);
    assertTrue(capturedContext().playerDamaged);
  }

  @Test
  void goldPickedUpReportsAmount() {
    player().getEvents().trigger(AchievementProgressComponent.GOLD_PICKED_UP, 7);
    assertEquals(7, capturedContext().goldCollected);
  }

  @Test
  void progressComponentToleratesMissingService() {
    ServiceLocator.clear();
    assertDoesNotThrow(
        () -> new Entity().addComponent(new AchievementProgressComponent()).create());
  }

  // ---- RoomAcheivements ----

  private Entity room() {
    Entity room = new Entity().addComponent(new RoomAcheivements());
    room.create();
    return room;
  }

  @Test
  void dungeonClearedReportsIdAndTime() {
    when(timer.getDungeonTime()).thenReturn(42f);
    room().getEvents().trigger(RoomClearService.DUNGEON_CLEARED, "dungeonOne");

    AchievementContext ctx = capturedContext();
    assertEquals("dungeonOne", ctx.dungeonCompletedId);
    assertEquals("dungeonOne", ctx.dungeonId);
    assertEquals(42f, ctx.dungeonSeconds);
  }

  @Test
  void switchingDungeonRestartsTimerAndReportsEntered() {
    RoomConfig oldRoom = new RoomConfig();
    oldRoom.dungeonId = "dungeonOne";
    RoomConfig newRoom = new RoomConfig();
    newRoom.dungeonId = "dungeonTwo";

    room().getEvents().trigger(RoomSelector.ROOM_SWITCHED_EVENT, oldRoom, newRoom);

    verify(timer).stopDungeon();
    verify(timer).startDungeon("dungeonTwo");
    assertEquals("dungeonTwo", capturedContext().dungeonEnteredId);
  }

  @Test
  void switchingRoomWithinSameDungeonDoesNothing() {
    RoomConfig a = new RoomConfig();
    a.dungeonId = "dungeonOne";
    RoomConfig b = new RoomConfig();
    b.dungeonId = "dungeonOne";

    room().getEvents().trigger(RoomSelector.ROOM_SWITCHED_EVENT, a, b);

    verify(service, never()).update(any());
    verify(timer, never()).startDungeon(any());
  }
}
