package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AchievementServiceTest {
    private AchievementService service;
    private List<String> unlockEvents;

    /** An achievement that unlocks when the player is damaged. */
    private static Achievement onDamage(String name) {
        return new Achievement(name, "desc of " + name, ctx -> ctx.playerDamaged);
    }

    @BeforeEach
    void setUp() {
        service = new AchievementService();
        unlockEvents = new ArrayList<>();
        service.getEvents().addListener("achievementUnlocked", (String n) -> unlockEvents.add(n));
    }

    @Test
    void updateDispatchesUnlockEventOnce() {
        service.register(onDamage("Ouch"));
        AchievementContext hit = new AchievementContext();
        hit.playerDamaged = true;

        service.update(hit);
        service.update(hit);

        assertEquals(List.of("Ouch"), unlockEvents);
    }

    @Test
    void unrelatedContextDoesNotUnlock() {
        service.register(onDamage("Ouch"));
        service.update(new AchievementContext());
        assertTrue(unlockEvents.isEmpty());
        assertTrue(service.getUnlockedAchievementNames().isEmpty());
    }

    @Test
    void queriesReflectRegisteredAchievements() {
        Achievement a = onDamage("Ouch");
        a.setProgress(2f);
        service.register(a);

        assertEquals(List.of(a), service.getAchievements());
        assertEquals("desc of Ouch", service.getAchievementDescriptions().get("Ouch"));
        assertEquals(2f, service.getAchievementProgress().get("Ouch"));
    }

    @Test
    void restoreStateRoundTrip() {
        service.register(onDamage("A"));
        service.register(onDamage("B"));
        AchievementContext hit = new AchievementContext();
        hit.playerDamaged = true;
        service.update(hit); // unlocks both

        List<String> names = service.getUnlockedAchievementNames();
        Map<String, Float> progress = service.getAchievementProgress();

        AchievementService fresh = new AchievementService();
        fresh.register(onDamage("A"));
        fresh.register(onDamage("B"));
        fresh.restoreState(names, progress);

        assertEquals(List.of("A", "B"), fresh.getUnlockedAchievementNames());
    }

    @Test
    void restoreStateOnlyUnlocksNamedAchievements() {
        service.register(onDamage("A"));
        service.register(onDamage("B"));
        service.restoreState(List.of("A"), Map.of("A", 1f));

        assertEquals(List.of("A"), service.getUnlockedAchievementNames());
        assertEquals(0f, service.getAchievementProgress().get("B")); // missing progress -> 0
    }

    @Test
    void restoreStateAcceptsNulls() {
        service.register(onDamage("A"));
        assertDoesNotThrow(() -> service.restoreState(null, null));
        assertTrue(service.getUnlockedAchievementNames().isEmpty());
    }
}