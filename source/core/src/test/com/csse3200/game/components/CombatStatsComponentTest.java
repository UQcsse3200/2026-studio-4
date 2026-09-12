package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatStatsComponentTest {
  @Test
  void shouldClassifyHostileBodiesAndProjectilesUsingLayerMasks() {
    assertFalse(CombatStatsComponent.isHostileAttacker(null));
    assertFalse(CombatStatsComponent.isHostileAttacker(new Entity()));
    for (short layer :
        new short[] {PhysicsLayer.NPC, (short) (PhysicsLayer.NPC | PhysicsLayer.WEAPON)}) {
      assertTrue(
          CombatStatsComponent.isHostileAttacker(
              new Entity().addComponent(new HitboxComponent().setLayer(layer))));
      assertTrue(
          CombatStatsComponent.isHostileAttacker(
              new Entity().addComponent(new ColliderComponent().setLayer(layer))));
    }
    for (short layer :
        new short[] {PhysicsLayer.PLAYER, (short) (PhysicsLayer.PLAYER | PhysicsLayer.NPC)}) {
      assertTrue(
          CombatStatsComponent.isHostileAttacker(
              new Entity().addComponent(new TouchAttackComponent(layer))));
    }
    Entity friendlyWeapon =
        new Entity()
            .addComponent(new TouchAttackComponent(PhysicsLayer.NPC))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.PLAYER));
    assertFalse(CombatStatsComponent.isHostileAttacker(friendlyWeapon));
  }

  @Test
  void shouldBlockOnlyHostileDamageDuringInvisibilityAndResumeAtExpiry() {
    GameTime time = mock(GameTime.class);
    CombatStatsComponent combat = new CombatStatsComponent(100, 11);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    Entity player = new Entity().addComponent(combat).addComponent(abilities);
    player.create();
    List<String> events = new ArrayList<>();
    player.getEvents().addListener("damageBlocked", () -> events.add("blocked"));
    player.getEvents().addListener("hitReaction", (Entity source) -> events.add("reaction"));
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) -> events.add("damage:" + lost));
    Entity hostile = new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    assertTrue(abilities.tryInvisibility());

    combat.takeDamage(10, hostile);
    assertEquals(100, combat.getHealth());
    assertEquals(List.of("blocked"), events);
    events.clear();
    combat.takeDamage(7, new Entity());
    combat.takeDamage(3);
    assertEquals(90, combat.getHealth());
    assertEquals(List.of("damage:7", "reaction", "damage:3", "reaction"), events);

    when(time.getTime()).thenReturn(PlayerAbilitiesComponent.INVISIBILITY_DURATION_MS);
    events.clear();
    combat.takeDamage(10, hostile);
    assertEquals(80, combat.getHealth());
    assertEquals(List.of("damage:10", "reaction"), events);
  }

  @Test
  void shouldReportActualScaledAndFloorLimitedLossWithOriginalAttacker() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 0);
    Entity target = new Entity().addComponent(combat);
    Entity projectile = new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    List<DamageEvent> events = new ArrayList<>();
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) -> {
              assertEquals(remaining.intValue(), combat.getHealth());
              events.add(new DamageEvent(source, lost, remaining));
            });
    combat.setIncomingDamageMultiplier(0.5f);
    combat.setMinimumHealth(90);

    combat.takeDamage(5, projectile);
    combat.takeDamage(100, projectile);
    combat.takeDamage(100, projectile);

    assertEquals(
        List.of(new DamageEvent(projectile, 3, 97), new DamageEvent(projectile, 7, 90)), events);
  }

  @Test
  void shouldEmitDeathBeforeDamageAndReportOnlyRemainingHealthOnOverkill() {
    CombatStatsComponent combat = new CombatStatsComponent(10, 0);
    Entity target = new Entity().addComponent(combat);
    List<String> order = new ArrayList<>();
    List<DamageEvent> damage = new ArrayList<>();
    target
        .getEvents()
        .addListener("updateHealth", (Integer health) -> order.add("health:" + health));
    target.getEvents().addListener("entityDied", () -> order.add("death"));
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) -> {
              order.add("damage");
              damage.add(new DamageEvent(source, lost, remaining));
            });
    target.getEvents().addListener("hitReaction", (Entity source) -> order.add("reaction"));

    combat.takeDamage(100);
    assertEquals(List.of("health:0", "death", "damage", "reaction"), order);
    assertEquals(List.of(new DamageEvent(null, 10, 0)), damage);
    combat.takeDamage(100);
    assertEquals(1, damage.size());
    assertEquals(1, order.stream().filter("death"::equals).count());
  }

  @Test
  void shouldNotReportDamageForSettersNonpositiveOrBlockedDamage() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 0);
    Entity target = new Entity().addComponent(combat);
    List<String> events = new ArrayList<>();
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) -> events.add("damage"));
    target.getEvents().addListener("hitReaction", (Entity source) -> events.add("reaction"));
    target.getEvents().addListener("damageBlocked", () -> events.add("blocked"));
    combat.setHealth(80);
    combat.addHealth(-10);
    combat.takeDamage(0);
    combat.takeDamage(-10);
    assertTrue(events.isEmpty());

    combat.setInvulnerable(true);
    combat.takeDamage(10);
    assertEquals(List.of("blocked"), events);
    combat.setInvulnerable(false);
    combat.setIncomingDamageMultiplier(0.1f);
    events.clear();
    combat.takeDamage(1);
    assertEquals(List.of("blocked", "reaction"), events);
    assertEquals(70, combat.getHealth());
  }

  @Test
  void shouldApplyLastStandToEffectiveStatsAndHitsWithoutMutatingRawBuffs() {
    GameTime time = mock(GameTime.class);
    CombatStatsComponent combat = new CombatStatsComponent(100, 11, 3f, 2f);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    Entity player = new Entity().addComponent(combat).addComponent(abilities);
    player.create();
    abilities.enableLastStand();
    assertEquals(11, combat.getEffectiveBaseAttack());
    assertEquals(2f, combat.getEffectiveAttackSpeed());
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));

    assertEquals(17, combat.getEffectiveBaseAttack());
    assertEquals(3f, combat.getEffectiveAttackSpeed());
    List<Integer> rawUpdates = new ArrayList<>();
    player.getEvents().addListener("updateBaseAttack", (EventListener1<Integer>) rawUpdates::add);
    combat.addBaseAttack(2);
    combat.addAttackSpeed(2f);
    assertEquals(20, combat.getEffectiveBaseAttack());
    assertEquals(6f, combat.getEffectiveAttackSpeed());
    assertEquals(13, combat.getBaseAttack());
    assertEquals(4f, combat.getAttackSpeed());
    assertEquals(3f, combat.getMovementSpeed());
    assertEquals(List.of(13), rawUpdates);

    CombatStatsComponent victim = new CombatStatsComponent(100, 0);
    Entity target = new Entity().addComponent(victim);
    List<DamageEvent> damage = new ArrayList<>();
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) ->
                damage.add(new DamageEvent(source, lost, remaining)));
    victim.hit(combat);
    assertEquals(List.of(new DamageEvent(player, 20, 80)), damage);

    when(time.getTime()).thenReturn(PlayerAbilitiesComponent.LAST_STAND_DURATION_MS);
    assertEquals(13, combat.getEffectiveBaseAttack());
    assertEquals(4f, combat.getEffectiveAttackSpeed());
    assertEquals(List.of(13), rawUpdates);
  }

  @Test
  void shouldUseRawEffectiveStatsWithoutEntityOrAbilities() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 11, 3f, 2f);
    assertEquals(11, combat.getEffectiveBaseAttack());
    assertEquals(2f, combat.getEffectiveAttackSpeed());
    new Entity().addComponent(combat);
    assertEquals(11, combat.getEffectiveBaseAttack());
    assertEquals(2f, combat.getEffectiveAttackSpeed());
  }

  private record DamageEvent(Entity attacker, int healthLost, int remainingHealth) {}

  @Test
  void shouldKeepLethalDamageSnapshotWhenDeathListenerRevivesTarget() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 0);
    Entity target = new Entity().addComponent(combat);
    List<DamageEvent> damage = new ArrayList<>();
    target.getEvents().addListener("entityDied", () -> combat.setHealth(100));
    target
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer remaining) ->
                damage.add(new DamageEvent(source, lost, remaining)));

    combat.takeDamage(1000);

    assertEquals(100, combat.getHealth());
    assertEquals(List.of(new DamageEvent(null, 100, 0)), damage);
  }

  @Test
  void shouldSetGetHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getHealth());
    assertEquals(100, combat.getMaxHealth());

    combat.setHealth(150);
    assertEquals(100, combat.getHealth());

    combat.setHealth(50);
    assertEquals(50, combat.getHealth());

    combat.setHealth(-50);
    assertEquals(0, combat.getHealth());
  }

  @Test
  void shouldCheckIsDead() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertFalse(combat.isDead());

    combat.setHealth(0);
    assertTrue(combat.isDead());
  }

  @Test
  void shouldAddHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addHealth(-500);
    assertEquals(0, combat.getHealth());

    combat.addHealth(100);
    combat.addHealth(-20);
    assertEquals(80, combat.getHealth());
  }

  @Test
  void shouldSetGetMaxHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getMaxHealth());

    combat.setMaxHealth(200);
    assertEquals(200, combat.getMaxHealth());

    combat.setMaxHealth(-50);
    assertEquals(200, combat.getMaxHealth());
  }

  @Test
  void shouldSetGetBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(20, combat.getBaseAttack());

    combat.setBaseAttack(150);
    assertEquals(150, combat.getBaseAttack());

    combat.setBaseAttack(-50);
    assertEquals(150, combat.getBaseAttack());
  }

  @Test
  void shouldAddBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBaseAttack(10);
    assertEquals(30, combat.getBaseAttack());

    combat.addBaseAttack(-10);
    assertEquals(20, combat.getBaseAttack());
  }

  @Test
  void shouldTriggerBaseAttackUpdate() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    Entity entity = new Entity().addComponent(combat);
    entity.create();
    int[] updatedAttack = {0};
    entity
        .getEvents()
        .addListener("updateBaseAttack", attack -> updatedAttack[0] = (Integer) attack);

    combat.addBaseAttack(10);

    assertEquals(30, updatedAttack[0]);
  }

  @Test
  void shouldGetSetMovementSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 30, 3f, 4f);
    assertEquals(3f, combat.getMovementSpeed());

    combat.setMovementSpeed(5f);
    assertEquals(5f, combat.getMovementSpeed());

    combat.setMovementSpeed(-4f);
    assertEquals(5f, combat.getMovementSpeed());
  }

  @Test
  void shouldTriggerMovementSpeedUpdate() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4.0f, 6.0f);
    Entity entity = new Entity().addComponent(combat);
    entity.create();
    float[] updatedMovementSpeed = {0};
    entity
        .getEvents()
        .addListener(
            "updateMovementSpeed",
            movementSpeed -> updatedMovementSpeed[0] = (float) movementSpeed);

    combat.addMovementSpeed(2.0f);

    assertEquals(6.0f, updatedMovementSpeed[0]);
  }

  @Test
  void shouldAddMovementSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4f, 7f);
    combat.addMovementSpeed(-3);
    assertEquals(1f, combat.getMovementSpeed());

    combat.addMovementSpeed(6f);
    combat.addMovementSpeed(-8F);
    assertEquals(7, combat.getMovementSpeed());
  }

  @Test
  void shouldGetSetAttackSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 30, 3f, 4f);
    assertEquals(4f, combat.getAttackSpeed());

    combat.setAttackSpeed(2f);
    assertEquals(2f, combat.getAttackSpeed());

    combat.setAttackSpeed(-4f);
    assertEquals(2f, combat.getAttackSpeed());
  }

  @Test
  void shouldAddAttackSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4f, 7f);
    combat.addAttackSpeed(-7);
    assertEquals(0, combat.getAttackSpeed());

    combat.addAttackSpeed(6f);
    combat.addAttackSpeed(-2F);
    assertEquals(4f, combat.getAttackSpeed());
  }

  @Test
  void shouldTriggerAttackSpeedUpdate() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4.0f, 6.0f);
    Entity entity = new Entity().addComponent(combat);
    entity.create();
    float[] updateAttackSpeed = {0};
    entity
        .getEvents()
        .addListener(
            "updateAttackSpeed", attackSpeed -> updateAttackSpeed[0] = (float) attackSpeed);

    combat.addAttackSpeed(1.0f);

    assertEquals(7.0f, updateAttackSpeed[0]);
  }

  @Test
  void shouldBlockDamageWhileInvulnerable() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    Entity entity = new Entity().addComponent(combat);
    entity.create();

    int[] blockedEvents = {0};
    entity.getEvents().addListener("damageBlocked", () -> blockedEvents[0]++);

    combat.setInvulnerable(true);
    combat.takeDamage(30);

    assertEquals(100, combat.getHealth());
    assertEquals(1, blockedEvents[0]);
  }

  @Test
  void shouldScaleIncomingDamage() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);

    combat.setIncomingDamageMultiplier(0.25f);
    combat.takeDamage(20);

    assertEquals(95, combat.getHealth());
    assertEquals(0.25f, combat.getIncomingDamageMultiplier());
    assertThrows(IllegalArgumentException.class, () -> combat.setIncomingDamageMultiplier(-0.1f));
  }

  @Test
  void shouldApplyMinimumHealthOnlyToIncomingDamage() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.setMinimumHealth(90);

    combat.takeDamage(50);
    assertEquals(90, combat.getHealth());

    combat.setHealth(80);
    assertEquals(80, combat.getHealth());

    assertThrows(IllegalArgumentException.class, () -> combat.setMinimumHealth(101));
  }
}
