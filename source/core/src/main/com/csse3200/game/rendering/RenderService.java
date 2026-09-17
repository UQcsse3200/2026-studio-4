package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.SortedIntMap;

/**
 * Globally accessible service for registering renderable components. Any renderable registered with
 * this service has render() called once per frame.
 */
public class RenderService implements Disposable {
  private static final int INITIAL_LAYER_CAPACITY = 4;
  private static final int INITIAL_CAPACITY = 4;
  private static final float SHAKE_ANGULAR_SPEED = 80f;
  private Stage stage;
  private DebugRenderer debugRenderer;
  private final Matrix4 worldProjection = new Matrix4();
  private Object shakeOwner;
  private float shakeDuration;
  private float shakeElapsed;
  private float shakeStrength;

  /** Map from layer to list of renderables, allows us to render each layer in the correct order */
  private final SortedIntMap<Array<Renderable>> renderables =
      new SortedIntMap<>(INITIAL_LAYER_CAPACITY);

  /**
   * Register a new renderable.
   *
   * @param renderable new renderable.
   */
  public void register(Renderable renderable) {
    int layerIndex = renderable.getLayer();
    if (!renderables.containsKey(layerIndex)) {
      renderables.put(layerIndex, new Array<>(INITIAL_CAPACITY));
    }
    Array<Renderable> layer = renderables.get(layerIndex);
    layer.add(renderable);
  }

  /**
   * Unregister a renderable.
   *
   * @param renderable renderable to unregister.
   */
  public void unregister(Renderable renderable) {
    Array<Renderable> layer = renderables.get(renderable.getLayer());
    if (layer != null) {
      layer.removeValue(renderable, true);
    }
  }

  /**
   * Trigger rendering on the given batch. This should be called only from the main renderer.
   *
   * @param batch batch to render to.
   */
  public void render(SpriteBatch batch) {
    for (Array<Renderable> layer : renderables) {
      // Sort into rendering order
      layer.sort();

      for (Renderable renderable : layer) {
        renderable.render(batch);
      }
    }
  }

  /**
   * Start a decaying world shake, replacing any previous pulse. UI and gameplay cameras are never
   * modified. Null owners and nonpositive or nonfinite values are ignored.
   *
   * @param owner identity token used to cancel this pulse during cleanup
   * @param duration pulse duration in seconds
   * @param strength maximum displacement in world units
   */
  public void shake(Object owner, float duration, float strength) {
    if (owner == null
        || !Float.isFinite(duration)
        || !Float.isFinite(strength)
        || duration <= 0f
        || strength <= 0f) {
      return;
    }
    shakeOwner = owner;
    shakeDuration = duration;
    shakeElapsed = 0f;
    shakeStrength = strength;
  }

  /** Cancel the active pulse only when it belongs to this exact owner token. */
  public void clearShake(Object owner) {
    if (shakeOwner == owner) {
      resetShake();
    }
  }

  /**
   * Advance the shake once per rendered frame and return a world-only projection. The supplied
   * camera matrix is never modified, so following and gameplay bounds remain steady.
   *
   * @param base unmodified camera projection
   * @return reusable projection, valid until the next call; callers must not modify it
   */
  public Matrix4 getWorldProjection(Matrix4 base) {
    worldProjection.set(base);
    if (shakeOwner == null) {
      return worldProjection;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float delta = time == null ? 0f : time.getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      shakeElapsed = Math.min(shakeDuration, shakeElapsed + delta);
    }
    if (shakeElapsed >= shakeDuration) {
      resetShake();
      return worldProjection;
    }

    float amplitude = shakeStrength * (1f - shakeElapsed / shakeDuration);
    float angle = shakeElapsed * SHAKE_ANGULAR_SPEED;
    worldProjection.translate(
        MathUtils.cos(angle) * amplitude, MathUtils.sin(angle) * amplitude, 0f);
    return worldProjection;
  }

  private void resetShake() {
    shakeOwner = null;
    shakeDuration = 0f;
    shakeElapsed = 0f;
    shakeStrength = 0f;
  }

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public Stage getStage() {
    return stage;
  }

  public void setDebug(DebugRenderer debugRenderer) {
    this.debugRenderer = debugRenderer;
  }

  public DebugRenderer getDebug() {
    return debugRenderer;
  }

  @Override
  public void dispose() {
    renderables.clear();
    resetShake();
  }
}
