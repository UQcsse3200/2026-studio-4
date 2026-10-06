package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * The config is mocked (it is a plain data holder owned elsewhere) and constructors of the
 * achievement subclasses are intercepted with {@code mockConstruction}, so these tests verify only
 * the factory's own job: picking the right class and passing the right arguments.
 */
class AchievementsFactoryTest {

  private AchievementConfig config(String type, String name) {
    AchievementConfig c = mock(AchievementConfig.class);
    c.type = type;
    c.name = name;
    return c;
  }

  @Test
  void buildsDungeonClearAchievement() {
    AchievementConfig c = config("dungeonClear", "Forest Cleared");
    c.dungeonId = "forest";
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<DungeonClearAchievement> mc =
        mockConstruction(DungeonClearAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertEquals(1, mc.constructed().size());
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of("forest", "Forest Cleared"), args.get(0));
    }
  }

  @Test
  void buildsDungeonReachedAchievement() {
    AchievementConfig c = config("dungeonReached", "Cave Explorer");
    c.dungeonId = "cave";
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<DungeonReachedAchievement> mc =
        mockConstruction(DungeonReachedAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of("cave", "Cave Explorer"), args.get(0));
    }
  }

  @Test
  void buildsTypeKillAchievement() {
    AchievementConfig c = config("enemyKillCount", "Zombie Hunter");
    c.enemyType = EnemyType.ZOMBIE;
    c.target = 5;
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<TypeKillAchievement> mc =
        mockConstruction(TypeKillAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of(EnemyType.ZOMBIE, 5, "Zombie Hunter"), args.get(0));
    }
  }

  @Test
  void buildsEnemySetAchievementWithEnumSetOfConfiguredTypes() {
    AchievementConfig c = config("enemySet", "Collector");
    c.enemyTypes = List.of(EnemyType.ZOMBIE, EnemyType.KNIGHT);
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<EnemySetAchievement> mc =
        mockConstruction(EnemySetAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(EnumSet.of(EnemyType.ZOMBIE, EnemyType.KNIGHT), args.get(0).get(0));
      assertEquals("Collector", args.get(0).get(1));
    }
  }

  @Test
  void buildsKillStreakAchievement() {
    AchievementConfig c = config("killStreak", "Untouchable");
    c.target = 10;
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<KillStreakAchievement> mc =
        mockConstruction(KillStreakAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of(10, "Untouchable"), args.get(0));
    }
  }

  @Test
  void buildsSpeedRunAchievement() {
    AchievementConfig c = config("speedRun", "Speedy");
    c.dungeonId = "forest";
    c.maxSeconds = 90.5f;
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<SpeedRunAchievement> mc =
        mockConstruction(SpeedRunAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of("forest", 90.5f, "Speedy"), args.get(0));
    }
  }

  @Test
  void buildsGoldAchievement() {
    AchievementConfig c = config("gold", "Rich");
    c.target = 250;
    List<List<?>> args = new ArrayList<>();
    try (MockedConstruction<GoldAchievement> mc =
        mockConstruction(GoldAchievement.class, (m, ctx) -> args.add(ctx.arguments()))) {
      Achievement result = AchievementsFactory.build(c);
      assertSame(mc.constructed().get(0), result);
      assertEquals(List.of(250, "Rich"), args.get(0));
    }
  }

  @Test
  void unknownTypeThrowsWithTypeInMessage() {
    AchievementConfig c = config("bogus", "Nope");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> AchievementsFactory.build(c));
    assertEquals("Unknown achievement type: bogus", ex.getMessage());
  }

  @Test
  void typeMatchingIsCaseSensitive() {
    AchievementConfig c = config("Gold", "Rich");
    assertThrows(IllegalArgumentException.class, () -> AchievementsFactory.build(c));
  }

  @Test
  void builtAchievementsAreFunctionalWithoutConstructionMocking() {
    AchievementConfig c = config("gold", "Rich");
    c.target = 50;
    Achievement a = AchievementsFactory.build(c);
    assertInstanceOf(GoldAchievement.class, a);
    assertEquals("Rich", a.getName());
    assertTrue(a.onGoldChanged(50));
  }

  @Test
  void cannotBeInstantiated() throws Exception {
    Constructor<AchievementsFactory> ctor = AchievementsFactory.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    ctor.setAccessible(true);
    InvocationTargetException ex = assertThrows(InvocationTargetException.class, ctor::newInstance);
    assertInstanceOf(IllegalStateException.class, ex.getCause());
    assertEquals("Instantiating static util class", ex.getCause().getMessage());
  }
}
