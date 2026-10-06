package com.csse3200.game.components.shop;

/** Presentation boundary used by the shop session, independent of Scene2D layout. */
public interface ShopView {
  void show(Runnable onClose);

  void close();

  void refresh();
}
