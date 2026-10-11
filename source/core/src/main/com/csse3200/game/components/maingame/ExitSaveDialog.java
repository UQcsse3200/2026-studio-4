package com.csse3200.game.components.maingame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Builds the dialog that lets the player save, delete the save, or cancel before exiting. */
public final class ExitSaveDialog {

  public static Dialog create(
      Skin skin, Runnable saveAndExit, Runnable deleteSaveAndExit, Runnable onClose) {
    Dialog dialog = new Dialog("Exit game?", skin);
    dialog.text("Do you want to save your progress before exiting?");
    addButton(dialog, skin, "Save and exit", onClose, saveAndExit);
    addButton(dialog, skin, "Delete save and exit", onClose, deleteSaveAndExit);
    addButton(dialog, skin, "Cancel", onClose, () -> {});
    return dialog;
  }

  private static void addButton(
      Dialog dialog, Skin skin, String label, Runnable onClose, Runnable action) {
    TextButton button = new TextButton(label, skin);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onClose.run(); // resume first, same order as before
            action.run();
          }
        });
    dialog.button(button);
  }

  private ExitSaveDialog() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
