package com.csse3200.game.components.tutorial;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.rooms.EntityManagerComponent;
import com.csse3200.game.components.rooms.FriendlyNpcManagerComponent;
import com.csse3200.game.components.rooms.configs.PropSpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.TutorialPropFactory;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.rooms.RoomSelector;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spawns a room's training props and finds the one the player can use.
 *
 * <p>The prop counterpart to FriendlyNpcManagerComponent. Props are tracked here and never in
 * EnemyManagerComponent, so a practice dummy can never stop a room from counting as cleared.
 *
 * <p>Each frame it shows the indicator and HUD prompt for the nearest usable prop in range, and it
 * routes E (as {@link RoomSelector#PROP_INTERACT}) to that prop. Scores from dummies, targets and
 * cannons are shown through the room's status line.
 */
public class TutorialPropManagerComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(TutorialPropManagerComponent.class);

  private final PropSpawnConfig[] spawns;
  private final List<PropInteractableComponent> interactables = new ArrayList<>();
  private Entity player;
  private boolean promptShown;
  private int dodges;
  private int dodgeHits;

  /**
   * @param spawns the props to place when the room is created
   */
  public TutorialPropManagerComponent(PropSpawnConfig[] spawns) {
    this.spawns = spawns;
  }

  @Override
  public void create() {
    entity.getEvents().<Entity>addListener("RoomCreated", this::spawnProps);
    entity.getEvents().<Entity>addListener(RoomSelector.PROP_INTERACT, this::onInteract);
  }

  /**
   * Builds every configured prop. A prop with an unusable definition is skipped and logged rather
   * than stopping the room from loading.
   */
  public void spawnProps(Entity player) {
    this.player = player;
    for (PropSpawnConfig spawn : spawns) {
      Entity prop;
      try {
        prop = build(spawn);
      } catch (IllegalArgumentException | NullPointerException e) {
        logger.warn("Skipping prop at ({}, {}): {}", spawn.x, spawn.y, e.getMessage());
        continue;
      }
      PropInteractableComponent interactable = prop.getComponent(PropInteractableComponent.class);
      if (interactable != null) {
        interactables.add(interactable);
      }
      spawnEntityAt(prop, new GridPoint2(spawn.x, spawn.y), true, false);
    }
  }

  private Entity build(PropSpawnConfig spawn) {
    if (spawn.type == null) {
      throw new IllegalArgumentException("prop has no type");
    }
    String label = spawn.label == null ? defaultLabel(spawn.type) : spawn.label;
    return switch (spawn.type) {
      case TRAINING_DUMMY -> announceHits(TutorialPropFactory.createTrainingDummy(label));
      case ARCHERY_TARGET -> announceHits(TutorialPropFactory.createArcheryTarget(label));
      case SIGN -> buildSign(spawn, label);
      case SUPPLY_STAND -> buildSupplyStand(spawn);
      case DODGE_CANNON -> buildCannon(spawn);
    };
  }

  private Entity announceHits(Entity target) {
    target
        .getEvents()
        .<String, Integer, Integer>addListener(
            TrainingDummyComponent.HIT,
            (name, damage, hits) ->
                showStatus(name + ": " + damage + " damage  (hit " + hits + ")"));
    return target;
  }

  private Entity buildSign(PropSpawnConfig spawn, String label) {
    if (spawn.dialogueId == null || spawn.dialogueId.isBlank()) {
      throw new IllegalArgumentException("sign needs a dialogueId");
    }
    Entity sign = TutorialPropFactory.createSign();
    sign.addComponent(
        new PropInteractableComponent(
            "Press E — Read " + label, p -> CutsceneEvents.playDialogue(p, spawn.dialogueId)));
    return sign;
  }

  private Entity buildSupplyStand(PropSpawnConfig spawn) {
    String itemId = spawn.itemId;
    if (itemId == null || !ItemCatalog.contains(itemId)) {
      throw new IllegalArgumentException("supply stand needs a valid itemId, got: " + itemId);
    }
    Item item = ItemCatalog.create(itemId, 1);
    Entity stand = TutorialPropFactory.createSupplyStand(itemId);
    stand.addComponent(
        new PropInteractableComponent(
            "Press E — Take " + item.getName(), p -> lendItem(itemId, item.getName())));
    return stand;
  }

  private Entity buildCannon(PropSpawnConfig spawn) {
    Entity cannon = TutorialPropFactory.createDodgeCannon();
    cannon.addComponent(new DodgeCannonComponent(player, spawn.interval, spawn.delay, spawn.range));
    cannon.getEvents().<Boolean>addListener(DodgeCannonComponent.RESULT, this::onDodgeResult);
    return cannon;
  }

  private void lendItem(String itemId, String itemName) {
    TrainingLoadoutComponent loadout = entity.getComponent(TrainingLoadoutComponent.class);
    if (loadout == null) {
      showStatus("Supplies are only lent out in the training grounds.");
    } else if (loadout.lend(itemId)) {
      showStatus(
          "You took a " + itemName + ". TAB picks it, Q uses it. It goes back when you leave.");
    } else {
      showStatus("You are already holding a " + itemName + ". TAB picks it, Q uses it.");
    }
  }

  private void onDodgeResult(Boolean result) {
    if (Boolean.TRUE.equals(result)) {
      dodges++;
      showStatus("Dodged! " + dodges + " dodged, " + dodgeHits + " hit.");
    } else {
      dodgeHits++;
      showStatus("Clipped! No damage here, but it would hurt in a dungeon. SPACE dashes.");
    }
  }

  private void onInteract(Entity player) {
    PropInteractableComponent usable = usableNearest();
    if (usable != null) {
      usable.interact(player);
    }
  }

  @Override
  public void update() {
    if (player == null) {
      return;
    }
    PropInteractableComponent nearest = usableNearest();
    for (PropInteractableComponent prop : interactables) {
      prop.setIndicatorVisible(prop == nearest);
    }
    updatePrompt(nearest);
  }

  /** The prop E would use right now, or null if the player is busy or an NPC is closer. */
  private PropInteractableComponent usableNearest() {
    NpcInteractorComponent interactor = player.getComponent(NpcInteractorComponent.class);
    if (interactor != null && interactor.isInteracting()) {
      return null;
    }
    FriendlyNpcManagerComponent npcs = entity.getComponent(FriendlyNpcManagerComponent.class);
    if (npcs != null && npcs.findNearestInRange(player) != null) {
      return null;
    }
    return findNearestInRange(player);
  }

  /**
   * @param player the player entity
   * @return the closest prop the player is within range of, or null if there is none
   */
  public PropInteractableComponent findNearestInRange(Entity player) {
    PropInteractableComponent nearest = null;
    float nearestDistance = Float.MAX_VALUE;
    for (PropInteractableComponent prop : interactables) {
      float distance = prop.distanceTo(player);
      if (prop.isInRange(player) && distance < nearestDistance) {
        nearest = prop;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  /** Shows the nearest prop's prompt, and only ever clears a prompt this manager put up. */
  private void updatePrompt(PropInteractableComponent nearest) {
    InteractionPromptDisplay display = player.getComponent(InteractionPromptDisplay.class);
    if (display == null) {
      return;
    }
    if (nearest != null) {
      display.setPrompt(nearest.getPrompt());
      promptShown = true;
    } else if (promptShown) {
      display.clearPrompt();
      promptShown = false;
    }
  }

  private void showStatus(String message) {
    entity.getEvents().trigger(GameAreaDisplay.DISPLAY, message);
  }

  private static String defaultLabel(PropSpawnConfig.PropType type) {
    return switch (type) {
      case TRAINING_DUMMY -> "Training Dummy";
      case ARCHERY_TARGET -> "Archery Target";
      case SIGN -> "Notice Board";
      case SUPPLY_STAND -> "Supplies";
      case DODGE_CANNON -> "Training Cannon";
    };
  }

  @Override
  public void dispose() {
    if (promptShown && player != null) {
      InteractionPromptDisplay display = player.getComponent(InteractionPromptDisplay.class);
      if (display != null) {
        display.clearPrompt();
      }
    }
    interactables.clear();
    player = null;
    super.dispose();
  }
}
