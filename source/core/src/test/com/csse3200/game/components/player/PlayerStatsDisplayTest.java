package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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

@ExtendWith(GameExtension.class)
class PlayerStatsDisplayTest {
  private static final long START = 1_000L;

  private GameTime time;
  private CombatStatsComponent combat;
  private PlayerAbilitiesComponent abilities;
  private PlayerStatsDisplay display;
  private Entity player;

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
    player.create();
  }

  @Test
  void shouldShowAmplifiedStatsWhileLastStandIsActiveAndRevertOnExpiry() {
    assertStatLines(4f, 2f, 10);

    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    assertStatLines(6f, 3f, 15);

    when(time.getTime()).thenReturn(START + LastStand.DURATION_MS);
    player.update();

    assertStatLines(4f, 2f, 10);
  }

  @Test
  void shouldKeepAmplifyingRawStatChangesMadeDuringLastStand() {
    abilities.unlock(LastStand.class);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    combat.addMovementSpeed(2f);
    combat.addBaseAttack(2);

    assertStatLines(9f, 3f, 18);
    assertEquals(6f, combat.getMovementSpeed());
    assertEquals(12, combat.getBaseAttack());
  }

  private void assertStatLines(float movementSpeed, float attackSpeed, int strength) {
    assertEquals(String.format("Movement Speed: %.2f", movementSpeed), statLine("Movement Speed:"));
    assertEquals(String.format("Attack Speed: %.2f", attackSpeed), statLine("Attack Speed:"));
    assertEquals(String.format("Strength: %d", strength), statLine("Strength:"));
  }

  /** Returns the text of the first stat label starting with the given prefix. */
  private String statLine(String prefix) {
    Table table = display.table;
    for (Actor actor : table.getChildren()) {
      if (actor instanceof Label label && label.getText().toString().startsWith(prefix)) {
        return label.getText().toString();
      }
    }
    return null;
  }
}
