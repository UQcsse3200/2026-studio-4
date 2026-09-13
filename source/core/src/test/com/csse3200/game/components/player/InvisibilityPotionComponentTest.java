package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InvisibilityPotionComponentTest {
  private final AtomicLong nowMs = new AtomicLong();
  private Entity player;
  private InvisibilityPotionComponent invisibility;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> nowMs.get());
    ServiceLocator.registerTimeSource(time);
    nowMs.set(0L);

    stats = new CombatStatsComponent(100, 10);
    invisibility = new InvisibilityPotionComponent();
    player = new Entity().addComponent(stats).addComponent(invisibility);
    player.create();
  }

  @Test
  void shouldApplyDurationAndCooldownOnUse() {
    assertTrue(invisibility.tryUse());
    assertTrue(invisibility.isInvisible());
    assertTrue(stats.isInvulnerable());
    assertEquals(InvisibilityPotionComponent.DURATION_MS, invisibility.getRemainingDurationMs());
    assertEquals(InvisibilityPotionComponent.COOLDOWN_MS, invisibility.getRemainingCooldownMs());
    assertEquals("Invisibility: 15s", invisibility.getDurationHudText());
    assertEquals("Invis CD: 45s", invisibility.getCooldownHudText());
  }

  @Test
  void shouldRefreshWhileInvisibleAndRejectAfterExpiryUntilCooldownEnds() {
    assertTrue(invisibility.tryUse());
    nowMs.set(5_000L);
    assertTrue(invisibility.tryUse());
    assertEquals(InvisibilityPotionComponent.DURATION_MS, invisibility.getRemainingDurationMs());
    assertEquals(InvisibilityPotionComponent.COOLDOWN_MS, invisibility.getRemainingCooldownMs());

    nowMs.set(20_000L);
    invisibility.update();
    assertFalse(invisibility.isInvisible());
    assertFalse(stats.isInvulnerable());
    assertFalse(invisibility.tryUse());

    nowMs.set(50_000L);
    assertTrue(invisibility.tryUse());
    assertTrue(invisibility.isInvisible());
  }

  @Test
  void shouldIgnoreIncomingDamageWhileInvisible() {
    assertTrue(invisibility.tryUse());
    stats.takeDamage(40);
    assertEquals(100, stats.getHealth());

    nowMs.set(InvisibilityPotionComponent.DURATION_MS);
    invisibility.update();
    stats.takeDamage(40);
    assertEquals(60, stats.getHealth());
  }

  @Test
  void qaApplyShouldBypassCooldownAndKeepProductionCooldown() {
    assertTrue(invisibility.tryUse());
    nowMs.set(20_000L);
    invisibility.update();
    assertFalse(invisibility.isInvisible());
    assertFalse(invisibility.tryUse());

    assertTrue(invisibility.applyForQa());
    assertTrue(invisibility.isInvisible());
    assertEquals(25_000L, invisibility.getRemainingCooldownMs());
  }

  @Test
  void shouldNotifyAiWhenInvisibilityStarts() {
    AtomicInteger stealthSignals = new AtomicInteger();
    player.getEvents().addListener("invisiblePlayer", stealthSignals::incrementAndGet);

    invisibility.tryUse();
    assertEquals(1, stealthSignals.get());
  }
}
