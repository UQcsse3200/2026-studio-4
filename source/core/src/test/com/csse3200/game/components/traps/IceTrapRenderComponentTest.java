package com.csse3200.game.components.traps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class IceTrapRenderComponentTest {
  @Test
  void staysArmedThenGrowsHoldsAndMeltsWithoutRestarting() {
    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(160);
    when(texture.getHeight()).thenReturn(128);
    when(resources.getAsset(IceTrapRenderComponent.TEXTURE, Texture.class)).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    IceTrapRenderComponent visual = new IceTrapRenderComponent();
    Entity trap = new Entity().addComponent(visual);
    trap.setScale(0.5f, 0.5f);
    trap.setPosition(6f, 6f);
    trap.create();
    when(time.getDeltaTime()).thenReturn(10f);
    visual.update();
    assertEquals(0, visual.currentFrame().getRegionX());
    assertEquals(0, visual.currentFrame().getRegionY());
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    verify(batch).draw(visual.currentFrame(), 5.75f, 6f, 1f, 1f);
    assertEquals(0, visual.getLayer());
    assertEquals(1f, visual.getZIndex());
    trap.getEvents().trigger("trapTriggered");
    when(time.getDeltaTime()).thenReturn(0.51f);
    visual.update();
    assertEquals(0, visual.currentFrame().getRegionX());
    assertEquals(32, visual.currentFrame().getRegionY());
    when(time.getDeltaTime()).thenReturn(0.9f);
    visual.update();
    assertEquals(64, visual.currentFrame().getRegionX());
    assertEquals(64, visual.currentFrame().getRegionY());
    when(time.getDeltaTime()).thenReturn(0.4f);
    visual.update();
    assertEquals(96, visual.currentFrame().getRegionX());
    assertEquals(64, visual.currentFrame().getRegionY());
    trap.getEvents().trigger("trapTriggered");
    when(time.getDeltaTime()).thenReturn(0.8f);
    visual.update();
    assertNull(visual.currentFrame());
    clearInvocations(batch);
    visual.render(batch);
    verifyNoInteractions(batch);
  }
}
