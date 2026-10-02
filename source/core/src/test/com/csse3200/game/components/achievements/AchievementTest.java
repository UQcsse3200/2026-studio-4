package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
class AchievementTest {

  private Entity room;
  private EventHandler events;
  private Achievement achievement;

  @BeforeEach
  void setUp() {
    room = mock(Entity.class);
    events = mock(EventHandler.class);

    when(room.getEvents()).thenReturn(events);

    achievement = new Achievement(room, "FinalBossDefeated", 3, "Grandpa Fighter");
  }

  @Test
  void shouldSetAchievementProperties() {
    assertEquals("Grandpa Fighter", achievement.getName());
    assertEquals(3, achievement.getMaxProgression());
    assertEquals(0, achievement.getCurrentProgression());
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void shouldRegisterProgressionListenerWhenCreated() {
    verify(events).addListener(eq("FinalBossDefeated"), any(EventListener0.class));
  }

  @Test
  void shouldProgressAchievementWhenEventOccurs() {
    EventListener0 listener = captureProgressListener();

    listener.handle();

    assertEquals(1, achievement.getCurrentProgression());
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void shouldUnlockAchievementAtMaximumProgression() {
    EventListener0 listener = captureProgressListener();

    listener.handle();
    listener.handle();
    listener.handle();

    assertEquals(3, achievement.getCurrentProgression());
    assertTrue(achievement.isUnlocked());

    verify(events).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldNotProgressAfterAchievementIsUnlocked() {
    EventListener0 listener = captureProgressListener();

    listener.handle();
    listener.handle();
    listener.handle();

    assertTrue(achievement.isUnlocked());
    assertEquals(3, achievement.getCurrentProgression());

    listener.handle();
    listener.handle();

    assertEquals(3, achievement.getCurrentProgression());
    verify(events, times(1)).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldNotUnlockBeforeMaximumProgression() {
    EventListener0 listener = captureProgressListener();

    listener.handle();
    listener.handle();

    assertEquals(2, achievement.getCurrentProgression());
    assertFalse(achievement.isUnlocked());

    verify(events, never()).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldMoveAchievementToNewRoom() {
    Entity newRoom = mock(Entity.class);
    EventHandler newEvents = mock(EventHandler.class);

    when(newRoom.getEvents()).thenReturn(newEvents);

    achievement.newRoom(newRoom);

    verify(newEvents).addListener(eq("FinalBossDefeated"), any(EventListener0.class));
  }

  @Test
  void shouldProgressUsingListenerFromNewRoom() {
    Entity newRoom = mock(Entity.class);
    EventHandler newEvents = mock(EventHandler.class);

    when(newRoom.getEvents()).thenReturn(newEvents);

    achievement.newRoom(newRoom);

    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(newEvents).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    listenerCaptor.getValue().handle();

    assertEquals(1, achievement.getCurrentProgression());
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void shouldUnlockAchievementInNewRoom() {
    Entity newRoom = mock(Entity.class);
    EventHandler newEvents = mock(EventHandler.class);

    when(newRoom.getEvents()).thenReturn(newEvents);

    achievement.newRoom(newRoom);

    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(newEvents).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    EventListener0 listener = listenerCaptor.getValue();

    listener.handle();
    listener.handle();
    listener.handle();

    assertEquals(3, achievement.getCurrentProgression());
    assertTrue(achievement.isUnlocked());

    verify(newEvents).trigger("achievementUnlocked", "Grandpa Fighter");
    verify(events, never()).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldUnlockImmediatelyWhenMaximumProgressionIsOne() {
    // Ignore the listener registered by the achievement created in setUp().
    org.mockito.Mockito.clearInvocations(events);

    Achievement singleProgressAchievement =
        new Achievement(room, "FinalBossDefeated", 1, "Grandpa Fighter");

    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(events).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    listenerCaptor.getValue().handle();

    assertEquals(1, singleProgressAchievement.getCurrentProgression());
    assertTrue(singleProgressAchievement.isUnlocked());

    verify(events).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  @Test
  void shouldOnlyUnlockOnceWhenEventOccursManyTimes() {
    EventListener0 listener = captureProgressListener();

    for (int i = 0; i < 10; i++) {
      listener.handle();
    }

    assertEquals(3, achievement.getCurrentProgression());
    assertTrue(achievement.isUnlocked());

    verify(events, times(1)).trigger("achievementUnlocked", "Grandpa Fighter");
  }

  private EventListener0 captureProgressListener() {
    ArgumentCaptor<EventListener0> listenerCaptor = ArgumentCaptor.forClass(EventListener0.class);

    verify(events).addListener(eq("FinalBossDefeated"), listenerCaptor.capture());

    return listenerCaptor.getValue();
  }
}
