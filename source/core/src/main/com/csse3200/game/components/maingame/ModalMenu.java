package com.csse3200.game.components.maingame;

/**
 * A menu the player picks from with the keyboard while it is open, such as Hecate's gifts or an
 * NPC's offer to teleport. Its state lives here, away from how it looks, so it can be tested
 * without a GL context.
 */
public interface ModalMenu {
  /**
   * @return whether the menu is on screen and taking input
   */
  boolean isOpen();

  /**
   * Moves the highlight, wrapping at both ends.
   *
   * @param delta rows to move by, usually -1 or 1
   */
  void moveSelection(int delta);

  /** Acts on the highlighted row and closes. */
  boolean confirm();

  /** Closes the menu and gives the player back their controls. */
  void close();

  /**
   * @return the highlighted row
   */
  int getSelectedIndex();
}
