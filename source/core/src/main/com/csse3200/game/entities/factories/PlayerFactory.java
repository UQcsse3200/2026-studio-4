package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.ConsumableSelectionComponent;
import com.csse3200.game.components.player.InteractionPromptDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.InvisibilityPotionComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerAnimationController;
import com.csse3200.game.components.player.PlayerCerberusMistDebuffComponent;
import com.csse3200.game.components.player.PlayerDamageFlashComponent;
import com.csse3200.game.components.player.PlayerPetrificationComponent;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.player.SpeedPotionAfterimageComponent;
import com.csse3200.game.components.spells.FreezeSpellComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.spells.SpellAoeVisualComponent;
import com.csse3200.game.components.spells.targeting.StrategyWithinRadius;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponAssetsComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
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
  /** How far a spell reaches from the player, in world units; the screen is 20 units wide. */
  private static final float SPELL_RADIUS = 3f;

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
    animator.addAnimation("attack_down", 0.06f, Animation.PlayMode.NORMAL);
    animator.addAnimation("attack_left", 0.06f, Animation.PlayMode.NORMAL);
    animator.addAnimation("attack_right", 0.06f, Animation.PlayMode.NORMAL);
    animator.addAnimation("attack_up", 0.06f, Animation.PlayMode.NORMAL);
    animator.addAnimation("walk_down", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walk_left", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walk_right", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walk_up", 0.1f, Animation.PlayMode.LOOP);

    Entity player =
        new Entity()
            .addComponent(animator)
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(
                new CombatStatsComponent(
                    stats.health, stats.baseAttack, stats.movementSpeed, stats.attackSpeed))
            .addComponent(new PlayerAbilitiesComponent())
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new ConsumableSelectionComponent())
            .addComponent(new ConsumableEffectComponent())
            .addComponent(new SpeedPotionAfterimageComponent())
            .addComponent(new InvisibilityPotionComponent())
            .addComponent(new ItemPickupComponent())
            .addComponent(inputComponent)
            .addComponent(new PlayerAnimationController())
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new InteractionPromptDisplay())
            // Weapon damage = round(baseAttack * multiplier); charms that raise base attack
            // therefore scale weapon hits too.
            .addComponent(new WeaponAssetsComponent())
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            // Tracks which weapons are upgraded; the "upgrade" terminal command grants them.
            .addComponent(new WeaponUpgradeComponent())
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new PlayerCerberusMistDebuffComponent())
            .addComponent(new PlayerDamageFlashComponent())
            .addComponent(new PlayerPetrificationComponent())
            .addComponent(new BowWeaponComponent())
            // Owns the equipped weapon: equips the sword and disables the rest on create.
            .addComponent(new WeaponSelectionComponent())
            // Both spells share one area disc; each cast says what colour and how far it reached.
            .addComponent(new SpellAoeVisualComponent())
            .addComponent(
                new LightningSpellComponent(5f, 25, 400L, new StrategyWithinRadius(SPELL_RADIUS)))
            .addComponent(
                new FreezeSpellComponent(5f, 5000L, new StrategyWithinRadius(SPELL_RADIUS)));

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
