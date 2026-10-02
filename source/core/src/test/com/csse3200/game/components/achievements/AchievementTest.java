package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class AchievementsManagerTest {

  private Entity room;
  private EventHandler events;
  private AchievementsManager achievementsManager;

  @BeforeEach
  void setUp() {
    room = mock(Entity.class);
    events = mock(EventHandler.class);

    when(room.getEvents()).thenReturn(events);

    achievementsManager = new AchievementsManager(room);
  }

  @Test
  void shouldCreateFinalBossAchievement() {
    verify(events).addListener(eq("FinalBossDefeated"), any(EventListener0.class));
  }

  @Test
  void shouldCreateFinalBossAchievementWithCorrectName() {
    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(events).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    listenerCaptor.getValue().handle();

    verify(events).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldMoveAchievementToNewRoom() {
    Entity newRoom = mock(Entity.class);
    EventHandler newEvents = mock(EventHandler.class);

    when(newRoom.getEvents()).thenReturn(newEvents);

    achievementsManager.newRoom(newRoom);

    verify(newEvents).addListener(eq("FinalBossDefeated"), any(EventListener0.class));
  }

  @Test
  void shouldUnlockAchievementInNewRoom() {
    Entity newRoom = mock(Entity.class);
    EventHandler newEvents = mock(EventHandler.class);

    when(newRoom.getEvents()).thenReturn(newEvents);

    achievementsManager.newRoom(newRoom);

    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(newEvents).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    listenerCaptor.getValue().handle();

    verify(newEvents).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldAllowMovingToMultipleNewRooms() {
    Entity secondRoom = mock(Entity.class);
    EventHandler secondEvents = mock(EventHandler.class);

    Entity thirdRoom = mock(Entity.class);
    EventHandler thirdEvents = mock(EventHandler.class);

    when(secondRoom.getEvents()).thenReturn(secondEvents);
    when(thirdRoom.getEvents()).thenReturn(thirdEvents);

    assertDoesNotThrow(
        () -> {
          achievementsManager.newRoom(secondRoom);
          achievementsManager.newRoom(thirdRoom);
        });

    verify(secondEvents).addListener(eq("FinalBossDefeated"), any(EventListener0.class));

    verify(thirdEvents).addListener(eq("FinalBossDefeated"), any(EventListener0.class));
  }
}
