package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AchievementTest {

  private static final AchievementContext ANY_CONTEXT = new AchievementContext();

  @Test
  void getName_returnsNameGivenToConstructor() {
    Achievement achievement = new Achievement("Slayer", "Test achievement", ctx -> true);

    assertEquals("Slayer", achievement.getName());
  }

  @Test
  void newAchievement_startsLocked() {
    Achievement achievement = new Achievement("Slayer", "Test achievement", ctx -> true);

    assertFalse(achievement.isUnlocked());
  }

  @Test
  void update_conditionNotMet_returnsFalseAndStaysLocked() {
    Achievement achievement = new Achievement("Never", "Test achievement", ctx -> false);

    assertFalse(achievement.update(ANY_CONTEXT));
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void update_conditionMet_returnsTrueAndUnlocks() {
    Achievement achievement = new Achievement("Always", "Test achievement", ctx -> true);

    assertTrue(achievement.update(ANY_CONTEXT));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void update_passesContextToCondition() {
    AchievementContext context = new AchievementContext();
    context.goldTotal = 50;
    Achievement achievement =
        new Achievement(
            "Rich", "Test achievement", ctx -> ctx.goldTotal != null && ctx.goldTotal == 50);

    assertTrue(achievement.update(context));
  }

  @Test
  void update_afterUnlocking_returnsFalseSoItOnlyReportsUnlockOnce() {
    Achievement achievement = new Achievement("Always", "Test achievement", ctx -> true);

    assertTrue(achievement.update(ANY_CONTEXT));
    assertFalse(achievement.update(ANY_CONTEXT));
  }

  @Test
  void update_afterUnlocking_staysUnlockedEvenIfConditionLaterFails() {
    AchievementContext rich = new AchievementContext();
    rich.goldTotal = 100;
    AchievementContext poor = new AchievementContext();
    poor.goldTotal = 0;
    Achievement achievement =
        new Achievement(
            "Rich", "Test achievement", ctx -> ctx.goldTotal != null && ctx.goldTotal >= 100);

    achievement.update(rich);
    achievement.update(poor);

    assertTrue(achievement.isUnlocked());
  }

  @Test
  void update_afterUnlocking_doesNotEvaluateConditionAgain() {
    AtomicInteger evaluations = new AtomicInteger();
    Achievement achievement =
        new Achievement(
            "Counted",
            "Test achievement",
            ctx -> {
              evaluations.incrementAndGet();
              return true;
            });

    achievement.update(ANY_CONTEXT);
    achievement.update(ANY_CONTEXT);
    achievement.update(ANY_CONTEXT);

    assertEquals(1, evaluations.get());
  }

  @Test
  void update_conditionFailsThenPasses_unlocksOnLaterUpdate() {
    AchievementContext notYet = new AchievementContext();
    AchievementContext now = new AchievementContext();
    now.dungeonCompletedId = "forest";
    Achievement achievement =
        new Achievement(
            "Forest", "Test achievement", ctx -> "forest".equals(ctx.dungeonCompletedId));

    assertFalse(achievement.update(notYet));
    assertFalse(achievement.isUnlocked());
    assertTrue(achievement.update(now));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void getProgress_startsAtZero() {
    Achievement achievement = new Achievement("Counter", "Test achievement");
    assertEquals(0f, achievement.getProgress());
  }

  @Test
  void getTarget_returnsTarget() {
    Achievement achievement = new Achievement("Slayer", "Test achievement");

    assertEquals(0, achievement.getTarget());
  }

  @Test
  void addProgress_accumulatesAcrossCalls() {
    Achievement achievement = new Achievement("Counter", "Test achievement");
    achievement.setCondition(ctx -> false);
    achievement.addProgress(2f);
    achievement.addProgress(3f);
    assertEquals(5f, achievement.getProgress());
  }
}
