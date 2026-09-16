package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMistComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerCerberusMistDebuffComponentTest {
  private Entity player;
  private Entity leftHead;
  private Entity anotherHead;
  private CombatStatsComponent stats;
  private PlayerCerberusMistDebuffComponent mistDebuff;

  @BeforeEach
  void setUp() {
    stats = new CombatStatsComponent(100, 10, 4f, 1f);
    mistDebuff = new PlayerCerberusMistDebuffComponent();

    player =
        new Entity()
            .addComponent(stats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(mistDebuff);
    player.create();

    leftHead = new Entity();
    anotherHead = new Entity();
  }

  @Test
  void shouldSlowPlayerAndDoubleIncomingDamageInsideMist() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    assertTrue(mistDebuff.isMistDebuffed());
    assertEquals(2f, stats.getEffectiveMovementSpeed());

    stats.takeDamage(10);

    assertEquals(80, stats.getHealth());
  }

  @Test
  void shouldRestoreSpeedAndDamageAfterLeavingMist() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    stats.takeDamage(10);
    player.getEvents().trigger(CerberusMistComponent.EXITED, leftHead);

    assertFalse(mistDebuff.isMistDebuffed());
    assertEquals(4f, stats.getEffectiveMovementSpeed());

    stats.takeDamage(10);

    assertEquals(70, stats.getHealth());
  }

  @Test
  void shouldNotStackWhenEnteringTheSameMistMoreThanOnce() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    assertEquals(2f, stats.getEffectiveMovementSpeed());

    stats.takeDamage(10);

    assertEquals(80, stats.getHealth());
  }

  @Test
  void shouldIgnoreExitFromAnotherMistSource() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);
    player.getEvents().trigger(CerberusMistComponent.EXITED, anotherHead);

    assertTrue(mistDebuff.isMistDebuffed());
    assertEquals(2f, stats.getEffectiveMovementSpeed());

    player.getEvents().trigger(CerberusMistComponent.EXITED, leftHead);

    assertFalse(mistDebuff.isMistDebuffed());
    assertEquals(4f, stats.getEffectiveMovementSpeed());
  }

  @Test
  void shouldClearEffectsWhenPlayerDiesAndAllowNewMistAfterHealing() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);
    stats.setHealth(0);

    assertFalse(mistDebuff.isMistDebuffed());

    stats.setHealth(100);
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    assertTrue(mistDebuff.isMistDebuffed());
    assertEquals(2f, stats.getEffectiveMovementSpeed());
  }

  @Test
  void shouldClearEffectsWhenComponentIsDisposed() {
    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    mistDebuff.dispose();

    assertFalse(mistDebuff.isMistDebuffed());
    assertEquals(4f, stats.getEffectiveMovementSpeed());

    player.getEvents().trigger(CerberusMistComponent.ENTERED, leftHead);

    assertFalse(mistDebuff.isMistDebuffed());
  }
}
