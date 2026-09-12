package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class RenderComponentTest {
  @Mock RenderService service;

  @Test
  void shouldMultiplyExistingAlphaForInvisibilityAndRestoreMutableBatchColor() {
    PlayerAbilitiesComponent abilities = mock(PlayerAbilitiesComponent.class);
    when(abilities.isInvisible()).thenReturn(true);
    RecordingRender component = new RecordingRender();
    new Entity().addComponent(abilities).addComponent(component);
    Color original = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    SpriteBatch batch = mutableColorBatch(original);

    component.render(batch);
    assertEquals(new Color(0.8f, 0.6f, 0.4f, 0.5f * 0.35f), component.drawColor);
    assertEquals(original, batch.getColor());
    component.render(batch);
    assertEquals(new Color(0.8f, 0.6f, 0.4f, 0.5f * 0.35f), component.drawColor);
    assertEquals(original, batch.getColor());
    assertEquals(2, component.drawCalls);
  }

  @Test
  void shouldTintLastStandWithoutChangingAlphaAndRestoreColorWhenDrawThrows() {
    PlayerAbilitiesComponent abilities = mock(PlayerAbilitiesComponent.class);
    when(abilities.isLastStandActive()).thenReturn(true);
    RecordingRender component = new RecordingRender();
    new Entity().addComponent(abilities).addComponent(component);
    Color original = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    SpriteBatch batch = mutableColorBatch(original);
    RuntimeException failure = new IllegalStateException("draw failed");
    component.failure = failure;

    assertSame(failure, assertThrows(IllegalStateException.class, () -> component.render(batch)));
    assertEquals(new Color(0.8f, 0.6f * 0.35f, 0.4f * 0.35f, 0.5f), component.drawColor);
    assertEquals(original, batch.getColor());
    assertEquals(1, component.drawCalls);
  }

  @Test
  void shouldComposeLiveWeaponSourceEffectsAndExpireWithoutAnUpdateFrame() {
    GameTime time = mock(GameTime.class);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    Entity player = new Entity().addComponent(combat).addComponent(abilities);
    player.create();
    abilities.enableLastStand();
    RecordingRender weaponRender = new RecordingRender();
    new Entity().addComponent(weaponRender);
    weaponRender.setVisualSource(player);
    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertTrue(abilities.tryInvisibility());
    Color original = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    SpriteBatch batch = mutableColorBatch(original);

    weaponRender.render(batch);
    assertEquals(new Color(0.8f, 0.6f * 0.35f, 0.4f * 0.35f, 0.5f * 0.35f), weaponRender.drawColor);
    assertEquals(original, batch.getColor());
    weaponRender.failure = new IllegalStateException("weapon draw failed");
    assertThrows(IllegalStateException.class, () -> weaponRender.render(batch));
    assertEquals(original, batch.getColor());
    weaponRender.failure = null;

    when(time.getTime()).thenReturn(PlayerAbilitiesComponent.LAST_STAND_DURATION_MS);
    weaponRender.render(batch);
    assertEquals(new Color(0.8f, 0.6f, 0.4f, 0.5f * 0.35f), weaponRender.drawColor);
    assertEquals(original, batch.getColor());
    when(time.getTime()).thenReturn(PlayerAbilitiesComponent.INVISIBILITY_DURATION_MS);
    weaponRender.render(batch);
    assertEquals(original, weaponRender.drawColor);
    assertEquals(original, batch.getColor());
  }

  @Test
  void shouldUseExplicitVisualSourceInsteadOfOwnAbilitiesAndAllowResettingIt() {
    PlayerAbilitiesComponent ownAbilities = mock(PlayerAbilitiesComponent.class);
    when(ownAbilities.isInvisible()).thenReturn(true);
    RecordingRender component = new RecordingRender();
    new Entity().addComponent(ownAbilities).addComponent(component);
    component.setVisualSource(new Entity());
    Color original = new Color(0.8f, 0.6f, 0.4f, 0.5f);
    SpriteBatch batch = mutableColorBatch(original);

    component.render(batch);
    assertEquals(original, component.drawColor);
    verify(batch, never()).setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    component.setVisualSource(null);
    component.render(batch);
    assertEquals(new Color(0.8f, 0.6f, 0.4f, 0.5f * 0.35f), component.drawColor);
    assertEquals(original, batch.getColor());
  }

  @Test
  void shouldDrawWithoutTouchingBatchColorWhenAbilitiesAreInactive() {
    PlayerAbilitiesComponent abilities = mock(PlayerAbilitiesComponent.class);
    RenderComponent component = spy(RenderComponent.class);
    new Entity().addComponent(abilities).addComponent(component);
    SpriteBatch batch = mock(SpriteBatch.class);

    component.render(batch);

    verify(component).draw(batch);
    verify(batch, never()).getColor();
    verify(batch, never()).setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  private static SpriteBatch mutableColorBatch(Color initial) {
    SpriteBatch batch = mock(SpriteBatch.class);
    Color color = new Color(initial);
    when(batch.getColor()).thenReturn(color);
    // Match SpriteBatch's aliasing: setColor mutates the same object returned by getColor.
    doAnswer(
            invocation -> {
              color.set(
                  invocation.getArgument(0),
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    return batch;
  }

  private static class RecordingRender extends RenderComponent {
    Color drawColor;
    int drawCalls;
    RuntimeException failure;

    @Override
    protected void draw(SpriteBatch batch) {
      drawCalls++;
      drawColor = new Color(batch.getColor());
      if (failure != null) {
        throw failure;
      }
    }
  }

  @Test
  void shouldRegisterSelf() {
    ServiceLocator.registerRenderService(service);
    RenderComponent component = spy(RenderComponent.class);
    component.create();
    verify(service).register(component);
  }

  @Test
  void shouldUnregisterOnDispose() {
    ServiceLocator.registerRenderService(service);
    RenderComponent component = spy(RenderComponent.class);
    component.create();
    component.dispose();
    verify(service).unregister(component);
  }

  @Test
  void shouldDrawOnRender() {
    RenderComponent component = spy(RenderComponent.class);
    component.render(null);
    verify(component).draw(any());
  }

  @Test
  void shouldGiveCorrectRenderOrder() {
    RenderComponent component1 = spy(RenderComponent.class);
    RenderComponent component2 = spy(RenderComponent.class);
    assertEquals(component1.getLayer(), component2.getLayer());

    Entity entity1 = new Entity();
    Entity entity2 = new Entity();
    component1.setEntity(entity1);
    component2.setEntity(entity2);

    entity1.setPosition(0f, 1f);
    entity2.setPosition(0f, 2f);
    assertTrue(component1.getZIndex() > component2.getZIndex());

    entity2.setPosition(5f, -3f);
    assertTrue(component1.getZIndex() < component2.getZIndex());
  }
}
