package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EarthquakeAttackTaskTest {

  private Entity player;
  private Entity boss;

  private CombatStatsComponent playerStats;
  private CombatStatsComponent bossStats;

  private GameTime gameTime;
  private EarthquakeAttackTask task;

  private EventHandler eventHandler;

  @BeforeEach
  void setUp() {
    player = mock(Entity.class);
    boss = mock(Entity.class);
    eventHandler = mock(EventHandler.class);

    playerStats = mock(CombatStatsComponent.class);
    bossStats = mock(CombatStatsComponent.class);

    gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(1f);
    ServiceLocator.registerTimeSource(gameTime);

    when(player.getComponent(CombatStatsComponent.class)).thenReturn(playerStats);

    when(boss.getComponent(CombatStatsComponent.class)).thenReturn(bossStats);

    when(player.getPosition()).thenReturn(new Vector2(2f, 0f));

    when(boss.getPosition()).thenReturn(new Vector2(0f, 0f));

    when(bossStats.getBaseAttack()).thenReturn(10);

    when(boss.getEvents()).thenReturn(eventHandler);

    TaskRunner owner = mock(TaskRunner.class);

    task = new EarthquakeAttackTask(player, 3f, boss);
    task.create(owner);
    task.start();
  }

  @Test
  void shouldHaveNegativePriorityBeforePhaseTwo() {
    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldBecomeActiveWhenPhaseTwoStarts() {
    task.activate();

    assertEquals(20, task.getPriority());
  }

  @Test
  void shouldDamagePlayerWhenWithinRange() {
    task.activate();

    TaskRunner owner = mock(TaskRunner.class);
    when(owner.getEntity()).thenReturn(boss);

    task.create(owner);
    task.start();
    task.update();

    verify(playerStats).takeDamage(bossStats.getBaseAttack(), boss);
  }

  @Test
  void shouldNotDamagePlayerWhenOutsideRange() {
    when(player.getPosition()).thenReturn(new Vector2(10f, 0f));

    task.activate();

    TaskRunner owner = mock(TaskRunner.class);
    when(owner.getEntity()).thenReturn(boss);

    task.create(owner);
    task.start();
    task.update();

    verify(playerStats, never()).takeDamage(anyInt(), eq(boss));
  }

  @Test
  void shouldOnlyAttackOnceDuringCooldown() {
    task.activate();

    TaskRunner owner = mock(TaskRunner.class);
    when(owner.getEntity()).thenReturn(boss);

    task.create(owner);
    task.start();

    task.update();
    task.update();

    verify(playerStats, times(2)).takeDamage(10, boss);
  }

  @Test
  void shouldAttackAgainAfterCooldown() {
    task.activate();

    TaskRunner owner = mock(TaskRunner.class);
    when(owner.getEntity()).thenReturn(boss);

    task.create(owner);
    task.start();

    // First attack.
    task.update();

    // Three seconds pass.
    when(gameTime.getDeltaTime()).thenReturn(1f);

    task.update();

    // Second attack should now be possible.
    verify(playerStats, times(2)).takeDamage(10, boss);
  }

  @Test
  void shouldNotAttackBeforePhaseTwo() {
    TaskRunner owner = mock(TaskRunner.class);
    when(owner.getEntity()).thenReturn(boss);

    task.create(owner);
    task.start();
    task.update();

    verify(playerStats, never()).takeDamage(anyInt(), eq(boss));
  }
}
