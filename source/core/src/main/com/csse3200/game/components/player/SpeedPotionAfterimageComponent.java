package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Brief, fading copies of the player while a speed potion is active and the player is moving. */
public class SpeedPotionAfterimageComponent extends RenderComponent {
  private static final float SAMPLE_INTERVAL = 0.08f;
  private static final float LIFETIME = 0.4f;
  private static final int MAX_FRAMES = 3;

  private final Array<Snapshot> snapshots = new Array<>(MAX_FRAMES);
  private AnimationRenderComponent animator;
  private ConsumableEffectComponent consumables;
  private final Vector2 lastPosition = new Vector2();
  private float sampleElapsed = SAMPLE_INTERVAL;

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
    consumables = entity.getComponent(ConsumableEffectComponent.class);
    lastPosition.set(entity.getPosition());
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;

    for (int i = snapshots.size - 1; i >= 0; i--) {
      Snapshot snapshot = snapshots.get(i);
      snapshot.remaining -= delta;
      if (snapshot.remaining <= 0f) snapshots.removeIndex(i);
    }

    Vector2 position = entity.getPosition();
    boolean moving = !position.epsilonEquals(lastPosition, 0.001f);
    if (moving && consumables.getRemainingMs(ItemIds.SPEED_POTION) > 0) {
      sampleElapsed += delta;
      TextureRegion frame = animator.getCurrentFrame();
      if (sampleElapsed >= SAMPLE_INTERVAL && frame != null) {
        Vector2 scale = entity.getScale();
        if (snapshots.size == MAX_FRAMES) snapshots.removeIndex(0);
        snapshots.add(
            new Snapshot(
                frame,
                lastPosition.x,
                lastPosition.y + animator.getVerticalOffset(),
                scale.x,
                scale.y));
        sampleElapsed = 0f;
      }
    } else {
      sampleElapsed = SAMPLE_INTERVAL;
    }
    lastPosition.set(position);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (isRepeatPass() || snapshots.isEmpty()) return;
    float originalColour = batch.getPackedColor();
    for (Snapshot snapshot : snapshots) {
      batch.setColor(0.55f, 0.8f, 1f, 0.45f * snapshot.remaining / LIFETIME);
      batch.draw(snapshot.frame, snapshot.x, snapshot.y, snapshot.width, snapshot.height);
    }
    batch.setPackedColor(originalColour);
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.1f;
  }

  @Override
  public void dispose() {
    snapshots.clear();
    super.dispose();
  }

  private static class Snapshot {
    final TextureRegion frame;
    final float x;
    final float y;
    final float width;
    final float height;
    float remaining = LIFETIME;

    Snapshot(TextureRegion frame, float x, float y, float width, float height) {
      this.frame = frame;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
  }
}
