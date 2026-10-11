package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbilityMenuDisplayTest {
  private Stage stage;
  private AbilityAttunementComponent attunement;
  private AbilityMenu menu;
  private AbilityMenuDisplay display;
  private Entity ui;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(1_000L);
    ServiceLocator.registerTimeSource(time);
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());

    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());

    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    attunement = new AbilityAttunementComponent();
    Entity player =
        new Entity()
            .addComponent(physics)
            .addComponent(new CombatStatsComponent(100, 10, 2f, 4f))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(attunement)
            .addComponent(new PlayerActions());
    player.create();
    attunement.update();

    menu = new AbilityMenu(player);
    display = new AbilityMenuDisplay(menu);
    ui = new Entity().addComponent(menu).addComponent(display);
    ServiceLocator.getEntityService().register(ui);
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    stage.dispose();
  }

  private Table root() {
    Actor actor = stage.getRoot().findActor("ability-menu");
    assertNotNull(actor, "the menu panel was never added to the stage");
    return (Table) actor;
  }

  private Table rows() {
    return (Table) stage.getRoot().findActor("ability-menu-rows");
  }

  @Test
  void shouldAddAHiddenPanelToTheStage() {
    assertFalse(root().isVisible());
    assertEquals(1, stage.getActors().size);
  }

  @Test
  void shouldShowARowPerAbilityWhenOpened() {
    menu.open();

    assertTrue(root().isVisible());
    assertEquals(menu.getOptions().size(), rows().getChildren().size);
  }

  @Test
  void shouldHideAgainWhenClosed() {
    menu.open();
    menu.close();

    assertFalse(root().isVisible());
  }

  @Test
  void shouldNotLeakActorsAcrossRepeatedVisits() {
    for (int i = 0; i < 5; i++) {
      menu.open();
      menu.close();
    }

    assertEquals(1, stage.getActors().size, "the panel is reused, never re-added");
    menu.open();
    assertEquals(
        menu.getOptions().size(), rows().getChildren().size, "rows are rebuilt, not piled");
  }

  @Test
  void shouldShowReadableNamesRatherThanEventIds() {
    menu.open();

    assertTrue(labels().contains("Invisibility"));
    assertTrue(labels().contains("Last Stand"), "'laststand' is an event id, not a name");
    assertFalse(labels().contains("laststand"));
  }

  @Test
  void shouldMarkWhicheverAbilityIsCarried() {
    assertTrue(attunement.attune(LastStand.class));
    menu.open();

    assertTrue(labels().contains("carried"));
  }

  @Test
  void shouldMarkNothingAsCarriedBeforeTheFirstChoice() {
    menu.open();

    assertFalse(labels().contains("carried"));
  }

  @Test
  void shouldFollowTheSelectionOnUpdate() {
    menu.open();
    display.update();

    menu.moveSelection(1);
    display.update();

    // The highlight is a background on the selected row and none on the others.
    Table selected = (Table) rows().getChildren().get(1);
    Table other = (Table) rows().getChildren().get(0);
    assertNotNull(selected.getBackground(), "the chosen row should be highlighted");
    assertNull(other.getBackground());
  }

  @Test
  void shouldSurviveUpdatesWhileClosed() {
    display.update();
    display.update();

    assertFalse(root().isVisible());
  }

  @Test
  void shouldRemoveItselfFromTheStageWhenDisposed() {
    ui.dispose();
    ui = null;

    assertNull(stage.getRoot().findActor("ability-menu"));
  }

  /** Every bit of text currently on the panel, however deeply nested. */
  private List<String> labels() {
    List<String> found = new ArrayList<>();
    collect(stage.getRoot(), found);
    return found;
  }

  private void collect(Group group, List<String> into) {
    for (Actor child : group.getChildren()) {
      if (child instanceof Label label) {
        into.add(label.getText().toString());
      } else if (child instanceof Group nested) {
        collect(nested, into);
      }
    }
  }
}
