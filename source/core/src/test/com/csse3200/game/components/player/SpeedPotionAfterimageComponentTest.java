package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SpeedPotionAfterimageComponentTest {
  private final AtomicLong now = new AtomicLong();
  private Entity player;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private SpeedPotionAfterimageComponent afterimages;
  private AtlasRegion frame;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now.get());
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(new RenderService());

    TextureAtlas atlas = mock(TextureAtlas.class);
    frame = mock(AtlasRegion.class);
    when(atlas.findRegions("walk")).thenReturn(new Array<>(new AtlasRegion[] {frame}));
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("walk", 0.1f);
    animator.startAnimation("walk");

    inventory = new InventoryComponent(0);
    consumables = new ConsumableEffectComponent();
    afterimages = new SpeedPotionAfterimageComponent();
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(animator)
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(effects)
            .addComponent(consumables)
            .addComponent(afterimages);
    effects.create();
    consumables.create();
    afterimages.create();
  }

  @Test
  void movingWithSpeedPotionLeavesAFrameBehindThenFades() {
    SpriteBatch batch = mock(SpriteBatch.class);
    inventory.addConsumable(ItemType.SPEED_POTION);
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));

    player.setPosition(1f, 0f);
    afterimages.update();
    afterimages.render(batch);
    verify(batch).draw(frame, 0f, 0f, 1f, 1f);

    now.set(8000L);
    player.setPosition(2f, 0f);
    for (int i = 0; i < 5; i++) afterimages.update();
    clearInvocations(batch);
    afterimages.render(batch);
    verify(batch, never()).draw(frame, 0f, 0f, 1f, 1f);
    verify(batch, never()).draw(frame, 1f, 0f, 1f, 1f);
  }

  @Test
  void standingStillOrMovingWithoutPotionLeavesNoTrail() {
    SpriteBatch batch = mock(SpriteBatch.class);
    inventory.addConsumable(ItemType.SPEED_POTION);
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));
    afterimages.update();
    afterimages.render(batch);
    verify(batch, never()).draw(frame, 0f, 0f, 1f, 1f);

    now.set(8000L);
    player.setPosition(1f, 0f);
    afterimages.update();
    afterimages.render(batch);
    verify(batch, never()).draw(frame, 0f, 0f, 1f, 1f);
  }

  @Test
  void trailKeepsAtMostThreeRecentFrames() {
    SpriteBatch batch = mock(SpriteBatch.class);
    inventory.addConsumable(ItemType.SPEED_POTION);
    assertTrue(consumables.tryUse(ItemType.SPEED_POTION));
    for (int i = 1; i <= 4; i++) {
      player.setPosition(i, 0f);
      afterimages.update();
    }

    afterimages.render(batch);
    verify(batch, times(3)).draw(eq(frame), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }
}
