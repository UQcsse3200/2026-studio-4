package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Component that applies aphysical knockback impulse to an entity when hit. */
public class KnockbackComponent extends Component {
  private final float strength;

  /**
   * @param strength The magnitude of the knockback forceZ.
   */
  public KnockbackComponent(float strength) {
    this.strength = strength;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("hitReaction", this::onHitReaction);
  }

  /**
   * Calculates knockback direction vector and applies linear impulse via Box2D.
   *
   * @param attacker The entity causing the damage （can be null)
   */
  private void onHitReaction(Entity attacker) {
    if (attacker == null || entity == null) {
      System.out.println("击退失败：攻击者或实体为空");
      return;
    }

    PhysicsComponent physicsComponent = entity.getComponent(PhysicsComponent.class);
    if (physicsComponent == null || physicsComponent.getBody() == null) {
      System.out.println("击退失败：缺少物理组件");
      return;
    }

    Vector2 knockbackDir = entity.getCenterPosition().cpy().sub(attacker.getPosition()).nor();
    Vector2 impulse = knockbackDir.scl(strength);
    System.out.println("成功施加击退力，冲量大小为: " + impulse);

    physicsComponent
        .getBody()
        .applyLinearImpulse(impulse, physicsComponent.getBody().getWorldCenter(), true);
  }
}
