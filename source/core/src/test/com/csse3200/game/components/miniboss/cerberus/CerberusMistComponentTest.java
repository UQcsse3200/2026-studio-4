package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerCerberusMistDebuffComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerberusMistComponentTest {
  private GameTime time;
  private RenderService renderService;
  private Entity player;
  private CombatStatsComponent playerStats;
  private PlayerCerberusMistDebuffComponent mistDebuff;
  private Entity leftHead;
  private CombatStatsComponent leftStats;
  private CerberusMistComponent mist;
  private int entered;
  private int exited;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    renderService = mock(RenderService.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(renderService);

    playerStats = new CombatStatsComponent(100, 0);
    playerStats.setMovementSpeed(3f);
    mistDebuff = new PlayerCerberusMistDebuffComponent();
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(mistDebuff);
    player.setPosition(1f, 0f);
    player.getEvents().addListener(CerberusMistComponent.ENTERED, (Entity source) -> entered++);
    player.getEvents().addListener(CerberusMistComponent.EXITED, (Entity source) -> exited++);
    player.create();

    leftStats = new CombatStatsComponent(100, 10);
    mist = new CerberusMistComponent(player);

    leftHead = new Entity().addComponent(leftStats).addComponent(mist);
    leftHead.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    mist.update();
  }

  @Test
  void shouldWaitForCooldownAndExpireAfterDuration() {
    int[] casts = {0};
    leftHead.getEvents().addListener("cerberusCast", () -> casts[0]++);
    tick(4f);
    assertFalse(mist.isMistActive());
    assertEquals(0, casts[0]);

    tick(1f);
    assertTrue(mist.isMistActive());
    assertEquals(1, entered);
    assertEquals(1, casts[0]);

    tick(2f);
    assertTrue(mist.isMistActive());
    assertEquals(1, entered);
    assertEquals(1, casts[0]);

    tick(1f);
    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    assertEquals(3f, playerStats.getMovementSpeed());
    assertFalse(mistDebuff.isMistDebuffed());

    tick(4f);
    assertFalse(mist.isMistActive());

    tick(1f);
    assertTrue(mist.isMistActive());
    assertEquals(2, entered);
    assertEquals(2, casts[0]);
  }

  @Test
  void shouldKeepMistFixedAndReportExitAndReentry() {
    tick(5f);
    Vector2 originalCentre = mist.getMistCentre();

    player.setPosition(10f, 0f);
    tick(0.1f);

    assertEquals(originalCentre, mist.getMistCentre());
    assertEquals(1, exited);
    assertEquals(3f, playerStats.getMovementSpeed());

    tick(0.1f);
    assertEquals(1, exited);

    player.setPosition(1f, 0f);
    tick(0.1f);

    assertEquals(2, entered);
  }

  @Test
  void shouldNotCastOutsideRange() {
    player.setPosition(20f, 0f);
    tick(5f);

    assertFalse(mist.isMistActive());
    assertEquals(0, entered);

    player.setPosition(1f, 0f);
    tick(0f);

    assertTrue(mist.isMistActive());
  }

  @Test
  void shouldClearImmediatelyWhenLeftHeadDiesAndNeverRecast() {
    tick(5f);
    assertEquals(1.5f, playerStats.getMovementSpeed());

    leftStats.setHealth(0);

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    assertEquals(3f, playerStats.getMovementSpeed());
    assertFalse(mistDebuff.isMistDebuffed());

    tick(10f);
    assertFalse(mist.isMistActive());
    assertEquals(1, entered);
    assertEquals(1, exited);
  }

  @Test
  void shouldReleaseOccupancyAndUnregisterOnDisposal() {
    tick(5f);

    mist.dispose();

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    assertEquals(3f, playerStats.getMovementSpeed());
    verify(renderService).unregister(mist);

    tick(10f);
    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
  }

  @Test
  void shouldClearWhenPlayerDies() {
    tick(5f);

    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(0.1f);

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    assertEquals(3f, playerStats.getMovementSpeed());
  }

  @Test
  void repeatedMistExposureDoesNotPermanentlyReduceMovement() {
    for (int exposure = 0; exposure < 3; exposure++) {
      tick(5f);
      assertEquals(1.5f, playerStats.getMovementSpeed());
      assertTrue(mistDebuff.isMistDebuffed());

      tick(3f);
      assertEquals(3f, playerStats.getMovementSpeed());
      assertFalse(mistDebuff.isMistDebuffed());
    }
  }

  @Test
  void shouldWaitForCoordinatorWithoutRestartingCooldown() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    mist.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(leftHead)).thenReturn(false);

    tick(5f);

    assertFalse(mist.isMistActive());
    assertEquals(0, entered);

    when(coordinator.tryStart(leftHead)).thenReturn(true);
    tick(0f);

    assertTrue(mist.isMistActive());
    assertEquals(1, entered);
  }

  @Test
  void shouldHoldAttackUntilMistExpiresThenReleaseDuringCooldown() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    mist.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(leftHead)).thenReturn(true);

    tick(5f);
    tick(2f);

    assertTrue(mist.isMistActive());
    verify(coordinator, never()).finish(leftHead);
    verify(coordinator, times(1)).tryStart(leftHead);

    tick(1f);

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    verify(coordinator).finish(leftHead);

    tick(4f);

    assertFalse(mist.isMistActive());
    verify(coordinator, times(1)).tryStart(leftHead);
  }

  @Test
  void shouldReleaseAttackImmediatelyWhenLeftHeadDies() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    mist.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(leftHead)).thenReturn(true);

    tick(5f);

    assertTrue(mist.isMistActive());
    verify(coordinator, never()).finish(leftHead);

    leftStats.setHealth(0);

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    verify(coordinator).finish(leftHead);

    tick(10f);

    assertFalse(mist.isMistActive());
    verify(coordinator, times(1)).tryStart(leftHead);
  }

  @Test
  void shouldReleaseAttackWhenActiveMistIsDisposed() {
    CerberusAttackCoordinator coordinator = mock(CerberusAttackCoordinator.class);
    mist.setAttackCoordinator(coordinator);
    when(coordinator.tryStart(leftHead)).thenReturn(true);

    tick(5f);

    assertTrue(mist.isMistActive());

    mist.dispose();

    assertFalse(mist.isMistActive());
    assertEquals(1, exited);
    verify(coordinator).finish(leftHead);
    verify(renderService).unregister(mist);
  }
}
