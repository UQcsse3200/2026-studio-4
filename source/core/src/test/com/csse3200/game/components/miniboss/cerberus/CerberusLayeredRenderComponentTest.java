package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Action;
import com.csse3200.game.components.miniboss.cerberus.CerberusLayeredRenderComponent.Part;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

@ExtendWith(GameExtension.class)
class CerberusLayeredRenderComponentTest {
  private TextureAtlas atlas;
  private SpriteBatch batch;
  private AtlasRegion body;
  private AtlasRegion left;
  private AtlasRegion middle;
  private AtlasRegion right;
  private CerberusLayeredRenderComponent renderer;

  private Entity createHead(int health) {
    return new Entity().addComponent(new CombatStatsComponent(health, 10));
  }

  private void stubAnimations(String prefix, AtlasRegion idleFrame) {
    String[] actions = {"idle", "move", "lunge", "cast"};

    for (String action : actions) {
      for (int index = 0; index < 4; index++) {
        AtlasRegion frame =
            "idle".equals(action) && index == 0 ? idleFrame : mock(AtlasRegion.class);

        when(atlas.findRegion(prefix + "_" + action, index)).thenReturn(frame);
      }
    }
  }

  @BeforeEach
  void setUp() {
    atlas = mock(TextureAtlas.class);
    batch = mock(SpriteBatch.class);

    body = mock(AtlasRegion.class);
    left = mock(AtlasRegion.class);
    middle = mock(AtlasRegion.class);
    right = mock(AtlasRegion.class);

    stubAnimations("body", body);
    stubAnimations("left", left);
    stubAnimations("middle", middle);
    stubAnimations("right", right);

    renderer = new CerberusLayeredRenderComponent(atlas);

    Entity entity = new Entity().addComponent(renderer);
    entity.setPosition(2f, 3f);
    entity.setScale(4f, 4f);
  }

  @Test
  void shouldDrawAllLayersInOrderWithMatchingBounds() {
    renderer.draw(batch);

    InOrder order = inOrder(batch);
    order.verify(batch).draw(body, 2f, 3f, 4f, 4f);
    order.verify(batch).draw(left, 2f, 3f, 4f, 4f);
    order.verify(batch).draw(middle, 2f, 3f, 4f, 4f);
    order.verify(batch).draw(right, 2f, 3f, 4f, 4f);
    order.verifyNoMoreInteractions();
  }

  @Test
  void shouldKeepBodyAndSideHeadsWhenMiddleHeadIsHidden() {
    renderer.setPartVisible(Part.MIDDLE, false);

    renderer.draw(batch);

    assertFalse(renderer.isPartVisible(Part.MIDDLE));
    assertTrue(renderer.isPartVisible(Part.BODY));
    verify(batch).draw(body, 2f, 3f, 4f, 4f);
    verify(batch).draw(left, 2f, 3f, 4f, 4f);
    verify(batch).draw(right, 2f, 3f, 4f, 4f);
    verify(batch, never()).draw(middle, 2f, 3f, 4f, 4f);
  }

  @Test
  void shouldAllowHeadToBeShownAgain() {
    renderer.setPartVisible(Part.LEFT, false);
    renderer.setPartVisible(Part.LEFT, true);

    renderer.draw(batch);

    assertTrue(renderer.isPartVisible(Part.LEFT));
    verify(batch).draw(left, 2f, 3f, 4f, 4f);
  }

  @Test
  void shouldRejectAtlasWithMissingLayer() {
    when(atlas.findRegion("right_idle", 0)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> new CerberusLayeredRenderComponent(atlas));
  }

  @Test
  void shouldHideOnlyTheDeadHead() {
    Entity leftHead = createHead(100);
    Entity middleHead = createHead(200);
    Entity rightHead = createHead(100);
    renderer.bindHeads(leftHead, middleHead, rightHead);

    leftHead.getComponent(CombatStatsComponent.class).setHealth(0);
    renderer.update(0f);

    assertFalse(renderer.isPartVisible(Part.LEFT));
    assertTrue(renderer.isPartVisible(Part.MIDDLE));
    assertTrue(renderer.isPartVisible(Part.RIGHT));
    assertTrue(renderer.isPartVisible(Part.BODY));
  }

  @Test
  void shouldKeepBodyAndSideHeadsAfterMiddleHeadDies() {
    Entity leftHead = createHead(100);
    Entity middleHead = createHead(200);
    Entity rightHead = createHead(100);
    renderer.bindHeads(leftHead, middleHead, rightHead);

    middleHead.getComponent(CombatStatsComponent.class).setHealth(0);
    renderer.update(0f);

    assertFalse(renderer.isPartVisible(Part.MIDDLE));
    assertTrue(renderer.isPartVisible(Part.LEFT));
    assertTrue(renderer.isPartVisible(Part.RIGHT));
    assertTrue(renderer.isPartVisible(Part.BODY));
  }

  @Test
  void shouldKeepBodyUntilEntityDisposalAfterAllHeadsDie() {
    Entity leftHead = createHead(100);
    Entity middleHead = createHead(200);
    Entity rightHead = createHead(100);
    renderer.bindHeads(leftHead, middleHead, rightHead);

    leftHead.getComponent(CombatStatsComponent.class).setHealth(0);
    middleHead.getComponent(CombatStatsComponent.class).setHealth(0);
    rightHead.getComponent(CombatStatsComponent.class).setHealth(0);
    renderer.update(0f);

    assertFalse(renderer.isPartVisible(Part.LEFT));
    assertFalse(renderer.isPartVisible(Part.MIDDLE));
    assertFalse(renderer.isPartVisible(Part.RIGHT));
    assertTrue(renderer.isPartVisible(Part.BODY));
  }

  @Test
  void shouldImmediatelyHideHeadThatDiedBeforeBinding() {
    Entity leftHead = createHead(100);
    Entity middleHead = createHead(200);
    Entity rightHead = createHead(100);
    rightHead.getComponent(CombatStatsComponent.class).setHealth(0);

    renderer.bindHeads(leftHead, middleHead, rightHead);

    assertFalse(renderer.isPartVisible(Part.RIGHT));
    assertTrue(renderer.isPartVisible(Part.BODY));
  }

  @Test
  void shouldAdvanceIdleFrames() {
    renderer.update(0.16f);

    assertSame(atlas.findRegion("body_idle", 1), renderer.getCurrentFrame(Part.BODY));
  }

  @Test
  void shouldLoopIdleAnimation() {
    renderer.update(0.61f);

    assertEquals(Action.IDLE, renderer.getAction(Part.BODY));
    assertSame(body, renderer.getCurrentFrame(Part.BODY));
  }

  @Test
  void shouldAnimateHeadsIndependently() {
    renderer.play(Part.LEFT, Action.CAST);
    renderer.play(Part.MIDDLE, Action.LUNGE);
    renderer.update(0.16f);

    assertSame(atlas.findRegion("left_cast", 1), renderer.getCurrentFrame(Part.LEFT));
    assertSame(atlas.findRegion("middle_lunge", 1), renderer.getCurrentFrame(Part.MIDDLE));
    assertEquals(Action.IDLE, renderer.getAction(Part.RIGHT));
    assertEquals(Action.IDLE, renderer.getAction(Part.BODY));
  }

  @Test
  void shouldReturnToIdleAfterAttack() {
    renderer.play(Part.MIDDLE, Action.LUNGE);
    renderer.update(0.61f);

    assertEquals(Action.IDLE, renderer.getAction(Part.MIDDLE));
    assertSame(middle, renderer.getCurrentFrame(Part.MIDDLE));
  }

  @Test
  void shouldIgnoreInvalidAnimationTime() {
    renderer.update(Float.NaN);
    renderer.update(Float.POSITIVE_INFINITY);
    renderer.update(-1f);

    assertSame(body, renderer.getCurrentFrame(Part.BODY));
  }

  @Test
  void shouldNotAdvanceAnimationWhenDrawingTwice() {
    renderer.draw(batch);
    renderer.draw(batch);

    assertSame(body, renderer.getCurrentFrame(Part.BODY));
  }
}
