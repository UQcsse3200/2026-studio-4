package com.csse3200.game.components.maingame;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/** Dialog that lets the player save, delete the save, or cancel before exiting. */
public class ExitSaveDialog extends Dialog {
  private final Runnable saveAndExit;
  private final Runnable deleteSaveAndExit;
  private final Runnable onCancel;

  public ExitSaveDialog(
      Skin skin, Runnable saveAndExit, Runnable deleteSaveAndExit, Runnable onCancel) {
    super("Exit game?", skin);
    this.saveAndExit = saveAndExit;
    this.deleteSaveAndExit = deleteSaveAndExit;
    this.onCancel = onCancel;

    text("Do you want to save your progress before exiting?");
    button("Save and exit", "save");
    button("Delete save and exit", "delete");
    button("Cancel", "cancel");
  }

  @Override
  protected void result(Object object) {
    onCancel.run();

    if ("save".equals(object)) {
      saveAndExit.run();
    } else if ("delete".equals(object)) {
      deleteSaveAndExit.run();
    }
  }
}
