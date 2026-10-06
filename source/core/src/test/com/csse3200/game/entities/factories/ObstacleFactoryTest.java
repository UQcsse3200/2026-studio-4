package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ObstacleFactoryTest {
  private static final float EPS = 0.0001f;

  @BeforeEach
  void setUp() {
    // Give the mock real dimensions. An unstubbed mock returns 0 width/height, which makes
    // scaleEntity() divide 0/0 and every scale/collider in these tests NaN. Large enough that
    // TileSheet can index any tile on the sheet.
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(512);
    when(texture.getHeight()).thenReturn(512);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerPhysicsService(new PhysicsService()); // PhysicsComponent() needs this
    ServiceLocator.registerRenderService(
        new RenderService()); // entity.create() registers renderers
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  /** Builds the entity's fixture; ColliderComponent.getFixture() is null until create(). */
  private static Entity created(Entity entity) {
    entity.create();
    return entity;
  }

  private static PolygonShape shapeOf(Entity entity) {
    return (PolygonShape) entity.getComponent(ColliderComponent.class).getFixture().getShape();
  }

  private static void assertStaticObstacle(Entity entity) {
    assertNotNull(entity.getComponent(PhysicsComponent.class));
    ColliderComponent collider = entity.getComponent(ColliderComponent.class);
    assertNotNull(collider);
    assertEquals(
        BodyType.StaticBody, entity.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(PhysicsLayer.OBSTACLE, collider.getLayer());
  }

  // rock & barrel
  @Test
  void rockIsRenderedStaticObstacleWithBoxCollider() {
    Entity rock = created(ObstacleFactory.createRock());
    assertNotNull(rock.getComponent(TextureRenderComponent.class));
    assertStaticObstacle(rock);
    assertEquals(4, shapeOf(rock).getVertexCount());
  }

  @Test
  void barrelIsRenderedStaticObstacleWithBoxCollider() {
    Entity barrel = created(ObstacleFactory.createBarrel());
    assertNotNull(barrel.getComponent(TextureRenderComponent.class));
    assertStaticObstacle(barrel);
    assertEquals(4, shapeOf(barrel).getVertexCount());
  }

  // tile (bevelled collider)

  @Test
  void tileIsStaticObstacleWithEightVertexCollider() {
    Entity tile = created(ObstacleFactory.createTile());
    assertStaticObstacle(tile);
    assertEquals(8, shapeOf(tile).getVertexCount());
  }

  @Test
  void tileColliderIsHalfTileAndBottomAligned() {
    Entity tile = created(ObstacleFactory.createTile());
    float[] b = bounds(shapeOf(tile));
    Vector2 scale = tile.getScale();

    assertEquals(scale.x * 0.5f, b[2] - b[0], EPS, "width");
    assertEquals(scale.y * 0.5f, b[3] - b[1], EPS, "height");
    assertEquals(0f, b[1], EPS, "bottom");
    // Horizontally centred on the entity.
    assertEquals(scale.x / 2f, (b[0] + b[2]) / 2f, EPS, "centre x");
  }

  @Test
  void tileColliderHasNoSquareCorners() {
    // The whole point of the bevel: no vertex may sit on a corner of the bounding box,
    // otherwise the player snags on it again.
    PolygonShape shape = shapeOf(created(ObstacleFactory.createTile()));
    float[] b = bounds(shape);
    Vector2 v = new Vector2();
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, v);
      boolean onVerticalEdge = near(v.x, b[0]) || near(v.x, b[2]);
      boolean onHorizontalEdge = near(v.y, b[1]) || near(v.y, b[3]);
      assertFalse(onVerticalEdge && onHorizontalEdge, "square corner at " + v);
    }
  }

  // invisible wall

  @Test
  void wallIsStaticObstacleWithGivenScale() {
    Entity wall = created(ObstacleFactory.createWall(3f, 2f));
    assertStaticObstacle(wall);
    assertNull(wall.getComponent(TextureRenderComponent.class), "walls are invisible");
    assertEquals(3f, wall.getScale().x, EPS);
    assertEquals(2f, wall.getScale().y, EPS);
  }

  // utility class

  @Test
  void constructorThrows() throws Exception {
    Constructor<ObstacleFactory> ctor = ObstacleFactory.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    InvocationTargetException e = assertThrows(InvocationTargetException.class, ctor::newInstance);
    assertTrue(e.getCause() instanceof IllegalStateException);
  }

  // helpers

  /** Returns {minX, minY, maxX, maxY}. Order-independent, because Box2D reorders vertices. */
  private static float[] bounds(PolygonShape shape) {
    float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    Vector2 v = new Vector2();
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, v);
      b[0] = Math.min(b[0], v.x);
      b[1] = Math.min(b[1], v.y);
      b[2] = Math.max(b[2], v.x);
      b[3] = Math.max(b[3], v.y);
    }
    return b;
  }

  private static boolean near(float a, float b) {
    return Math.abs(a - b) < EPS;
  }
}
