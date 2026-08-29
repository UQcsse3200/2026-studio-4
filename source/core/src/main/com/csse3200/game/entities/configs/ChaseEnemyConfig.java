package com.csse3200.game.entities.configs;

import com.badlogic.gdx.math.Vector2;

/** Defines the properties stored in chase enemy config files to be loaded by the NPC Factory. */
public class ChaseEnemyConfig extends BaseEntityConfig {
  public Vector2 movement = new Vector2(2f, 2f);
}
