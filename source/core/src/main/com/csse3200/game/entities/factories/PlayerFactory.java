package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.FollowingCameraComponent;
import com.csse3200.game.components.items.CharmPickupComponent;
import com.csse3200.game.components.player.CharmEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerAnimationController;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create a player entity.
 *
 * <p>Predefined player properties are loaded from a config stored as a json file and should have
 * the properties stores in 'PlayerConfig'.
 */
public class PlayerFactory {
  private static final PlayerConfig stats =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  /**
   * Create a player entity.
   *
   * @return entity
   */
  public static Entity createPlayer() {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/idle_down.atlas", TextureAtlas.class));
    animator.addAnimation("idle_down", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idle_left", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idle_right", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idle_up", 0.1f, Animation.PlayMode.LOOP);

    Entity player =
        new Entity()
            .addComponent(animator)
            // .addComponent(new TriggeredRenderComponent("images/box_boy_leaf.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(
                new CombatStatsComponent(
                    stats.health, stats.baseAttack, stats.movementSpeed, stats.attackSpeed))
            .addComponent(new CharmEffectComponent())
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new CharmPickupComponent())
            .addComponent(inputComponent)
            .addComponent(new PlayerAnimationController())
            .addComponent(new PlayerStatsDisplay())
            // Weapon damage = round(baseAttack * multiplier); charms that raise base attack
            // therefore scale weapon hits too.
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent())
            .addComponent(new FollowingCameraComponent());

    // Sword is equipped by default; the "weapon" terminal command switches at runtime.
    player.getComponent(KnifeWeaponComponent.class).setEnabled(false);
    player.getComponent(BowWeaponComponent.class).setEnabled(false);

    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    player.getComponent(AnimationRenderComponent.class).scaleEntity();
    player.getComponent(AnimationRenderComponent.class).startAnimation("idle_down");
    return player;
  }

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
