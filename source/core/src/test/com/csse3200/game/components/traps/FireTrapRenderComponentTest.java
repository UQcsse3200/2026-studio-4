package com.csse3200.game.components.traps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FireTrapRenderComponentTest {
  @Test
  void animatesAtOriginalSpeedThenExtinguishesWithoutRearming() {
    ResourceService resources = mock(ResourceService.class);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    Texture start = sheet(resources, FireTrapRenderComponent.START_TEXTURE);
    Texture loop = sheet(resources, FireTrapRenderComponent.LOOP_TEXTURE);
    Texture end = sheet(resources, FireTrapRenderComponent.END_TEXTURE);
    FireTrapRenderComponent visual = new FireTrapRenderComponent();
    Entity trap = new Entity().addComponent(visual);
    trap.setScale(0.5f, 0.5f);
    trap.setPosition(8f, 12f);
    trap.create();
    assertSame(start, visual.currentFrame().getTexture());
    when(time.getDeltaTime()).thenReturn(0.11f);
    visual.update();
    assertEquals(18, visual.currentFrame().getRegionX());
    when(time.getDeltaTime()).thenReturn(0.31f);
    visual.update();
    assertSame(loop, visual.currentFrame().getTexture());
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    verify(batch).draw(visual.currentFrame(), 7.96875f, 12f, 0.5625f, 1.84375f);
    assertTrue(visual.getLayer() < new TextureRenderComponent(start).getLayer());
    assertTrue(visual.getZIndex() > 0f);
    trap.getEvents().trigger("trapTriggered");
    when(time.getDeltaTime()).thenReturn(0.81f);
    visual.update();
    assertSame(end, visual.currentFrame().getTexture());
    trap.getEvents().trigger("trapTriggered");
    when(time.getDeltaTime()).thenReturn(0.41f);
    visual.update();
    assertNull(visual.currentFrame());
    clearInvocations(batch);
    visual.render(batch);
    verifyNoInteractions(batch);
  }

  private Texture sheet(ResourceService resources, String path) {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(72);
    when(texture.getHeight()).thenReturn(59);
    when(resources.getAsset(path, Texture.class)).thenReturn(texture);
    return texture;
  }
}
