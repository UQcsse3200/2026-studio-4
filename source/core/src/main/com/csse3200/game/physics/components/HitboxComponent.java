package com.csse3200.game.physics.components;

/** Physics comp */
public class HitboxComponent extends ColliderComponent {
  @Override
  public void create() {
    setSensor(true);
    super.create();
  }

  @Override
  public void dispose(){
    setSensor(false);
    super.dispose();
  }
}
