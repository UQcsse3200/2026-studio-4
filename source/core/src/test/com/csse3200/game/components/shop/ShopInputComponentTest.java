package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputFactory;
import com.csse3200.game.input.InputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopInputComponentTest {
  private ShopSessionComponent session;
  private ShopInputComponent input;
  private int downs;
  private int ups;

  @BeforeEach
  void setUp() {
    session = new ShopSessionComponent(new Entity(), new ShopSessionComponentTest.FakeView());
    input =
        new ShopInputComponent(
            session,
            new InputAdapter() {
              @Override
              public boolean touchDown(int x, int y, int pointer, int button) {
                downs++;
                return true;
              }

              @Override
              public boolean touchUp(int x, int y, int pointer, int button) {
                ups++;
                return true;
              }
            });
  }

  @Test
  void shouldConsumeEscapeWhenClosing() {
    session.open();
    assertTrue(input.keyDown(Input.Keys.ESCAPE));
    assertFalse(session.isOpen());
    assertTrue(input.keyUp(Input.Keys.ESCAPE));
  }

  @Test
  void shouldPassReleaseForPreheldKey() {
    assertFalse(input.keyDown(Input.Keys.W));
    session.open();
    assertFalse(input.keyUp(Input.Keys.W));
  }

  @Test
  void shouldSwallowReleaseAfterClosing() {
    session.open();
    assertTrue(input.keyDown(Input.Keys.W));
    session.close();
    assertTrue(input.keyUp(Input.Keys.W));
    assertFalse(input.keyUp(Input.Keys.W));
  }

  @Test
  void shouldForwardShopClicksOnce() {
    session.open();
    assertTrue(input.touchDown(10, 10, 0, Input.Buttons.LEFT));
    assertFalse(input.touchUp(10, 10, 1, Input.Buttons.LEFT));
    assertTrue(input.touchUp(10, 10, 0, Input.Buttons.LEFT));
    assertEquals(1, downs);
    assertEquals(1, ups);
  }

  @Test
  void shouldConsumeCloseClick() {
    input =
        new ShopInputComponent(
            session,
            new InputAdapter() {
              @Override
              public boolean touchDown(int x, int y, int pointer, int button) {
                session.close();
                return false;
              }
            });
    session.open();
    assertTrue(input.touchDown(10, 10, 0, Input.Buttons.LEFT));
    assertFalse(session.isOpen());
    assertTrue(input.touchUp(10, 10, 0, Input.Buttons.LEFT));
  }

  @Test
  void shouldPassInputWhenClosed() {
    assertFalse(input.keyDown(Input.Keys.E));
    assertFalse(input.keyTyped('e'));
    assertFalse(input.touchDown(10, 10, 0, Input.Buttons.LEFT));
    assertFalse(input.scrolled(0, 1));
    assertEquals(0, downs);
  }

  @Test
  void shouldConsumeInventoryAndPauseKeys() {
    session.open();
    assertTrue(input.keyDown(Input.Keys.I));
    assertTrue(input.keyDown(Input.Keys.P));
    assertTrue(input.keyTyped('i'));
    assertTrue(session.isOpen());
  }

  @Test
  void shouldNotBuyFromFinalDialogueClick() {
    InputService service = new InputService(mock(InputFactory.class));
    service.register(input);
    service.register(
        new InputComponent(20) {
          @Override
          public boolean touchDown(int x, int y, int pointer, int button) {
            session.open();
            return true;
          }
        });
    assertTrue(service.touchDown(10, 10, 0, Input.Buttons.LEFT));
    assertTrue(session.isOpen());
    assertEquals(0, downs);
  }
}
