package com.csse3200.game.components.spells.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link StrategyAll}.
 *
 * <p>NOTE: assumes {@code ServiceLocator.registerEntityService} exists and {@code GameExtension}
 * resets {@code ServiceLocator} between tests, matching the conventions used elsewhere in this
 * project's test suite (e.g. the earlier SwordWeaponComponentTest). Adjust the imports/API calls if
 * your project's actual scaffolding differs.
 */
@ExtendWith(GameExtension.class)
class AllEnemiesTargetingStrategyTest {
  private EntityService entityService;
  private StrategyAll strategy;
  private Entity caster;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    strategy = new StrategyAll();
    caster = mock(Entity.class);
    when(caster.getCenterPosition()).thenReturn(new Vector2(0f, 0f));
  }

  private void givenWorldEntities(Entity... entities) {
    Array<Entity> array = new Array<>();
    for (Entity entity : entities) {
      array.add(entity);
    }
    when(entityService.getEntities()).thenReturn(array);
  }

  @Test
  void returnsAllEnemiesInTheWorld() {
    Entity enemy1 = TargetingTestHelper.enemyAt(1f, 1f);
    Entity enemy2 = TargetingTestHelper.enemyAt(2f, 2f);
    givenWorldEntities(caster, enemy1, enemy2);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(2, targets.size);
    assertTrue(targets.contains(enemy1, true));
    assertTrue(targets.contains(enemy2, true));
  }

  @Test
  void excludesTheCasterEvenIfItIsOnTheEnemyLayer() {
    // Caster deliberately given an NPC-layer hitbox to prove exclusion is identity-based, not
    // just a side effect of the caster usually not being on the enemy layer.
    Entity npcCaster = TargetingTestHelper.enemyAt(0f, 0f);
    givenWorldEntities(npcCaster);

    Array<Entity> targets = strategy.selectTargets(npcCaster);

    assertEquals(0, targets.size);
  }

  @Test
  void excludesNonEnemyEntities() {
    Entity player = TargetingTestHelper.nonEnemyAt(1f, 1f);
    Entity wall = TargetingTestHelper.noHitboxAt(2f, 2f);
    givenWorldEntities(caster, player, wall);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }

  @Test
  void returnsEmptyArrayWhenNoEnemiesArePresent() {
    givenWorldEntities(caster);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }
}
