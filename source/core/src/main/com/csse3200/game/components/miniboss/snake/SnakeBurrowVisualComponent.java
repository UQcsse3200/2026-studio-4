package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Draws green burrowing dust and a fixed warning circle without external effect textures. */
public class SnakeBurrowVisualComponent extends RenderComponent {
  private static final int MAX_PARTICLES = 128;
  private static final float PIXEL_SIZE = 0.035f;
  private final List<Particle> particles = new ArrayList<>();
  private final Vector2 warningCentre = new Vector2();
  private Texture pixel;
  private Texture circle;
  private float warningRadius;
  private float warningAge;
  private boolean stopped;
  private boolean registered;

  private static class Particle {
    private final Vector2 position;
    private final Vector2 velocity;
    private final float lifetime;
    private final float size;
    private float age;

    private Particle(Vector2 centre, float angle, float spread) {
      position = centre.cpy();
      velocity = new Vector2(MathUtils.cos(angle), MathUtils.sin(angle)).scl(spread);
      lifetime = MathUtils.random(0.4f, 0.7f);
      size = MathUtils.random(2, 4) * PIXEL_SIZE;
    }
  }

  @Override
  public void create() {
    entity.getEvents().addListener("entityDied", this::stop);
    super.create();
    registered = true;
  }

  /** Adds the initial dust cloud; safe to call before render registration. */
  public void startBurrow(Vector2 centre) {
    emit(centre, 24, 0.65f);
  }

  /** Adds the larger cloud when the Snake comes back above ground. */
  public void startEmergence(Vector2 centre) {
    emit(centre, 32, 1.2f);
  }

  /** Leaves a brief green mark at a position travelled while underground. */
  public void addTrail(Vector2 centre) {
    emit(centre, 6, 0.3f);
  }

  private void emit(Vector2 centre, int count, float spread) {
    if (stopped) {
      return;
    }
    for (int i = 0; i < count; i++) {
      if (particles.size() >= MAX_PARTICLES) {
        particles.removeFirst();
      }
      float angle = MathUtils.PI2 * i / count + MathUtils.random(0.3f);
      particles.add(new Particle(centre, angle, spread * MathUtils.random(0.5f, 1f)));
    }
  }

  /** Locks the warning to a copy of the attack centre and the actual damage radius. */
  public void showWarning(Vector2 centre, float radius) {
    if (stopped || !Float.isFinite(radius) || radius <= 0f) {
      return;
    }
    warningCentre.set(centre);
    warningRadius = radius;
    warningAge = 0f;
  }

  /** Removes the warning without discarding dust still fading out. */
  public void hideWarning() {
    warningRadius = 0f;
  }

  public boolean isWarningVisible() {
    return warningRadius > 0f;
  }

  public Vector2 getWarningCentre() {
    return warningCentre.cpy();
  }

  public float getWarningRadius() {
    return warningRadius;
  }

  public int getParticleCount() {
    return particles.size();
  }

  /** Immediately removes all effects, for attack cancellation or death. */
  public void clear() {
    hideWarning();
    particles.clear();
  }

  private void stop() {
    stopped = true;
    clear();
  }

  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time != null) {
      update(time.getDeltaTime());
    }
  }

  /** Ages effects in game time; rendering never advances the animation. */
  public void update(float delta) {
    if (stopped || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    if (isWarningVisible()) {
      warningAge += delta;
    }
    for (Particle particle : particles) {
      particle.age += delta;
      particle.position.mulAdd(particle.velocity, delta);
    }
    particles.removeIf(particle -> particle.age >= particle.lifetime);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (stopped) {
      return;
    }
    float original = batch.getPackedColor();
    try {
      drawParticles(batch);
      if (isWarningVisible()) {
        drawWarning(batch);
      }
    } finally {
      batch.setPackedColor(original);
    }
  }

  private void drawParticles(SpriteBatch batch) {
    if (particles.isEmpty()) {
      return;
    }
    ensurePixel();
    for (Particle particle : particles) {
      float remaining = 1f - particle.age / particle.lifetime;
      batch.setColor(0.35f + remaining * 0.25f, 0.85f, 0.2f, remaining * 0.8f);
      float x = Math.round(particle.position.x / PIXEL_SIZE) * PIXEL_SIZE;
      float y = Math.round(particle.position.y / PIXEL_SIZE) * PIXEL_SIZE;
      batch.draw(
          pixel, x - particle.size / 2f, y - particle.size / 2f, particle.size, particle.size);
    }
  }

  private void ensurePixel() {
    if (pixel != null) {
      return;
    }
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    try {
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      pixel = new Texture(pixmap);
    } finally {
      pixmap.dispose();
    }
  }

  private void drawWarning(SpriteBatch batch) {
    if (circle == null) {
      circle = RadialTextureFactory.create(64, distance -> distance >= 0.88f ? 1f : 0.18f);
      circle.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }
    float pulse = 0.8f + 0.2f * MathUtils.sin(warningAge * 15f);
    batch.setColor(1f, 0.28f, 0.08f, pulse);
    batch.draw(
        circle,
        warningCentre.x - warningRadius,
        warningCentre.y - warningRadius,
        warningRadius * 2f,
        warningRadius * 2f);
  }

  @Override
  public float getZIndex() {
    return Float.NEGATIVE_INFINITY;
  }

  @Override
  public void dispose() {
    stop();
    if (pixel != null) {
      pixel.dispose();
      pixel = null;
    }
    if (circle != null) {
      circle.dispose();
      circle = null;
    }
    if (registered) {
      super.dispose();
      registered = false;
    }
  }
}
