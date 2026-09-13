package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CerberusMistComponentTest {
    private GameTime time;
    private RenderService renderService;
    private Entity player;
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

        player = new Entity().addComponent(new CombatStatsComponent(100, 0));
        player.setPosition(1f, 0f);
        player.getEvents()
                .addListener(CerberusMistComponent.ENTERED, (Entity source) -> entered++);
        player.getEvents()
                .addListener(CerberusMistComponent.EXITED, (Entity source) -> exited++);

        leftStats = new CombatStatsComponent(100, 10);
        mist = new CerberusMistComponent(player);

        Entity leftHead = new Entity().addComponent(leftStats).addComponent(mist);
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
        tick(4f);
        assertFalse(mist.isMistActive());

        tick(1f);
        assertTrue(mist.isMistActive());
        assertEquals(1, entered);

        tick(2f);
        assertTrue(mist.isMistActive());
        assertEquals(1, entered);

        tick(1f);
        assertFalse(mist.isMistActive());
        assertEquals(1, exited);

        tick(4f);
        assertFalse(mist.isMistActive());

        tick(1f);
        assertTrue(mist.isMistActive());
        assertEquals(2, entered);
    }

    @Test
    void shouldKeepMistFixedAndReportExitAndReentry() {
        tick(5f);
        Vector2 originalCentre = mist.getMistCentre();

        player.setPosition(10f, 0f);
        tick(0.1f);

        assertEquals(originalCentre, mist.getMistCentre());
        assertEquals(1, exited);

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

        leftStats.setHealth(0);

        assertFalse(mist.isMistActive());
        assertEquals(1, exited);

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
    }

}
