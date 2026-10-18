package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.items.RoomMagneticPickupComponent;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UiScale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MagneticPickupHintDisplayTest {
  private Stage stage;
  private GameTime time;
  private RunTimer timer;
  private EntityService entities;
  private Entity player;
  private Entity ui;
  private Table root;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    timer = new RunTimer(time);
    ServiceLocator.registerRunTimer(timer);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 720, true);
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    player = new Entity();
    createDisplay();
  }

  private void createDisplay() {
    ui = new Entity().addComponent(new MagneticPickupHintDisplay(player));
    ui.setUpdatesWhilePaused(true);
    entities.register(ui);
    root = stage.getRoot().findActor("magnetic-pickup-hint");
    assertNotNull(root);
  }

  @AfterEach
  void tearDown() {
    ui.dispose();
    stage.dispose();
  }

  private void drop(int count) {
    player.getEvents().trigger(EnemyManagerComponent.ENEMY_DROPS_SPAWNED, count);
  }

  private void tick(float seconds) {
    when(time.getDeltaTime()).thenReturn(seconds);
    entities.update();
  }

  @Test
  void onlyShowsWhenAnEnemyDropLeavesMultipleItemsInTheRoom() {
    assertFalse(root.isVisible());
    drop(0);
    assertFalse(root.isVisible());
    drop(1);
    assertFalse(root.isVisible());

    drop(2);

    assertTrue(root.isVisible());
    Label label = root.findActor("magnetic-pickup-hint-text");
    assertEquals("Hold E for 1 second to collect all room drops.", label.getText().toString());
    assertEquals(Touchable.disabled, root.getTouchable());
  }

  @Test
  void lastsSixSecondsAndLaterDropsDoNotExtendOrRepeatIt() {
    drop(2);
    tick(3f);
    drop(5);
    tick(2.5f);
    assertTrue(root.isVisible());

    tick(0.5f);
    assertFalse(root.isVisible());
    drop(3);
    assertFalse(root.isVisible());
  }

  @Test
  void freezesHideTheHintAndPreserveItsRemainingTime() {
    drop(2);
    tick(2f);
    Object dialogue = new Object();
    entities.setFrozen(dialogue, true);
    tick(30f);
    assertFalse(root.isVisible());

    entities.setFrozen(dialogue, false);
    tick(3.5f);
    assertTrue(root.isVisible());
    tick(0.5f);
    assertFalse(root.isVisible());
  }

  @Test
  void menuPausesHideTheHintAndPreserveItsRemainingTime() {
    drop(2);
    tick(2f);
    timer.requestPause();
    tick(30f);
    assertFalse(root.isVisible());

    timer.requestResume();
    tick(3.5f);
    assertTrue(root.isVisible());
    tick(0.5f);
    assertFalse(root.isVisible());
  }

  @Test
  void aDropDuringPauseWaitsUntilGameplayResumes() {
    timer.requestPause();
    drop(2);
    assertFalse(root.isVisible());
    tick(30f);

    timer.requestResume();
    tick(5.5f);
    assertTrue(root.isVisible());
    tick(0.5f);
    assertFalse(root.isVisible());
  }

  @Test
  void changingRoomsClearsTheHintWithoutAllowingItAgain() {
    drop(2);
    player.getEvents().trigger(RoomMagneticPickupComponent.RESET_EVENT);
    assertFalse(root.isVisible());
    drop(2);
    tick(1f);
    assertFalse(root.isVisible());
  }

  @Test
  void changingRoomsBeforeTheFirstEligibleDropDoesNotSuppressTheHint() {
    player.getEvents().trigger(RoomMagneticPickupComponent.RESET_EVENT);
    drop(2);
    assertTrue(root.isVisible());
  }

  @Test
  void deathClearsTheHintAndIgnoresFurtherDrops() {
    drop(2);
    player.getEvents().trigger("entityDied");
    assertFalse(root.isVisible());
    drop(3);
    tick(1f);
    assertFalse(root.isVisible());
  }

  @Test
  void deathBeforeTheFirstDropPreventsAPosthumousHint() {
    player.getEvents().trigger("entityDied");
    drop(2);
    assertFalse(root.isVisible());
  }

  @Test
  void aNewRunCanShowTheHintAgainAndDisposalRemovesTheOldPanel() {
    drop(2);
    Table previousRoot = root;
    ui.dispose();
    assertNull(stage.getRoot().findActor("magnetic-pickup-hint"));
    player.getEvents().trigger(EnemyManagerComponent.ENEMY_DROPS_SPAWNED, 2);
    assertFalse(previousRoot.isVisible());

    player = new Entity();
    createDisplay();
    drop(2);
    assertTrue(root.isVisible());
    assertEquals(1, stage.getActors().size);
  }

  @Test
  void wrapsWithinANarrowViewportAtTheSelectedUiScale() {
    stage.getViewport().update(640, 480, true);
    UiScale.applyToStage(stage, 1.5f);
    root.validate();
    Table panel = (Table) root.getChildren().first();
    assertTrue(panel.getWidth() * root.getScaleX() <= stage.getWidth() - 32f);
    Label label = root.findActor("magnetic-pickup-hint-text");
    assertTrue(label.getWrap());
  }
}
