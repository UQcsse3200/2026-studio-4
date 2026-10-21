package com.csse3200.game.screens;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.splash.IntroDisplay;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class IntroScreenTest {
  private GdxGame game;
  private IntroScreen screen;

  @BeforeEach
  void setUp() {
    game = mock(GdxGame.class);
    screen = new IntroScreen(game);
  }

  @AfterEach
  void tearDown() {
    if (screen != null) {
      screen.dispose();
    }
  }

  @Test
  void switchesToMainMenuOnlyAfterIntroFinishes() throws ReflectiveOperationException {
    screen.render(0f);
    verifyNoInteractions(game);

    Field displayField = IntroScreen.class.getDeclaredField("display");
    displayField.setAccessible(true);
    IntroDisplay display = (IntroDisplay) displayField.get(screen);
    Field finishedField = IntroDisplay.class.getDeclaredField("finished");
    finishedField.setAccessible(true);
    finishedField.setBoolean(display, true);

    screen.render(0f);
    screen.render(0f);

    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
  }
}
