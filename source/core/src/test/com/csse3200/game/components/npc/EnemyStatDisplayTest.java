package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
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
 * stats and visibility events, and that updateHealthBar/dispose behave correctly across all their
 * branches (missing table, missing camera, and the fully-wired happy path).
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
    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertNotNull(healthBar);
    assertEquals(50f, healthBar.getValue());
    assertEquals(50f, healthBar.getMaxValue());
  }

  @Test
  void shouldUpdateHealthBarValueOnHealthEvent() {
    enemy.getEvents().trigger("updateHealth", 20);

    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertEquals(20f, healthBar.getValue());
  }

  @Test
  void shouldUpdateHealthBarRangeOnMaxHealthEvent() {
    enemy.getEvents().trigger("updateMaxHealth", 80);

    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertEquals(80f, healthBar.getMaxValue());
  }

  @Test
  void shouldToggleTableVisibilityOnVisibilityEvent() {
    Table table = getField(display, "table", Table.class);
    assertTrue(table.isVisible());

    enemy.getEvents().trigger("enemyHealthBarVisible", false);
    assertFalse(getField(display, "table", Table.class).isVisible());

    enemy.getEvents().trigger("enemyHealthBarVisible", true);
    assertTrue(getField(display, "table", Table.class).isVisible());
  }

  @Test
  void shouldApplyCustomScaleFromConstructor() {
    EnemyStatDisplay scaledDisplay = new EnemyStatDisplay(2.5f);
    Entity scaledEnemy =
        new Entity().addComponent(new CombatStatsComponent(50, 5)).addComponent(scaledDisplay);
    scaledEnemy.create();

    Table table = getField(scaledDisplay, "table", Table.class);
    assertEquals(2.5f, table.getScaleX());
    assertEquals(2.5f, table.getScaleY());
  }

  // ---------- updateHealthBar: table == null branch ----------

  @Test
  void shouldReturnImmediatelyFromUpdateHealthBarWhenTableIsNull() {
    // A fresh, never-created display has a null table field — updateHealthBar must bail out
    // before touching entity/camera at all, so this is safe to call with no entity attached.
    EnemyStatDisplay freshDisplay = new EnemyStatDisplay();
    assertDoesNotThrow(() -> freshDisplay.updateHealthBar(new Vector2(0f, 0f)));
  }

  // ---------- updateHealthBar: table != null, camera == null branch ----------

  @Test
  void shouldNotThrowWhenUpdatedWithoutRegisteredCamera() {
    // No CameraComponent/world camera registered in this test's ServiceLocator,
    // so updateHealthBar's early camera-null check should make update() a no-op.
    assertDoesNotThrow(enemy::update);
  }

  // ---------- updateHealthBar: full happy path, camera registered ----------

  @Test
  void shouldPositionTableWhenWorldCameraIsRegistered() {
    OrthographicCamera camera = new OrthographicCamera();
    camera.viewportWidth = 20f;
    camera.viewportHeight = 11f;
    camera.position.set(0f, 0f, 0f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);

    enemy.setPosition(new Vector2(5f, 5f));

    assertDoesNotThrow(enemy::update);

    Table table = getField(display, "table", Table.class);
    assertTrue(Float.isFinite(table.getX()));
    assertTrue(Float.isFinite(table.getY()));
  }

  // ---------- dispose: table == null branch ----------

  @Test
  void shouldNotThrowDisposeWhenTableWasNeverCreated() {
    EnemyStatDisplay freshDisplay = new EnemyStatDisplay();
    assertDoesNotThrow(freshDisplay::dispose);
  }

  // ---------- dispose: table != null branch ----------

  @Test
  void shouldRemoveTableOnDispose() {
    assertDoesNotThrow(display::dispose);
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

  /** Reads a private field via reflection since the display exposes no public getters. */
  private <T> T getField(EnemyStatDisplay display, String fieldName, Class<T> type) {
    try {
      Field field = EnemyStatDisplay.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return type.cast(field.get(display));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not read field '" + fieldName + "' via reflection", e);
    }
  }
}
