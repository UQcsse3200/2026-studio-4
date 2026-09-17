package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerJumpTest {
  private static final float EPSILON = 0.0001f;
  private final Object encounter = new Object();
  private Entity player;
  private PlayerActions actions;
  private AnimationRenderComponent animator;
  private KeyboardPlayerInputComponent input;
  private Body body;
  private GameTime time;

  @BeforeEach
  void setup() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ResourceService resources = mock(ResourceService.class);
    Sound impactSound = mock(Sound.class);
    when(resources.getAsset("sounds/Impact4.ogg", Sound.class)).thenReturn(impactSound);
    ServiceLocator.registerResourceService(resources);
    actions = new PlayerActions();
    animator = new AnimationRenderComponent(mock(TextureAtlas.class));
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(actions)
            .addComponent(animator);
    player.setPosition(2f, 3f);
    player.create();
    body = player.getComponent(PhysicsComponent.class).getBody();
    input = new KeyboardPlayerInputComponent();
    input.setEntity(player);
  }

  @Test
  void spaceJumpsWithoutChangingGroundPositionOrGrantingInvulnerability() {
    actions.setJumpEnabled(encounter, true);
    Vector2 position = player.getPosition();
    assertTrue(input.keyDown(Keys.SPACE));
    assertTrue(actions.isJumping());
    tick(0.425f);
    assertEquals(0.9f, actions.getJumpHeight(), EPSILON);
    assertEquals(actions.getJumpHeight(), animator.getVerticalOffset(), EPSILON);
    assertTrue(player.getPosition().epsilonEquals(position, EPSILON));
    assertTrue(body.getPosition().epsilonEquals(position, EPSILON));
    assertTrue(body.getLinearVelocity().isZero());
    assertFalse(player.getComponent(CombatStatsComponent.class).isInvulnerable());
    player.getComponent(CombatStatsComponent.class).takeDamage(5, null);
    assertEquals(95, player.getComponent(CombatStatsComponent.class).getHealth());
    tick(0.425f);
    assertFalse(actions.isJumping());
    assertEquals(0f, animator.getVerticalOffset());
  }

  @Test
  void canMoveAndAttackAtNormalSpeedDuringTheJump() {
    actions.setJumpEnabled(encounter, true);
    int[] attacks = {0};
    player.getEvents().addListener("weaponAttack", (Vector2 direction) -> attacks[0]++);
    input.keyDown(Keys.D);
    input.keyDown(Keys.SPACE);
    tick(0.2f);
    assertEquals(3f, body.getLinearVelocity().x, EPSILON);
    assertEquals(0f, body.getLinearVelocity().y, EPSILON);
    input.keyDown(Keys.J);
    assertEquals(1, attacks[0]);
    assertTrue(actions.isJumping());
    input.keyUp(Keys.D);
    assertTrue(body.getLinearVelocity().isZero());
  }

  @Test
  void cannotRetriggerInTheAirOrBeforeTheLandingCooldownEnds() {
    actions.setJumpEnabled(encounter, true);
    input.keyDown(Keys.SPACE);
    tick(0.4f);
    input.keyDown(Keys.SPACE);
    tick(0.46f);
    assertFalse(actions.isJumping());
    input.keyDown(Keys.SPACE);
    assertFalse(actions.isJumping());
    tick(0.3f);
    input.keyDown(Keys.SPACE);
    assertFalse(actions.isJumping());
    tick(0.05f);
    input.keyDown(Keys.SPACE);
    assertTrue(actions.isJumping());
  }

  @Test
  void ownersReleaseOnlyTheirOwnJumpModeAndNormalDashReturnsAfterLastOwner() {
    Object secondOwner = new Object();
    actions.setJumpEnabled(encounter, true);
    actions.setJumpEnabled(secondOwner, true);
    input.keyDown(Keys.SPACE);
    tick(0.2f);
    actions.setJumpEnabled(encounter, false);
    assertTrue(actions.isJumpEnabled());
    assertTrue(actions.isJumping());
    actions.setJumpEnabled(secondOwner, false);
    assertFalse(actions.isJumpEnabled());
    assertFalse(actions.isJumping());
    assertEquals(0f, animator.getVerticalOffset());
    input.keyDown(Keys.SPACE);
    tick(0.001f);
    assertFalse(actions.isJumping());
    assertEquals(-15f, body.getLinearVelocity().y, EPSILON);
  }

  @Test
  void enablingJumpCancelsAnActiveDashAndItsExtraSpeed() {
    input.keyDown(Keys.D);
    input.keyDown(Keys.SPACE);
    tick(0.001f);
    assertEquals(15f, body.getLinearVelocity().x, EPSILON);
    int[] stops = {0};
    player.getEvents().addListener("dashStop", () -> stops[0]++);
    actions.setJumpEnabled(encounter, true);
    assertEquals(1, stops[0]);
    assertEquals(3f, body.getLinearVelocity().x, EPSILON);
    input.keyDown(Keys.SPACE);
    assertTrue(actions.isJumping());
  }

  @Test
  void controlLocksCancelJumpAndOtherOwnersRemainLocked() {
    actions.setJumpEnabled(encounter, true);
    input.keyDown(Keys.SPACE);
    tick(0.2f);
    Object dialogue = new Object();
    actions.setControlsLocked(dialogue, true);
    assertFalse(actions.isJumping());
    assertEquals(0f, animator.getVerticalOffset());
    input.keyDown(Keys.SPACE);
    assertFalse(actions.isJumping());
    actions.setJumpEnabled(encounter, false);
    assertTrue(actions.areControlsLocked());
    actions.setControlsLocked(dialogue, false);
    actions.setJumpEnabled(encounter, true);
    input.keyDown(Keys.SPACE);
    assertTrue(actions.isJumping());
  }

  @Test
  void deathAndDisposalReturnTheSpriteToTheGround() {
    actions.setJumpEnabled(encounter, true);
    input.keyDown(Keys.SPACE);
    tick(0.2f);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    assertFalse(actions.isJumping());
    assertEquals(0f, animator.getVerticalOffset());
    input.keyDown(Keys.SPACE);
    assertFalse(actions.isJumping());
    player.getComponent(CombatStatsComponent.class).setHealth(100);
    input.keyDown(Keys.SPACE);
    tick(0.2f);
    assertTrue(actions.isJumping());
    actions.dispose();
    assertFalse(actions.isJumpEnabled());
    assertFalse(actions.isJumping());
    assertEquals(0f, animator.getVerticalOffset());
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    actions.update();
  }
}
