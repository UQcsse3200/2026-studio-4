package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbilityAttunementComponentTest {
  private static final long START = 1_000;
  private GameTime time;
  private CombatStatsComponent stats;
  private PlayerAbilitiesComponent abilities;
  private AbilityAttunementComponent attunement;
  private Entity player;
  private final List<String> attuned = new ArrayList<>();
  private final List<String> unattuned = new ArrayList<>();

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(START);
    stats = new CombatStatsComponent(100, 10, 2f, 4f);
    abilities = new PlayerAbilitiesComponent(time);
    attunement = new AbilityAttunementComponent();
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(attunement);
    player.create();
    player
        .getEvents()
        .addListener(
            AbilityAttunementComponent.ABILITY_ATTUNED, (EventListener1<String>) attuned::add);
    player
        .getEvents()
        .addListener(AbilityAttunementComponent.ABILITY_UNATTUNED, () -> unattuned.add("cleared"));
    // Components are created in an unspecified order, so the lock may land on the first update.
    attunement.update();
  }

  @Test
  void shouldStartWithNothingAttunedAndEverythingLocked() {
    assertNull(attunement.getAttuned());
    assertFalse(attunement.isAttuned(Invisibility.class));
    assertFalse(abilities.isUnlocked(Invisibility.class), "Invisibility must not be free");
    assertFalse(abilities.isUnlocked(LastStand.class));
    assertFalse(abilities.tryActivate(Invisibility.class));
  }

  @Test
  void shouldKeepAbilitiesLockedAcrossRepeatedUpdates() {
    for (int i = 0; i < 5; i++) {
      attunement.update();
    }
    assertFalse(abilities.isUnlocked(Invisibility.class));
    assertNull(attunement.getAttuned());
  }

  @Test
  void shouldUnlockOnlyTheAttunedAbility() {
    assertTrue(attunement.attune(Invisibility.class));

    assertEquals(Invisibility.class, attunement.getAttuned());
    assertTrue(attunement.isAttuned(Invisibility.class));
    assertTrue(abilities.isUnlocked(Invisibility.class));
    assertFalse(abilities.isUnlocked(LastStand.class));
    assertEquals(List.of(Invisibility.NAME), attuned);
    assertTrue(abilities.tryActivate(Invisibility.class));
  }

  @Test
  void shouldSwapExclusivelyWhenAttuningAnother() {
    assertTrue(attunement.attune(Invisibility.class));
    assertTrue(attunement.attune(LastStand.class));

    assertEquals(LastStand.class, attunement.getAttuned());
    assertTrue(abilities.isUnlocked(LastStand.class));
    assertFalse(abilities.isUnlocked(Invisibility.class), "The old ability is taken back");
    assertFalse(abilities.tryActivate(Invisibility.class));
    assertEquals(List.of(Invisibility.NAME, LastStand.NAME), attuned);
  }

  @Test
  void shouldEndARunningAbilityWhenAttuningAway() {
    assertTrue(attunement.attune(Invisibility.class));
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertTrue(abilities.isActive(Invisibility.class));

    assertTrue(attunement.attune(LastStand.class));
    assertFalse(abilities.isActive(Invisibility.class));
  }

  @Test
  void shouldTreatReattuningTheSameAbilityAsANoOp() {
    assertTrue(attunement.attune(Invisibility.class));
    assertTrue(abilities.tryActivate(Invisibility.class));

    assertTrue(attunement.attune(Invisibility.class), "Re-attuning is allowed");
    assertTrue(abilities.isActive(Invisibility.class), "and must not cancel what is running");
    assertEquals(List.of(Invisibility.NAME), attuned, "and must not announce a change");
  }

  @Test
  void shouldRefuseUnknownAndNullAbilitiesWithoutChangingAnything() {
    assertTrue(attunement.attune(LastStand.class));

    assertFalse(attunement.attune(UnregisteredAbility.class));
    assertFalse(attunement.attune(null));

    assertEquals(LastStand.class, attunement.getAttuned());
    assertTrue(abilities.isUnlocked(LastStand.class));
    assertEquals(List.of(LastStand.NAME), attuned);
  }

  @Test
  void shouldTakeBackEverythingWhenCleared() {
    assertTrue(attunement.attune(Invisibility.class));

    attunement.clearAttunement();

    assertNull(attunement.getAttuned());
    assertFalse(abilities.isUnlocked(Invisibility.class));
    assertFalse(abilities.isUnlocked(LastStand.class));
    assertEquals(List.of("cleared"), unattuned);
  }

  @Test
  void shouldAnnounceNothingWhenClearingAnAlreadyEmptyAttunement() {
    attunement.clearAttunement();

    assertNull(attunement.getAttuned());
    assertTrue(unattuned.isEmpty());
  }

  @Test
  void shouldTakeBackTheAttunementWhenThePlayerDies() {
    assertTrue(attunement.attune(Invisibility.class));

    stats.setHealth(0);
    assertTrue(stats.isDead());

    assertNull(attunement.getAttuned());
    assertEquals(List.of("cleared"), unattuned);
    // PlayerAbilitiesComponent also listens for entityDied, and whichever listener runs last wins,
    // so the lock itself is only guaranteed from the next frame. A dead player cannot cast either
    // way, which is the guarantee that actually matters.
    assertFalse(abilities.tryActivate(Invisibility.class));
    attunement.update();
    assertFalse(abilities.isUnlocked(Invisibility.class));
  }

  @Test
  void shouldStayLockedAfterDeathEvenThoughAbilitiesRelockToTheirDefaults() {
    assertTrue(attunement.attune(Invisibility.class));
    stats.setHealth(0);

    // PlayerAbilitiesComponent relocks every ability to unlockedByDefault while the player is
    // dead, which hands Invisibility back. The enforcement in update() has to take it away again.
    abilities.update();
    attunement.update();

    assertFalse(abilities.isUnlocked(Invisibility.class), "Death must not grant a free ability");
    assertFalse(abilities.isUnlocked(LastStand.class));
  }

  @Test
  void shouldRefuseToAttuneADeadPlayer() {
    stats.setHealth(0);
    assertTrue(stats.isDead());

    assertFalse(attunement.attune(LastStand.class));

    assertNull(attunement.getAttuned());
    assertTrue(attuned.isEmpty());
  }

  @Test
  void shouldDoNothingUsefulWithoutAnAbilitiesComponent() {
    AbilityAttunementComponent orphan = new AbilityAttunementComponent();
    Entity bare = new Entity().addComponent(orphan);
    bare.create();
    orphan.update();

    assertFalse(orphan.attune(Invisibility.class));
    assertNull(orphan.getAttuned());
    orphan.clearAttunement();
    assertNull(orphan.getAttuned());
  }

  /** An ability that is never registered, to prove unknown types are rejected. */
  private static final class UnregisteredAbility extends PlayerAbility {
    private UnregisteredAbility() {
      super("unregistered", 1_000, true);
    }

    @Override
    public void start() {
      // Never started.
    }

    @Override
    public void stop() {
      // Never started.
    }

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public long getRemainingMs() {
      return 0;
    }
  }
}
