package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class VenomSpitAttackTaskTest {

    private GameTime gameTime;
    private EntityService entityService;
    private CombatStatsComponent ownerStats;
    private Entity owner;
    private Entity target;
    private TaskRunner taskRunner;

    @BeforeEach
    void setUp() {
        gameTime = mock(GameTime.class);
        ServiceLocator.registerTimeSource(gameTime);
        ServiceLocator.registerPhysicsService(new PhysicsService());
        entityService = spy(new EntityService());
        ServiceLocator.registerEntityService(entityService);

        target = new Entity();
        target.setPosition(new Vector2(3f, 0f));

        owner = new Entity();
        owner.setPosition(new Vector2(0f, 0f));
        ownerStats = new CombatStatsComponent(100, 10);
        owner.addComponent(ownerStats);
        owner.create();

        taskRunner = mock(TaskRunner.class);
        when(taskRunner.getEntity()).thenReturn(owner);
    }

    @Test
    void shouldNotTriggerWhenHealthIsAboveThreshold() {
        when(gameTime.getTime()).thenReturn(0L);
        VenomSpitAttackTask task = new VenomSpitAttackTask(target);
        task.create(taskRunner);

        assertEquals(-1, task.getPriority());
    }

    @Test
    void shouldNotTriggerWhenTargetIsFar() {
        when(gameTime.getTime()).thenReturn(0L);
        ownerStats.setHealth(40);
        VenomSpitAttackTask task = new VenomSpitAttackTask(target);
        task.create(taskRunner);

        target.setPosition(new Vector2(20f, 0f));

        assertEquals(-1, task.getPriority());
    }

    @Test
    void shouldTriggerWhenBelowHealthThresholdAndTargetIsClose() {
        when(gameTime.getTime()).thenReturn(0L);
        ownerStats.setHealth(40);
        VenomSpitAttackTask task = new VenomSpitAttackTask(target);
        task.create(taskRunner);

        assertEquals(15, task.getPriority());
    }

    @Test
    void shouldSpawnVenomPoolAfterTelegraphDuration() {
        when(gameTime.getTime()).thenReturn(0L);
        ownerStats.setHealth(40);
        VenomSpitAttackTask task = new VenomSpitAttackTask(target);
        task.create(taskRunner);
        task.start();

        when(gameTime.getTime()).thenReturn(700L);
        task.update();

        verify(entityService, times(1)).register(any(Entity.class));
    }

    @Test
    void shouldRespectCooldownAfterSpitting() {
        when(gameTime.getTime()).thenReturn(0L);
        ownerStats.setHealth(40);
        VenomSpitAttackTask task = new VenomSpitAttackTask(target);
        task.create(taskRunner);
        task.start();

        when(gameTime.getTime()).thenReturn(700L);
        task.update();

        task.stop();

        when(gameTime.getTime()).thenReturn(1000L);
        assertEquals(-1, task.getPriority());

        when(gameTime.getTime()).thenReturn(5000L);
        assertEquals(15, task.getPriority());
    }
}
