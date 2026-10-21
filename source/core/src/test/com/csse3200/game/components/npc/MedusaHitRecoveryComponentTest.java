package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.tasks.LungeAttackTask;
import com.csse3200.game.components.weapons.WeaponAttackSourceComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MedusaHitRecoveryComponentTest {
  private GameTime time;
  private Entity player;
  private LungeAttackTask lunge;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    lunge = mock(LungeAttackTask.class);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 1))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.create();
    player.setPosition(2f, 0f);
  }

  @Test
  void playerHitImmediatelyBlocksContactDamageAndKnockbackUntilRecoveryEnds() {
    Entity medusa = medusa();
    stats(medusa).takeDamage(1, player);

    verify(lunge).interrupt();
    assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    assertNull(effects(medusa).getTint());
    contact(medusa, "collisionStart");
    assertEquals(100, stats(player).getHealth());
    assertTrue(player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().isZero());

    contact(medusa, "collisionEnd");
    when(time.getTime()).thenReturn(349L);
    contact(medusa, "collisionStart");
    assertEquals(100, stats(player).getHealth());

    when(time.getTime()).thenReturn(350L);
    medusa.getComponent(TouchAttackComponent.class).update();
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    assertEquals(90, stats(player).getHealth());
    assertTrue(player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x > 0f);
  }

  @Test
  void successivePlayerHitsRefreshRecoveryFromTheLatestHit() {
    Entity medusa = medusa();
    stats(medusa).takeDamage(1, player);
    when(time.getTime()).thenReturn(300L);
    stats(medusa).takeDamage(1, player);

    when(time.getTime()).thenReturn(350L);
    assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    when(time.getTime()).thenReturn(649L);
    assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    when(time.getTime()).thenReturn(650L);
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    verify(lunge, times(2)).interrupt();
  }

  @Test
  void recoveryDoesNotRemoveAnExistingFreeze() {
    Entity medusa = medusa();
    StatusEffect frozen = StatusEffectsFactory.createFrozen(time, 1000L);
    effects(medusa).addStatusEffect(frozen);
    stats(medusa).takeDamage(1, player);
    when(time.getTime()).thenReturn(200L);
    stats(medusa).takeDamage(1, player);

    when(time.getTime()).thenReturn(550L);
    effects(medusa).update();
    assertTrue(effects(medusa).hasStatusEffect(frozen));
    assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    when(time.getTime()).thenReturn(1000L);
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
  }

  @Test
  void playerWeaponsAndOwnerlessPlayerArrowsTriggerRecovery() {
    Entity sword =
        new Entity().addComponent(new WeaponAttackSourceComponent(player, WeaponType.SWORD));
    Entity arrow = weaponHitbox(PhysicsLayer.NPC);

    for (Entity source : new Entity[] {sword, arrow}) {
      Entity medusa = medusa();
      stats(medusa).takeDamage(1, source);
      assertEquals(99, stats(medusa).getHealth());
      assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    }
    verify(lunge, times(2)).interrupt();
  }

  @Test
  void unrelatedDamageSourcesDoNotTriggerRecovery() {
    Entity someoneElseWeapon =
        weaponHitbox(PhysicsLayer.NPC)
            .addComponent(new WeaponAttackSourceComponent(new Entity(), WeaponType.SWORD));
    Entity hostileWeapon = weaponHitbox((short) (PhysicsLayer.NPC | PhysicsLayer.PLAYER));

    for (Entity source : new Entity[] {null, new Entity(), someoneElseWeapon, hostileWeapon}) {
      Entity medusa = medusa();
      stats(medusa).takeDamage(1, source);
      assertEquals(99, stats(medusa).getHealth());
      assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    }
    verifyNoInteractions(lunge);
  }

  @Test
  void lightningDamageUsesItsCasterOnlyWhileTheSourceEventIsActive() {
    Entity medusa = medusa();
    medusa.getEvents().trigger(LightningSpellComponent.DAMAGE_SOURCE_EVENT, player);
    stats(medusa).takeDamage(1);
    assertTrue(StatusEffectsControllerComponent.isImmobilised(medusa));
    verify(lunge).interrupt();

    medusa.getEvents().<Entity>trigger(LightningSpellComponent.DAMAGE_SOURCE_EVENT, null);
    when(time.getTime()).thenReturn(350L);
    stats(medusa).takeDamage(1);
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));

    medusa.getEvents().trigger(LightningSpellComponent.DAMAGE_SOURCE_EVENT, new Entity());
    stats(medusa).takeDamage(1);
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    verify(lunge).interrupt();
  }

  @Test
  void zeroDamageAndBlockedHitsDoNotTriggerRecovery() {
    Entity medusa = medusa();
    stats(medusa).takeDamage(0, player);
    stats(medusa).setInvulnerable(true);
    stats(medusa).takeDamage(1, player);

    assertEquals(100, stats(medusa).getHealth());
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    verifyNoInteractions(lunge);
  }

  @Test
  void lethalHitDoesNotAddRecoveryAfterDeathClearsEffects() {
    Entity medusa = medusa();
    stats(medusa).takeDamage(100, player);

    assertTrue(stats(medusa).isDead());
    assertFalse(StatusEffectsControllerComponent.isImmobilised(medusa));
    verifyNoInteractions(lunge);
  }

  private Entity medusa() {
    Entity medusa =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f, 1.25f, true))
            .addComponent(new MedusaHitRecoveryComponent(player, lunge));
    medusa.create();
    return medusa;
  }

  private Entity weaponHitbox(short targetLayer) {
    return new Entity()
        .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
        .addComponent(new TouchAttackComponent(targetLayer));
  }

  private CombatStatsComponent stats(Entity entity) {
    return entity.getComponent(CombatStatsComponent.class);
  }

  private StatusEffectsControllerComponent effects(Entity entity) {
    return entity.getComponent(StatusEffectsControllerComponent.class);
  }

  private void contact(Entity medusa, String event) {
    medusa
        .getEvents()
        .trigger(
            event,
            medusa.getComponent(HitboxComponent.class).getFixture(),
            player.getComponent(HitboxComponent.class).getFixture());
  }
}
