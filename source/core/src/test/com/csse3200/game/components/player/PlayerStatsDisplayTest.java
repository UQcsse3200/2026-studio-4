package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the stats the HUD is handed while Last Stand runs, plus the health/shield bars and stat
 * labels that back the on-screen display. The display listens for the same events, so what is
 * recorded/read back here is what it draws.
 */
@ExtendWith(GameExtension.class)
class PlayerStatsDisplayTest {
  private static final long START = 1_000L;

  private GameTime time;
  private CombatStatsComponent combat;
  private PlayerAbilitiesComponent abilities;
  private PlayerStatsDisplay display;
  private Entity player;

  private float shownMovementSpeed;
  private float shownAttackSpeed;
  private int shownStrength;

  @BeforeEach
  void setUp() {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(START);

    combat = new CombatStatsComponent(100, 10, 4f, 2f);
    abilities = new PlayerAbilitiesComponent(time);
    display = new PlayerStatsDisplay();
    player =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(new InventoryComponent(0))
            .addComponent(display);

    shownMovementSpeed = combat.getEffectiveMovementSpeed();
    shownAttackSpeed = combat.getEffectiveAttackSpeed();
    shownStrength = combat.getEffectiveBaseAttack();
    player.getEvents().addListener("updateMovementSpeed", (Float s) -> shownMovementSpeed = s);
    player.getEvents().addListener("updateAttackSpeed", (Float s) -> shownAttackSpeed = s);
    player.getEvents().addListener("updateBaseAttack", (Integer s) -> shownStrength = s);

    player.create();
  }

  // ---------- Original Last Stand coverage ----------

  @Test
  void shouldShowAmplifiedStatsWhileLastStandIsActiveAndRevertOnExpiry() {
    assertShownStats(4f, 2f, 10);

    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    assertShownStats(6f, 3f, 15);

    when(time.getTime()).thenReturn(START + LastStand.DURATION_MS);
    player.update();

    assertShownStats(4f, 2f, 10);
  }

  @Test
  void shouldKeepAmplifyingRawStatChangesMadeDuringLastStand() {
    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    combat.addMovementSpeed(2f);
    combat.addBaseAttack(2);

    assertShownStats(9f, 3f, 18);
    assertEquals(6f, combat.getMovementSpeed());
    assertEquals(12, combat.getBaseAttack());
  }

  private void assertShownStats(float movementSpeed, float attackSpeed, int strength) {
    assertEquals(movementSpeed, shownMovementSpeed);
    assertEquals(attackSpeed, shownAttackSpeed);
    assertEquals(strength, shownStrength);
  }

  // ---------- New: health bar, shield bar, and stat label coverage ----------

  @Test
  void shouldInitialiseHealthBarFromCombatStats() {
    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertNotNull(healthBar);
    assertEquals(100f, healthBar.getValue());
    assertEquals(100f, healthBar.getMaxValue());

    Label healthValueLabel = getField(display, "healthValueLabel", Label.class);
    assertEquals("Health: 100 / 100", healthValueLabel.getText().toString());
  }

  @Test
  void shouldUpdateHealthBarAndLabelOnHealthEvent() {
    player.getEvents().trigger("updateHealth", 40);

    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertEquals(40f, healthBar.getValue());

    Label healthValueLabel = getField(display, "healthValueLabel", Label.class);
    assertEquals("Health: 40 / 100", healthValueLabel.getText().toString());
  }

  @Test
  void shouldUpdateHealthBarRangeAndLabelOnMaxHealthEvent() {
    player.getEvents().trigger("updateHealth", 30);
    player.getEvents().trigger("updateMaxHealth", 150);

    ProgressBar healthBar = getField(display, "healthBar", ProgressBar.class);
    assertEquals(150f, healthBar.getMaxValue());

    Label healthValueLabel = getField(display, "healthValueLabel", Label.class);
    assertEquals("Health: 30 / 150", healthValueLabel.getText().toString());
  }

  @Test
  void shouldUpdateStrengthLabelOnBaseAttackEvent() {
    player.getEvents().trigger("updateBaseAttack", 18);

    Label strengthLabel = getField(display, "strengthLabel", Label.class);
    assertEquals("18", strengthLabel.getText().toString());
  }

  @Test
  void shouldUpdateMovementSpeedLabelOnEvent() {
    player.getEvents().trigger("updateMovementSpeed", 6f);

    Label movementSpeedLabel = getField(display, "movementSpeedLabel", Label.class);
    assertEquals("6.0x", movementSpeedLabel.getText().toString());
  }

  @Test
  void shouldUpdateAttackSpeedLabelOnEvent() {
    player.getEvents().trigger("updateAttackSpeed", 3.5f);

    Label attackSpeedLabel = getField(display, "attackSpeedLabel", Label.class);
    assertEquals("3.5", attackSpeedLabel.getText().toString());
  }

  @Test
  void shouldRemoveBothTablesOnDispose() {
    assertDoesNotThrow(display::dispose);
  }

  @Test
  void shouldNotThrowWhenStageIsUnavailable() {
    RenderService headlessRenderService = mock(RenderService.class);
    when(headlessRenderService.getStage()).thenReturn(null);
    ServiceLocator.registerRenderService(headlessRenderService);

    Entity headlessPlayer =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10, 4f, 2f))
            .addComponent(new PlayerStatsDisplay());

    assertDoesNotThrow(headlessPlayer::create);
  }

  @Test
  void shouldHideDebugStatLabelsFromCombatCorner() {
    assertFalse(getField(display, "movementSpeedLabel", Label.class).isVisible());
    assertFalse(getField(display, "attackSpeedLabel", Label.class).isVisible());
    assertFalse(getField(display, "strengthLabel", Label.class).isVisible());
  }

  @Test
  void shouldWarnWhenHealthIsAtMostAQuarterOfMax() {
    assertFalse(display.isLowHealthWarning());
    player.getEvents().trigger("updateHealth", 26);
    assertFalse(display.isLowHealthWarning());
    player.getEvents().trigger("updateHealth", 25);
    assertTrue(display.isLowHealthWarning());
    player.getEvents().trigger("updateHealth", 20);
    assertTrue(display.isLowHealthWarning());
    Label healthValueLabel = getField(display, "healthValueLabel", Label.class);
    assertEquals(1f, healthValueLabel.getColor().r, 0.01f);
    assertEquals(0f, healthValueLabel.getColor().g, 0.01f);
  }

  @Test
  void shouldClearWarningWhenHealthRecoversOrMaxHealthChanges() {
    player.getEvents().trigger("updateHealth", 20);
    assertTrue(display.isLowHealthWarning());
    player.getEvents().trigger("updateHealth", 30);
    assertFalse(display.isLowHealthWarning());
    player.getEvents().trigger("updateHealth", 20);
    player.getEvents().trigger("updateMaxHealth", 60);
    assertFalse(display.isLowHealthWarning());
  }

  /** Reads a private field via reflection since the display exposes no public getters. */
  private <T> T getField(PlayerStatsDisplay display, String fieldName, Class<T> type) {
    try {
      Field field = PlayerStatsDisplay.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return type.cast(field.get(display));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not read field '" + fieldName + "' via reflection", e);
    }
  }
}
