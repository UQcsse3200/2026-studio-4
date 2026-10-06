package com.csse3200.game.components.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NarrativeInputComponentTest {
  private int advances;
  private NarrativeManagerComponent manager;
  private NarrativeInputComponent input;
  private boolean active;
  private int skips;

  @BeforeEach
  void setUp() {
    advances = 0;
    skips = 0;
    active = false;
    ServiceLocator.registerEntityService(new EntityService());
    // A manager whose state the test controls directly
    manager =
        new NarrativeManagerComponent(new Entity(), null, null) {
          @Override
          public boolean isNarrativeActive() {
            return active;
          }

          @Override
          public boolean advance() {
            advances++;
            return true;
          }

          @Override
          public boolean skipCutscene() {
            skips++;
            return true;
          }
        };
    input = new NarrativeInputComponent(manager);
  }

  @Test
  void ignoresEverythingWhileNothingIsPlaying() {
    assertFalse(input.keyDown(Input.Keys.SPACE));
    assertFalse(input.keyDown(Input.Keys.W));
    assertFalse(input.touchDown(0, 0, 0, Input.Buttons.LEFT));
    assertEquals(0, advances);
  }

  @Test
  void spaceEnterAndLeftClickAdvance() {
    active = true;
    assertTrue(input.keyDown(Input.Keys.SPACE));
    assertTrue(input.keyDown(Input.Keys.ENTER));
    assertTrue(input.touchDown(0, 0, 0, Input.Buttons.LEFT));
    assertEquals(3, advances);
  }

  @Test
  void rightClickIsSwallowedWithoutAdvancing() {
    active = true;
    assertTrue(input.touchDown(0, 0, 0, Input.Buttons.RIGHT));
    assertEquals(0, advances);
  }

  @Test
  void escapeSkipsTheCutscene() {
    active = true;
    assertTrue(input.keyDown(Input.Keys.ESCAPE));
    assertEquals(1, skips);
  }

  @Test
  void gameplayKeysAreSwallowedAndF1StillOpensTheTerminal() {
    active = true;
    assertTrue(input.keyDown(Input.Keys.W));
    assertTrue(input.keyDown(Input.Keys.J));
    assertFalse(input.keyDown(Input.Keys.F1));
    assertEquals(0, advances);
  }

  @Test
  void keyUpIsOnlySwallowedForKeysPressedDuringTheNarrative() {
    // D was already held when the dialogue opened, so releasing it must reach the player
    assertFalse(input.keyDown(Input.Keys.D));
    active = true;
    assertTrue(input.keyDown(Input.Keys.W));

    assertFalse(input.keyUp(Input.Keys.D));
    assertTrue(input.keyUp(Input.Keys.W));
    assertFalse(input.keyUp(Input.Keys.W));
  }
}
