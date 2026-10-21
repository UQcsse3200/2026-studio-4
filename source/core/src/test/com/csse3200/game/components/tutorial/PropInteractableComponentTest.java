package com.csse3200.game.components.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PropInteractableComponentTest {
  private final List<Entity> used = new ArrayList<>();
  private final List<String> indicator = new ArrayList<>();
  private Entity prop;
  private Entity player;

  @BeforeEach
  void setUp() {
    prop = new Entity().addComponent(new PropInteractableComponent("Press E", 2f, used::add));
    prop.setScale(1f, 1f);
    prop.setPosition(0f, 0f);
    prop.create();
    prop.getEvents()
        .addListener(NpcInteractionEvents.INDICATOR_SHOWN, () -> indicator.add("shown"));
    prop.getEvents()
        .addListener(NpcInteractionEvents.INDICATOR_HIDDEN, () -> indicator.add("hidden"));

    player = new Entity();
    player.setScale(1f, 1f);
  }

  private PropInteractableComponent component() {
    return prop.getComponent(PropInteractableComponent.class);
  }

  @Test
  void shouldBeInRangeOnlyWithinItsRange() {
    player.setPosition(1.5f, 0f);
    assertTrue(component().isInRange(player));

    player.setPosition(2.5f, 0f);
    assertFalse(component().isInRange(player));
  }

  @Test
  void shouldMeasureDistanceBetweenCentres() {
    player.setPosition(3f, 4f);

    assertEquals(5f, component().distanceTo(player), 1e-4f);
  }

  @Test
  void shouldRunItsActionWithThePlayer() {
    component().interact(player);

    assertEquals(1, used.size());
    assertSame(player, used.get(0));
  }

  @Test
  void shouldExposeItsPrompt() {
    assertEquals("Press E", component().getPrompt());
  }

  @Test
  void shouldOnlyAnnounceTheIndicatorWhenItChanges() {
    component().setIndicatorVisible(true);
    component().setIndicatorVisible(true);
    component().setIndicatorVisible(false);
    component().setIndicatorVisible(false);

    assertEquals(List.of("shown", "hidden"), indicator);
  }

  @Test
  void shouldRefuseMissingPromptOrAction() {
    assertThrows(NullPointerException.class, () -> new PropInteractableComponent(null, p -> {}));
    assertThrows(NullPointerException.class, () -> new PropInteractableComponent("Press E", null));
  }
}
