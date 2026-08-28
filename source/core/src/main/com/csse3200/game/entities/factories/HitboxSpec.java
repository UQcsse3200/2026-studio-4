package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;

/** Spawn fields for {@link HitboxFactory}. Set {@code owner} so melee follows the wielder. */
public class HitboxSpec {
  public Vector2 position;
  public Vector2 size;
  public float lifetime;
  public short layer = PhysicsLayer.WEAPON;
  public short targetLayer = PhysicsLayer.NPC;
  public int damage;
  public float knockback;
  public Entity owner;
  public Vector2 localOffset = new Vector2();
}
