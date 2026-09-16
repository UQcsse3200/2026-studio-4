package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks that the enemy health bar's internal ProgressBar and Table reflect the entity's combat
 * stats and visibility events. Since EnemyStatDisplay doesn't re-emit events (it only consumes them
 * into private widgets), state is read back via reflection rather than event capture.
 */
@ExtendWith(GameExtension.class)
class EnemyStatDisplayTest {

  private CombatStatsComponent combat;
  private Entity enemy;
  private EnemyStatDisplay display;

  @BeforeEach
  void setUp() {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    combat = new CombatStatsComponent(50, 5);
    display = new EnemyStatDisplay();
    enemy = new Entity().addComponent(combat).addComponent(display);

    enemy.create();
  }

  @Test
  void shouldInitialiseHealthBarFromCombatStats() {
    ProgressBar healthBar = getHealthBar(display);
    assertNotNull(healthBar);
    assertEquals(50f, healthBar.getValue());
    assertEquals(50f, healthBar.getMaxValue());
  }

  @Test
  void shouldUpdateHealthBarValueOnHealthEvent() {
    enemy.getEvents().trigger("updateHealth", 20);

    ProgressBar healthBar = getHealthBar(display);
    assertEquals(20f, healthBar.getValue());
  }

  @Test
  void shouldUpdateHealthBarRangeOnMaxHealthEvent() {
    enemy.getEvents().trigger("updateMaxHealth", 80);

    ProgressBar healthBar = getHealthBar(display);
    assertEquals(80f, healthBar.getMaxValue());
  }

  @Test
  void shouldToggleTableVisibilityOnVisibilityEvent() {
    Table table = getTable(display);
    assertTrue(table.isVisible());

    enemy.getEvents().trigger("enemyHealthBarVisible", false);
    assertFalse(getTable(display).isVisible());

    enemy.getEvents().trigger("enemyHealthBarVisible", true);
    assertTrue(getTable(display).isVisible());
  }

  @Test
  void shouldApplyCustomScaleFromConstructor() {
    EnemyStatDisplay scaledDisplay = new EnemyStatDisplay(2.5f);
    Entity scaledEnemy =
        new Entity().addComponent(new CombatStatsComponent(50, 5)).addComponent(scaledDisplay);
    scaledEnemy.create();

    Table table = getTable(scaledDisplay);
    assertEquals(2.5f, table.getScaleX());
    assertEquals(2.5f, table.getScaleY());
  }

  @Test
  void shouldNotThrowWhenUpdatedWithoutRegisteredCamera() {
    // No CameraComponent/world camera registered in this test's ServiceLocator,
    // so updateHealthBar's early camera-null check should make update() a no-op.
    assertDoesNotThrow(enemy::update);
  }

  @Test
  void shouldNotThrowWhenStageIsUnavailable() {
    RenderService headlessRenderService = mock(RenderService.class);
    when(headlessRenderService.getStage()).thenReturn(null);
    ServiceLocator.registerRenderService(headlessRenderService);

    Entity headlessEnemy =
        new Entity()
            .addComponent(new CombatStatsComponent(30, 3))
            .addComponent(new EnemyStatDisplay());

    assertDoesNotThrow(headlessEnemy::create);
  }

  /** Reads the private ProgressBar field via reflection since it has no public accessor. */
  private ProgressBar getHealthBar(EnemyStatDisplay display) {
    try {
      Field field = EnemyStatDisplay.class.getDeclaredField("healthBar");
      field.setAccessible(true);
      return (ProgressBar) field.get(display);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not read healthBar field via reflection", e);
    }
  }

  /** Reads the private Table field via reflection since it has no public accessor. */
  private Table getTable(EnemyStatDisplay display) {
    try {
      Field field = EnemyStatDisplay.class.getDeclaredField("table");
      field.setAccessible(true);
      return (Table) field.get(display);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not read table field via reflection", e);
    }
  }
}
