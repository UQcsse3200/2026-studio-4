package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the stats the HUD is handed while Last Stand runs. The display listens for the same three
 * events, so what is recorded here is what it draws.
 */
@ExtendWith(GameExtension.class)
class PlayerStatsDisplayTest {
  private static final long START = 1_000L;

  private GameTime time;
  private CombatStatsComponent combat;
  private PlayerAbilitiesComponent abilities;
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
    player =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(new InventoryComponent(0))
            .addComponent(new PlayerStatsDisplay());

    shownMovementSpeed = combat.getEffectiveMovementSpeed();
    shownAttackSpeed = combat.getEffectiveAttackSpeed();
    shownStrength = combat.getEffectiveBaseAttack();
    player.getEvents().addListener("updateMovementSpeed", (Float s) -> shownMovementSpeed = s);
    player.getEvents().addListener("updateAttackSpeed", (Float s) -> shownAttackSpeed = s);
    player.getEvents().addListener("updateBaseAttack", (Integer s) -> shownStrength = s);

    player.create();
  }

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
}
