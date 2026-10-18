package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.boss.FinalBossStageTwoComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.RoomMagneticPickupComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldPickupComponent;
import com.csse3200.game.components.player.KeyboardPlayerInputComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Item;
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
    player = new Entity().addComponent(new RoomMagneticPickupComponent());
    entities.register(player);
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
    for (Entity entity : new Array<>(entities.getEntities())) {
      entity.dispose();
    }
    stage.dispose();
  }

  private ItemComponent item(int quantity) {
    Item item = mock(Item.class);
    when(item.getQuantity()).thenReturn(quantity);
    return new ItemComponent(item);
  }

  private Entity drop(int quantity) {
    Entity drop = new Entity().addComponent(item(quantity));
    entities.register(drop);
    return drop;
  }

  private void collect(Entity drop) {
    assertTrue(drop.getComponent(ItemComponent.class).tryPickUp(player));
  }

  private void tick(float seconds) {
    when(time.getDeltaTime()).thenReturn(seconds);
    // Exercise the hint inside the service's active libGDX Array iterator.
    entities.update();
  }

  @Test
  void showsForTwoGroundEntitiesWithoutAnEnemyEventAndDoesNotCountStackQuantity() {
    tick(0f);
    assertFalse(root.isVisible());
    drop(7);
    tick(0f);
    assertFalse(root.isVisible());
    drop(1);
    tick(0f);

    assertTrue(root.isVisible());
    Label label = root.findActor("magnetic-pickup-hint-text");
    assertEquals("Hold E for 1 second to collect all room drops.", label.getText().toString());
    assertEquals(Touchable.disabled, root.getTouchable());
  }

  @Test
  void staysUntilTheLastItemIsCollectedEvenIfTheEntitiesAwaitDisposal() {
    Entity first = drop(1);
    Entity second = drop(1);
    tick(0f);
    tick(600f);
    assertTrue(root.isVisible());

    collect(first);
    tick(0f);
    assertTrue(root.isVisible(), "The hint must remain while one item is still on the ground");
    collect(second);
    tick(0f);
    assertFalse(root.isVisible());
    assertTrue(entities.getEntities().contains(first, true));
    assertTrue(entities.getEntities().contains(second, true));
  }

  @Test
  void repeatsAfterAnEmptyRoomAndIncludesNewDropsWhileAlreadyVisible() {
    Entity first = drop(1);
    Entity second = drop(1);
    tick(0f);
    collect(first);
    collect(second);
    tick(0f);
    assertFalse(root.isVisible());

    Entity next = drop(1);
    tick(0f);
    assertFalse(root.isVisible());
    Entity nextPartner = drop(1);
    tick(0f);
    assertTrue(root.isVisible());
    Entity late = drop(1);
    collect(next);
    collect(nextPartner);
    tick(0f);
    assertTrue(root.isVisible(), "A later drop keeps the active reminder relevant");
    collect(late);
    tick(0f);
    assertFalse(root.isVisible());
  }

  @Test
  void ignoresCollectedUnregisteredAndSpecialBossEntities() {
    Entity collected = drop(1);
    collect(collected);
    Entity removed = drop(1);
    entities.unregister(removed);
    entities.register(new Entity().addComponent(mock(SnakeShieldPickupComponent.class)));
    entities.register(new Entity().addComponent(mock(FinalBossStageTwoComponent.class)));
    drop(1);
    tick(0f);
    assertFalse(root.isVisible());

    drop(1);
    tick(0f);
    assertTrue(root.isVisible());
  }

  @Test
  void neverCountsThePlayerAsAGroundItem() {
    ui.dispose();
    player.dispose();
    player = new Entity().addComponent(item(1));
    entities.register(player);
    createDisplay();
    drop(1);
    tick(0f);
    assertFalse(root.isVisible());
  }

  @Test
  void removingTheLastGroundEntityAlsoClearsTheHint() {
    Entity first = drop(1);
    Entity second = drop(1);
    tick(0f);
    first.dispose();
    tick(0f);
    assertTrue(root.isVisible());
    second.dispose();
    tick(0f);
    assertFalse(root.isVisible());
  }

  @Test
  void freezesHideAnActiveHintWithoutLosingItsSingleItemContinuation() {
    Entity first = drop(1);
    Entity second = drop(1);
    tick(0f);
    Object dialogue = new Object();
    entities.setFrozen(dialogue, true);
    collect(first);
    tick(30f);
    assertFalse(root.isVisible());

    entities.setFrozen(dialogue, false);
    tick(0f);
    assertTrue(root.isVisible());
    collect(second);
    tick(0f);
    assertFalse(root.isVisible());
  }

  @Test
  void menuPausesHideTheHintAndNewDropsCanShowItAfterResuming() {
    timer.requestPause();
    drop(1);
    Entity second = drop(1);
    tick(30f);
    assertFalse(root.isVisible());

    timer.requestResume();
    tick(0f);
    assertTrue(root.isVisible());
    timer.requestPause();
    collect(second);
    tick(30f);
    assertFalse(root.isVisible());
    timer.requestResume();
    tick(0f);
    assertTrue(root.isVisible());
  }

  @Test
  void changingRoomsClearsTheActiveHintAndRequiresTwoNewRoomItems() {
    Entity first = drop(1);
    Entity second = drop(1);
    tick(0f);
    player.getEvents().trigger(RoomMagneticPickupComponent.RESET_EVENT);
    assertFalse(root.isVisible());
    first.dispose();
    second.dispose();
    drop(1);
    tick(0f);
    assertFalse(root.isVisible());
    drop(1);
    tick(0f);
    assertTrue(root.isVisible());
  }

  @Test
  void deathHidesTheHintAndDisposalRemovesItsPanel() {
    drop(1);
    drop(1);
    tick(0f);
    player.getEvents().trigger("entityDied");
    assertFalse(root.isVisible());
    drop(1);
    tick(0f);
    assertFalse(root.isVisible());
    ui.dispose();
    assertNull(stage.getRoot().findActor("magnetic-pickup-hint"));
  }

  @Test
  void aNewRunCanDisplayTheHintAfterThePreviousPlayerDied() {
    drop(1);
    drop(1);
    tick(0f);
    player.getEvents().trigger("entityDied");
    ui.dispose();
    player.dispose();
    player = new Entity();
    entities.register(player);
    createDisplay();
    tick(0f);
    assertTrue(root.isVisible());
    assertEquals(1, stage.getActors().size);
  }

  @Test
  void remainsVisibleDuringMagneticFlightAndHidesAfterTheLastAutomaticPickup() {
    Entity near = drop(1);
    near.setPosition(3f, 0f);
    Entity far = drop(1);
    far.setPosition(6f, 0f);
    tick(0f);
    assertTrue(root.isVisible());

    player.getEvents().trigger(KeyboardPlayerInputComponent.ROOM_MAGNETIC_PICKUP);
    tick(0.05f);
    assertTrue(root.isVisible());
    assertFalse(near.getComponent(ItemComponent.class).isCollected());
    assertFalse(far.getComponent(ItemComponent.class).isCollected());
    tick(0.5f);
    assertTrue(near.getComponent(ItemComponent.class).isCollected());
    assertFalse(far.getComponent(ItemComponent.class).isCollected());
    assertTrue(root.isVisible());
    tick(0.5f);
    assertTrue(far.getComponent(ItemComponent.class).isCollected());
    assertFalse(root.isVisible());
    verify(near.getComponent(ItemComponent.class).getItem()).pickUp(player);
    verify(far.getComponent(ItemComponent.class).getItem()).pickUp(player);
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
