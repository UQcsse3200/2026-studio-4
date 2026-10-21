package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeBurrowVisualComponentTest {
  private SnakeBurrowVisualComponent visual;
  private Entity snake;
  private RenderService renderer;
  private World world;

  @BeforeEach
  void setUp() {
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    visual = new SnakeBurrowVisualComponent();
    snake = new Entity().addComponent(visual);
  }

  @AfterEach
  void disposeEffectsAndWorld() {
    visual.dispose();
    if (world != null) {
      world.dispose();
    }
  }

  @Test
  void allDustEffectsKeepTheirRenderedEdgesOutsideNearbyWalls() {
    createWorld();
    List<Rectangle> walls =
        List.of(
            new Rectangle(0f, 3.65f, 6f, 0.1f),
            new Rectangle(0f, 2.25f, 6f, 0.1f),
            new Rectangle(2.25f, 0f, 0.1f, 6f),
            new Rectangle(3.65f, 0f, 0.1f, 6f));
    walls.forEach(this::createWall);
    List<Consumer<Vector2>> effects =
        List.of(visual::startBurrow, visual::addTrail, visual::startEmergence);
    for (Consumer<Vector2> effect : effects) {
      visual.clear();
      effect.accept(new Vector2(3f, 3f));
      for (int frame = 0; frame < 8; frame++) {
        List<Rectangle> particles = drawnParticles();
        assertFalse(particles.isEmpty());
        for (Rectangle particle : particles) {
          assertTrue(new Rectangle(2.35f, 2.35f, 1.3f, 1.3f).contains(particle));
          assertTrue(walls.stream().noneMatch(wall -> wall.overlaps(particle)));
        }
        visual.update(0.05f);
      }
    }
  }

  @Test
  void aSlowFrameCannotSendDustThroughAThinWall() {
    createWorld();
    visual.startEmergence(new Vector2(3f, 3f));
    visual.update(0.35f);
    assertTrue(drawnParticles().stream().anyMatch(particle -> particle.x + particle.width > 3.4f));
    visual.clear();
    createWall(new Rectangle(3.4f, 0f, 0.01f, 6f));
    visual.startEmergence(new Vector2(3f, 3f));
    visual.update(0.35f);

    List<Rectangle> particles = drawnParticles();
    assertEquals(44, particles.size());
    assertTrue(particles.stream().allMatch(particle -> particle.x + particle.width < 3.4f));
    visual.update(1f);
    assertEquals(0, visual.getParticleCount());
  }

  @Test
  void dustStaysInsideTheRoomAndDoesNotStartInsideAWall() {
    createWorld();
    Rectangle room = new Rectangle(0f, 0f, 2f, 2f);
    visual.setDustArena(new SnakeBurrowArena(room));
    visual.startEmergence(new Vector2(0.3f, 0.3f));
    visual.update(0.35f);
    List<Rectangle> particles = drawnParticles();
    assertFalse(particles.isEmpty());
    assertTrue(particles.stream().allMatch(room::contains));

    visual.clear();
    createWall(room);
    visual.startBurrow(new Vector2(1f, 1f));
    assertEquals(0, visual.getParticleCount());
  }

  private List<Rectangle> drawnParticles() {
    List<Rectangle> rectangles = new ArrayList<>();
    SpriteBatch batch = mock(SpriteBatch.class);
    doAnswer(
            invocation -> {
              rectangles.add(
                  new Rectangle(
                      invocation.getArgument(1),
                      invocation.getArgument(2),
                      invocation.getArgument(3),
                      invocation.getArgument(4)));
              return null;
            })
        .when(batch)
        .draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    visual.draw(batch);
    return rectangles;
  }

  private void createWorld() {
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
  }

  private void createWall(Rectangle area) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(area.x + area.width / 2f, area.y + area.height / 2f);
    Body wall = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    try {
      shape.setAsBox(area.width / 2f, area.height / 2f);
      wall.createFixture(shape, 0f);
    } finally {
      shape.dispose();
    }
  }

  @Test
  void shouldAcceptEffectsBeforeRenderRegistration() {
    visual.startBurrow(new Vector2());
    visual.addTrail(new Vector2(1f, 0f));
    visual.showWarning(new Vector2(2f, 0f), 1f);
    assertTrue(visual.getParticleCount() > 0);
    assertTrue(visual.isWarningVisible());
    verifyNoInteractions(renderer);
  }

  @Test
  void shouldKeepWarningAtTheLockedCentreAndDamageRadius() {
    Vector2 attackCentre = new Vector2(3f, 4f);
    visual.showWarning(attackCentre, 1.25f);
    attackCentre.setZero();
    visual.getWarningCentre().setZero();
    visual.update(0.5f);
    assertEquals(new Vector2(3f, 4f), visual.getWarningCentre());
    assertEquals(1.25f, visual.getWarningRadius());
    visual.hideWarning();
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldExpireDustUsingTheRegisteredGameClock() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    visual.startEmergence(new Vector2());
    when(time.getDeltaTime()).thenReturn(0f);
    visual.update();
    assertTrue(visual.getParticleCount() > 0);
    when(time.getDeltaTime()).thenReturn(1f);
    visual.update();
    assertEquals(0, visual.getParticleCount());
  }

  @Test
  void shouldBoundRepeatedTrailEmissions() {
    for (int i = 0; i < 100; i++) {
      visual.addTrail(new Vector2((float) i, 0f));
    }
    assertTrue(visual.getParticleCount() <= 128);
    visual.update(1f);
    assertEquals(0, visual.getParticleCount());
  }

  @Test
  void shouldIgnoreInvalidTimeAndClearActiveEffects() {
    visual.startBurrow(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    int initialCount = visual.getParticleCount();
    visual.update(Float.NaN);
    visual.update(-1f);
    visual.update(Float.POSITIVE_INFINITY);
    assertEquals(initialCount, visual.getParticleCount());
    visual.clear();
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldClearAndStopEmittingOnDeath() {
    visual.create();
    visual.startBurrow(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    snake.getEvents().trigger("entityDied");
    visual.startEmergence(new Vector2());
    visual.addTrail(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldClearAndUnregisterWhenDisposedBeforeFirstDraw() {
    visual.create();
    visual.startEmergence(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    visual.dispose();
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
    verify(renderer).register(visual);
    verify(renderer).unregister(visual);
  }
}
