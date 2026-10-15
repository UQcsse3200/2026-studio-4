package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AchievementTest {

  private static final AchievementContext ANY_CONTEXT = new AchievementContext();
  private static final String DESC = "description";

  @Test
  void getName_returnsNameGivenToConstructor() {
    Achievement achievement = new Achievement("Slayer", DESC, ctx -> true);

    assertEquals("Slayer", achievement.getName());
  }

  @Test
  void getDescription_returnsDescriptionGivenToConstructor() {
    Achievement achievement = new Achievement("Slayer", "Defeat 10 enemies.", ctx -> true);

    assertEquals("Defeat 10 enemies.", achievement.getDescription());
  }

  @Test
  void getDescription_nullDescription_isNull() {
    Achievement withCondition = new Achievement("Slayer", null, ctx -> true);
    Achievement withoutCondition = new Achievement("Slayer", null);

    assertNull(withCondition.getDescription());
    assertNull(withoutCondition.getDescription());
  }

  @Test
  void bothConstructors_keepNameAndDescriptionAndStartLocked() {
    Achievement withCondition = new Achievement("A", "descA", ctx -> true);
    Achievement withoutCondition = new Achievement("B", "descB");

    assertEquals("A", withCondition.getName());
    assertEquals("descA", withCondition.getDescription());
    assertEquals("B", withoutCondition.getName());
    assertEquals("descB", withoutCondition.getDescription());
    assertFalse(withCondition.isUnlocked());
    assertFalse(withoutCondition.isUnlocked());
  }

  @Test
  void newAchievement_startsLocked() {
    Achievement achievement = new Achievement("Slayer", DESC, ctx -> true);

    assertFalse(achievement.isUnlocked());
  }

  @Test
  void update_conditionNotMet_returnsFalseAndStaysLocked() {
    Achievement achievement = new Achievement("Never", DESC, ctx -> false);

    assertFalse(achievement.update(ANY_CONTEXT));
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void update_conditionMet_returnsTrueAndUnlocks() {
    Achievement achievement = new Achievement("Always", DESC, ctx -> true);

    assertTrue(achievement.update(ANY_CONTEXT));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void update_passesContextToCondition() {
    AchievementContext context = new AchievementContext();
    context.goldCollected = 50;
    Achievement achievement =
        new Achievement("Rich", DESC, ctx -> ctx.goldCollected != null && ctx.goldCollected == 50);

    assertTrue(achievement.update(context));
  }

  @Test
  void update_afterUnlocking_returnsFalseSoItOnlyReportsUnlockOnce() {
    Achievement achievement = new Achievement("Always", DESC, ctx -> true);

    assertTrue(achievement.update(ANY_CONTEXT));
    assertFalse(achievement.update(ANY_CONTEXT));
  }

  @Test
  void update_afterUnlocking_staysUnlockedEvenIfConditionLaterFails() {
    AchievementContext rich = new AchievementContext();
    rich.goldCollected = 100;
    AchievementContext poor = new AchievementContext();
    poor.goldCollected = 0;
    Achievement achievement =
        new Achievement("Rich", DESC, ctx -> ctx.goldCollected != null && ctx.goldCollected >= 100);

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
            DESC,
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
        new Achievement("Forest", DESC, ctx -> "forest".equals(ctx.dungeonCompletedId));

    assertFalse(achievement.update(notYet));
    assertFalse(achievement.isUnlocked());
    assertTrue(achievement.update(now));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void setCondition_conditionAttachedAfterConstruction_isUsedByUpdate() {
    Achievement achievement = new Achievement("Late", DESC);
    achievement.setCondition(ctx -> true);

    assertTrue(achievement.update(ANY_CONTEXT));
  }

  @Test
  void getProgress_startsAtZero() {
    Achievement achievement = new Achievement("Counter", DESC);

    assertEquals(0f, achievement.getProgress());
  }

  @Test
  void getTarget_startsAtZero() {
    Achievement achievement = new Achievement("Slayer", DESC);

    assertEquals(0f, achievement.getTarget());
  }

  @Test
  void setTarget_isReturnedByGetTarget() {
    Achievement achievement = new Achievement("Slayer", DESC);
    achievement.setTarget(25f);

    assertEquals(25f, achievement.getTarget());
  }

  @Test
  void setProgress_replacesTheCurrentValue() {
    Achievement achievement = new Achievement("Counter", DESC);
    achievement.addProgress(4f);
    achievement.setProgress(1f);

    assertEquals(1f, achievement.getProgress());
  }

  @Test
  void addProgress_accumulatesAcrossCalls() {
    Achievement achievement = new Achievement("Counter", DESC);
    achievement.setCondition(ctx -> false);
    achievement.addProgress(2f);
    achievement.addProgress(3f);

    assertEquals(5f, achievement.getProgress());
  }
}
